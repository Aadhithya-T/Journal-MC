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
    private final BlockBreakingManager blockBreakingManager;
    private final FluidPhysicsManager fluidPhysicsManager;
    private final ItemEntityManager itemEntityManager;
    private final ParticleManager particleManager;

    private String currentBiome = "Plains";
    private String currentWorldName = "Hardcore World";
    private long currentSeed = 4242;
    private boolean inWorld = false;

    public WorldSession() {
        this.blockBreakingManager = new BlockBreakingManager();
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

    public BlockBreakingManager getBlockBreakingManager() {
        return blockBreakingManager;
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

        System.out.println("[WorldSession] Initializing infinite streamed Hardcore world: '" + worldName + "' (Seed: " + seed + ")...");
        File worldDir = new File("saves/" + worldName.toLowerCase().replaceAll("[^a-z0-9_-]", "_"));
        this.chunkManager = new ChunkManager(settings.renderDistance, seed, worldDir);

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
        int spawnCx = Math.floorDiv((int) (existingSave != null ? existingSave.playerX : 8), 16);
        int spawnCz = Math.floorDiv((int) (existingSave != null ? existingSave.playerZ : 8), 16);
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
            player.pos.set(existingSave.playerX, existingSave.playerY, existingSave.playerZ);
            player.prevPos.set(existingSave.playerX, existingSave.playerY, existingSave.playerZ);
            player.yaw = existingSave.playerYaw;
            player.pitch = existingSave.playerPitch;
            player.health = existingSave.health;
            player.hunger = existingSave.hunger;
            player.selectedSlot = Math.clamp(existingSave.selectedSlot, 0, 8);
            if (existingSave.hotbarBlocks != null && existingSave.hotbarCounts != null) {
                System.arraycopy(existingSave.hotbarBlocks, 0, player.hotbarBlocks, 0, 9);
                System.arraycopy(existingSave.hotbarCounts, 0, player.hotbarCounts, 0, 9);
                for (int i = 0; i < 9; i++) {
                    if (Item.isTool(player.hotbarBlocks[i])) {
                        player.hotbarCounts[i] = Math.min(player.hotbarCounts[i], 1);
                    }
                }
            } else {
                player.hotbarBlocks[0] = Item.IRON_AXE;
                player.hotbarCounts[0] = 1;
                player.hotbarBlocks[1] = Item.IRON_SHOVEL;
                player.hotbarCounts[1] = 1;
                player.hotbarBlocks[2] = Item.IRON_PICKAXE;
                player.hotbarCounts[2] = 1;
            }

            player.velocity.set(0, 0, 0);
            player.isDead = false;

            camera.setYaw(player.yaw);
            camera.setPitch(player.pitch);
            System.out.println("[WorldSession] 🚀 Restored player state at (" +
                    String.format("%.1f, %.1f, %.1f", player.pos.x, player.pos.y, player.pos.z) + ", HP: " + player.health + "/20, Time: " +
                    String.format("%.0f", atmosphericSystem != null ? atmosphericSystem.getWorldTimeTicks() : 6000.0) + " ticks)!");
        } else {
            if (atmosphericSystem != null) {
                atmosphericSystem.setWorldTimeTicks(6000.0); // Day 1 baseline
            }

            // Safe surface spawn scan for new world
            int spawnX = 8;
            int spawnZ = 8;
            int spawnY = 66;
            for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                if (chunkManager.getBlockStateAt(spawnX, y, spawnZ).isSolid()) {
                    spawnY = y + 2;
                    break;
                }
            }

            player.pos.set(spawnX, spawnY, spawnZ);
            player.prevPos.set(spawnX, spawnY, spawnZ);
            player.yaw = 0;
            player.pitch = 0;
            player.velocity.set(0, 0, 0);
            player.health = 20;
            player.hunger = 20;
            player.selectedSlot = 0;
            Arrays.fill(player.hotbarBlocks, (byte) 0);
            Arrays.fill(player.hotbarCounts, 0);
            player.hotbarBlocks[0] = Item.IRON_AXE;
            player.hotbarCounts[0] = 1;
            player.hotbarBlocks[1] = Item.IRON_SHOVEL;
            player.hotbarCounts[1] = 1;
            player.hotbarBlocks[2] = Item.IRON_PICKAXE;
            player.hotbarCounts[2] = 1;
            player.isDead = false;

            camera.setYaw(0);
            camera.setPitch(0);

            // Immediately save initial state
            save(player, atmosphericSystem != null ? atmosphericSystem.getWorldTimeTicks() : 6000.0);
            System.out.println("[WorldSession] 🚀 Spawned into fresh Hardcore World at Y=" + spawnY + "!");
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

        // Throw held item (Q key)
        if (inputSystem != null) {
            inputSystem.handleItemDropInput(input, player, camera, itemEntityManager, handRenderer);
        }

        // Mine blocks (LMB) & Place blocks (RMB) with particles and fluid physics
        boolean lmb = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_1);
        boolean rmb = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_2);
        blockBreakingManager.update(chunkManager, chunkRenderer, particleManager, itemEntityManager, fluidPhysicsManager, handRenderer, player, camera, lmb, rmb);

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
