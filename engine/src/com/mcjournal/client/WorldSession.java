package com.mcjournal.client;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkMeshBuilder;
import com.mcjournal.ChunkPos;
import com.mcjournal.FluidPhysicsManager;
import com.mcjournal.Item;
import org.joml.Vector3f;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * WorldSession encapsulates the active voxel world lifecycle:
 * chunk streaming, terrain generation, block mutation, physics, fluid dynamics,
 * dropped entities, particle simulation, and persistence.
 */
public class WorldSession {
    private static final double TICK_DURATION = 0.050; // 50ms = 20 TPS

    private ChunkManager chunkManager;
    private final BlockPlacementManager blockPlacementManager;
    private final FluidPhysicsManager fluidPhysicsManager;
    private final ItemEntityManager itemEntityManager;
    private final ParticleManager particleManager;

    private String currentBiome = "Plains";
    private String currentWorldName = "Hardcore World";
    private long currentSeed = 4242;
    private boolean inWorld = false;

    public WorldSession() {
        this.blockPlacementManager = new BlockPlacementManager();
        this.fluidPhysicsManager = new FluidPhysicsManager();
        this.itemEntityManager = new ItemEntityManager();
        this.particleManager = new ParticleManager();
    }

    public void init() {
        itemEntityManager.init();
        particleManager.init();
    }

    public boolean isInWorld() {
        return inWorld;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public BlockPlacementManager getBlockPlacementManager() {
        return blockPlacementManager;
    }

    public FluidPhysicsManager getFluidPhysicsManager() {
        return fluidPhysicsManager;
    }

    public ItemEntityManager getItemEntityManager() {
        return itemEntityManager;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public String getCurrentBiome() {
        return currentBiome;
    }

    public String getCurrentWorldName() {
        return currentWorldName;
    }

    public long getCurrentSeed() {
        return currentSeed;
    }

    public void setRenderDistance(int renderDistance) {
        if (chunkManager != null) {
            chunkManager.setRenderDistance(renderDistance);
        }
    }

    public void enterWorld(long seed, String worldName, String biome, WorldSaveManager.SavedWorld existingSave,
                           Player player, Camera camera, ChunkRenderer chunkRenderer,
                           GameSettings settings, AtmosphericTimeSystem atmosphericSystem) {
        this.currentBiome = biome;
        this.currentWorldName = worldName;
        this.currentSeed = seed;

        com.mcjournal.world.WorldConfiguration worldConfig;
        if (existingSave != null && !existingSave.isLegacy()) {
            worldConfig = existingSave.toWorldConfiguration();
        } else {
            worldConfig = com.mcjournal.world.WorldConfiguration.createDefault(seed);
        }
        com.mcjournal.world.WorldBounds worldBounds = new com.mcjournal.world.WorldBounds(worldConfig);

        System.out.println("[WorldSession] Initializing finite world: '" + worldName + "' (" + worldConfig.width() + "x" + worldConfig.depth() + "x" + worldConfig.height() + ", Seed: " + seed + ")...");
        File worldDir = new File("saves/" + worldName.toLowerCase().replaceAll("[^a-z0-9_-]", "_"));
        this.chunkManager = new ChunkManager(settings.renderDistance, seed, worldDir, worldBounds);

        // 1. If loading an existing save, apply all persisted voxel block changes
        if (existingSave != null) {
            Map<String, com.mcjournal.block.BlockState> deltas = existingSave.getBlockStateDeltas();
            if (deltas != null && !deltas.isEmpty()) {
                System.out.println("[WorldSession] Restoring " + deltas.size() + " modified world block states from save file...");
                chunkManager.applyModifiedBlockStates(deltas);
            }
        }

        // 2. Clear previous GPU meshes and synchronously load initial spawn chunk neighborhood
        if (chunkRenderer != null) {
            chunkRenderer.cleanup();
        }
        int defaultSpawnX = worldConfig.spawnX();
        int defaultSpawnY = worldConfig.spawnY();
        int defaultSpawnZ = worldConfig.spawnZ();

        // Terrain-aware safe spawn resolution for fresh world initialization
        if (existingSave == null) {
            com.mcjournal.world.terrain.SpawnLocator.SpawnPosition safeSpawn =
                com.mcjournal.world.terrain.SpawnLocator.findSafeSpawn(chunkManager.getGenerator(), worldBounds);
            defaultSpawnX = safeSpawn.x();
            defaultSpawnY = safeSpawn.y();
            defaultSpawnZ = safeSpawn.z();
        }

        float targetPlayerX = (existingSave != null ? existingSave.playerX : defaultSpawnX + 0.5f);
        float targetPlayerY = (existingSave != null ? existingSave.playerY : (float) defaultSpawnY);
        float targetPlayerZ = (existingSave != null ? existingSave.playerZ : defaultSpawnZ + 0.5f);

        // Validate spawn coordinates against world bounds using complete player AABB
        if (!worldBounds.isPlayerAABBInsideWorld(targetPlayerX, targetPlayerY, targetPlayerZ)) {
            targetPlayerX = defaultSpawnX;
            targetPlayerY = defaultSpawnY;
            targetPlayerZ = defaultSpawnZ;
        }

        int spawnCx = Math.floorDiv((int) targetPlayerX, 16);
        int spawnCz = Math.floorDiv((int) targetPlayerZ, 16);
        chunkManager.waitForInitialChunks(spawnCx, spawnCz, 3); // 7x7 core chunks generated & meshed

        // Upload initial spawn meshes to GPU
        if (chunkRenderer != null) {
            ChunkPos uploadPos;
            while ((uploadPos = chunkManager.pollPendingMeshUpload()) != null) {
                ChunkMeshBuilder.MeshData mesh = chunkManager.getChunkMesh(uploadPos);
                if (mesh != null) {
                    chunkRenderer.uploadChunkMesh(uploadPos, mesh);
                    chunkManager.markGpuLoaded(uploadPos);
                }
            }
        }

        // 3. Restore or compute player spawn position, stats & time of day
        if (existingSave != null) {
            if (atmosphericSystem != null) {
                atmosphericSystem.setWorldTimeTicks(existingSave.worldTime);
            }
            float px = existingSave.playerX;
            float py = existingSave.playerY;
            float pz = existingSave.playerZ;

            // Validate complete player AABB against loaded world configuration
            if (!worldBounds.isPlayerAABBInsideWorld(px, py, pz)) {
                System.out.println("[WorldSession] Saved player AABB at (" + px + ", " + py + ", " + pz + ") extends outside world bounds! Relocating to spawn (" + defaultSpawnX + ", " + defaultSpawnY + ", " + defaultSpawnZ + ")...");
                px = defaultSpawnX;
                py = (float) defaultSpawnY;
                pz = defaultSpawnZ;
            }

            player.pos.set(px, py, pz);
            player.prevPos.set(px, py, pz);
            player.yaw = existingSave.playerYaw;
            player.pitch = existingSave.playerPitch;
            player.selectedSlot = Math.clamp(existingSave.selectedSlot, 0, 8);
            if (existingSave.hotbarBlocks != null && existingSave.hotbarCounts != null) {
                System.arraycopy(existingSave.hotbarBlocks, 0, player.hotbarBlocks, 0, 9);
                System.arraycopy(existingSave.hotbarCounts, 0, player.hotbarCounts, 0, 9);
                for (int i = 0; i < 9; i++) {
                    if (Item.isLegacyTool(player.hotbarBlocks[i])) {
                        // Safely sanitize legacy tool items from older save files
                        player.hotbarBlocks[i] = 0;
                        player.hotbarCounts[i] = 0;
                    }
                }
            } else {
                Arrays.fill(player.hotbarBlocks, (byte) 0);
                Arrays.fill(player.hotbarCounts, 0);
            }

            player.velocity.set(0, 0, 0);

            camera.setYaw(player.yaw);
            camera.setPitch(player.pitch);
            System.out.println("[WorldSession] 🚀 Restored player state at (" +
                    String.format("%.1f, %.1f, %.1f", player.pos.x, player.pos.y, player.pos.z) + ", Time: " +
                    String.format("%.0f", atmosphericSystem != null ? atmosphericSystem.getWorldTimeTicks() : 6000.0) + " ticks)!");
        } else {
            if (atmosphericSystem != null) {
                atmosphericSystem.setWorldTimeTicks(6000.0); // Day 1 baseline
            }

            // Terrain-aware safe surface spawn for new world
            float spawnX = defaultSpawnX + 0.5f;
            float spawnY = (float) defaultSpawnY;
            float spawnZ = defaultSpawnZ + 0.5f;

            player.pos.set(spawnX, spawnY, spawnZ);
            player.prevPos.set(spawnX, spawnY, spawnZ);
            player.yaw = 0;
            player.pitch = 0;
            player.velocity.set(0, 0, 0);
            player.selectedSlot = 0;
            Arrays.fill(player.hotbarBlocks, (byte) 0);
            Arrays.fill(player.hotbarCounts, 0);

            camera.setYaw(0);
            camera.setPitch(0);

            // Immediately save initial state
            save(player, atmosphericSystem != null ? atmosphericSystem.getWorldTimeTicks() : 6000.0);
            System.out.println("[WorldSession] 🚀 Spawned into fresh World at Y=" + spawnY + "!");
        }

        this.inWorld = true;
    }

    public void tick(Player player, Camera camera, InputHandler input,
                     FirstPersonHandRenderer handRenderer, ChunkRenderer chunkRenderer,
                     GameInputSystem inputSystem) {
        if (!inWorld || chunkManager == null || player == null) return;

        boolean forward = input.isKeyDown(GLFW_KEY_W);
        boolean backward = input.isKeyDown(GLFW_KEY_S);
        boolean left = input.isKeyDown(GLFW_KEY_A);
        boolean right = input.isKeyDown(GLFW_KEY_D);
        boolean jump = input.isKeyDown(GLFW_KEY_SPACE);
        boolean sprint = input.isKeyDown(GLFW_KEY_LEFT_CONTROL);
        boolean sneak = input.isKeyDown(GLFW_KEY_LEFT_SHIFT);

        player.updateTick(chunkManager, forward, backward, left, right, jump, sprint, sneak);

        // Stream chunks dynamically based on player position & look direction
        int playerCx = Math.floorDiv((int) Math.floor(player.pos.x), 16);
        int playerCz = Math.floorDiv((int) Math.floor(player.pos.z), 16);
        Vector3f look = camera.getLookDirection();
        chunkManager.updatePlayerPosition(playerCx, playerCz, look.x, look.z);

        // Place blocks (RMB) from inventory with fluid physics
        boolean rmb = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_2);
        blockPlacementManager.update(chunkManager, chunkRenderer, particleManager, fluidPhysicsManager, handRenderer, player, camera, rmb);

        // Update Fluid Physics Simulation (Water flows, cascades, and fills cavities)
        fluidPhysicsManager.updateTicks(chunkManager, chunkRenderer, particleManager);

        // Update Dropped Item Entities simulation & player magnet
        itemEntityManager.update(TICK_DURATION, chunkManager, player);

        // Update Particle simulation
        particleManager.update(TICK_DURATION, chunkManager);
    }

    public void prepareRender(ChunkRenderer chunkRenderer) {
        if (!inWorld || chunkManager == null || chunkRenderer == null) return;

        // 1. Asynchronously process dirty chunks from block mutations & fluid updates
        chunkManager.processDirtyChunks();

        // 2. Drain pending GPU unloads and budgeted GPU uploads
        chunkManager.processGpuUnloads(chunkRenderer);
        chunkManager.processGpuUploads(chunkRenderer);
    }

    public void save(Player player, double worldTimeTicks) {
        if (inWorld && chunkManager != null) {
            WorldSaveManager.saveWorld(
                currentWorldName,
                currentBiome,
                currentSeed,
                player,
                worldTimeTicks,
                chunkManager
            );
        }
    }

    public void saveAndQuit(Player player, double worldTimeTicks) {
        if (inWorld) {
            save(player, worldTimeTicks);
            inWorld = false;
        }
    }

    public void shutdown(Player player, double worldTimeTicks) {
        if (inWorld) {
            save(player, worldTimeTicks);
            inWorld = false;
        }
        if (chunkManager != null) {
            chunkManager.shutdown();
        }
        itemEntityManager.cleanup();
        particleManager.cleanup();
    }
}
