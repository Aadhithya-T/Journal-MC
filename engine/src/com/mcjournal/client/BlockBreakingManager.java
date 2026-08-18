package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import com.mcjournal.FluidPhysicsManager;
import org.joml.Vector3f;

public class BlockBreakingManager {
    public static final float REACH_DISTANCE = 4.5f; // Vanilla Minecraft survival reach

    private Raycast.Hit currentHit;
    private int targetBx = -1;
    private int targetBy = -1;
    private int targetBz = -1;
    private float breakProgress = 0.0f; // 0.0 to 1.0

    private boolean wasLmbDown = false;
    private boolean wasRmbDown = false;
    private int hitParticleTick = 0;

    // Hotbar default block types for slot 0..8
    public static final byte[] HOTBAR_BLOCKS = {
        Block.DIRT,
        Block.COBBLESTONE,
        Block.OAK_LOG,
        Block.SAND,
        Block.OAK_LEAVES,
        Block.STONE,
        Block.POPPY,
        Block.DANDELION,
        Block.BIRCH_LOG
    };

    public void update(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, FluidPhysicsManager fluidPhysics, Player player, Camera camera, boolean lmbDown, boolean rmbDown) {
        // 1. Cast ray from player eye position along look direction
        Vector3f eyePos = player.getEyePosition(1.0f);
        Vector3f lookDir = camera.getLookDirection();

        currentHit = Raycast.cast(world, eyePos, lookDir, REACH_DISTANCE);

        if (currentHit == null) {
            resetBreak();
        } else {
            // Check if looking at a new block
            if (currentHit.bx != targetBx || currentHit.by != targetBy || currentHit.bz != targetBz) {
                targetBx = currentHit.bx;
                targetBy = currentHit.by;
                targetBz = currentHit.bz;
                breakProgress = 0.0f;
            }

            // 2. Handle Mining with Left Mouse Button (LMB)
            if (lmbDown) {
                float hardness = Block.getHardness(currentHit.blockType);

                if (hardness == 0.0f) {
                    // Instant break (Flowers, Tall Grass)
                    breakTargetBlock(world, renderer, particles, fluidPhysics, currentHit.bx, currentHit.by, currentHit.bz);
                    resetBreak();
                } else if (hardness > 0.0f) {
                    // Spawn mining chip particles periodically while hitting
                    hitParticleTick++;
                    if (hitParticleTick % 4 == 0 && particles != null) {
                        particles.spawnMiningHitParticles(currentHit.bx, currentHit.by, currentHit.bz, currentHit.blockType, currentHit.normalX, currentHit.normalY, currentHit.normalZ);
                    }

                    // Vanilla Hand Mining formula: damage = 1.0 / (hardness * 30 * 1.0)
                    float damagePerTick = 1.0f / (hardness * 30.0f);
                    breakProgress += damagePerTick;

                    if (breakProgress >= 1.0f) {
                        breakTargetBlock(world, renderer, particles, fluidPhysics, currentHit.bx, currentHit.by, currentHit.bz);
                        resetBreak();
                    }
                }
            } else {
                breakProgress = 0.0f;
                hitParticleTick = 0;
            }

            // 3. Handle Block Placement with Right Mouse Button (RMB Click)
            if (rmbDown && !wasRmbDown) {
                int placeX = currentHit.bx + currentHit.normalX;
                int placeY = currentHit.by + currentHit.normalY;
                int placeZ = currentHit.bz + currentHit.normalZ;

                byte blockToPlace = HOTBAR_BLOCKS[Math.clamp(player.selectedSlot, 0, 8)];

                // Check that placed block doesn't intersect player bounding box
                if (!isIntersectingPlayer(player, placeX, placeY, placeZ)) {
                    if (world.getBlockAt(placeX, placeY, placeZ) == Block.AIR || world.getBlockAt(placeX, placeY, placeZ) == Block.WATER) {
                        world.setBlockAt(placeX, placeY, placeZ, blockToPlace);
                        reuploadChunkMeshes(world, renderer, placeX, placeZ);
                        if (fluidPhysics != null) {
                            fluidPhysics.onBlockChanged(world, renderer, particles, placeX, placeY, placeZ);
                        }
                        System.out.println("[Building] 🧱 Placed " + Block.getName(blockToPlace) + " at (" + placeX + ", " + placeY + ", " + placeZ + ")");
                    }
                }
            }
        }

        wasLmbDown = lmbDown;
        wasRmbDown = rmbDown;
    }

    private void breakTargetBlock(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, FluidPhysicsManager fluidPhysics, int bx, int by, int bz) {
        byte brokenType = world.getBlockAt(bx, by, bz);
        if (brokenType != Block.AIR && brokenType != Block.BEDROCK) {
            world.setBlockAt(bx, by, bz, Block.AIR);
            reuploadChunkMeshes(world, renderer, bx, bz);

            // Spawn block disintegration debris particle cloud
            if (particles != null) {
                particles.spawnBlockBreakParticles(bx, by, bz, brokenType);
            }

            // Trigger water filling & flow physics
            if (fluidPhysics != null) {
                fluidPhysics.onBlockChanged(world, renderer, particles, bx, by, bz);
            }

            System.out.println("[Mining] ⛏ Broke " + Block.getName(brokenType) + " by hand at (" + bx + ", " + by + ", " + bz + ")!");
        }
    }

    private void reuploadChunkMeshes(ChunkManager world, ChunkRenderer renderer, int wx, int wz) {
        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wx, 16);

        renderer.uploadChunkMesh(cx, cz, world.getChunkMesh(cx, cz));

        if (lx == 0) renderer.uploadChunkMesh(cx - 1, cz, world.getChunkMesh(cx - 1, cz));
        if (lx == 15) renderer.uploadChunkMesh(cx + 1, cz, world.getChunkMesh(cx + 1, cz));
        if (lz == 0) renderer.uploadChunkMesh(cx, cz - 1, world.getChunkMesh(cx, cz - 1));
        if (lz == 15) renderer.uploadChunkMesh(cx, cz + 1, world.getChunkMesh(cx, cz + 1));
    }

    private boolean isIntersectingPlayer(Player player, int bx, int by, int bz) {
        float halfW = Player.WIDTH / 2.0f;
        float pMinX = player.pos.x - halfW;
        float pMaxX = player.pos.x + halfW;
        float pMinY = player.pos.y;
        float pMaxY = player.pos.y + Player.HEIGHT;
        float pMinZ = player.pos.z - halfW;
        float pMaxZ = player.pos.z + halfW;

        return (pMinX < bx + 1 && pMaxX > bx &&
                pMinY < by + 1 && pMaxY > by &&
                pMinZ < bz + 1 && pMaxZ > bz);
    }

    public void resetBreak() {
        targetBx = -1;
        targetBy = -1;
        targetBz = -1;
        breakProgress = 0.0f;
        hitParticleTick = 0;
    }

    public Raycast.Hit getCurrentHit() {
        return currentHit;
    }

    public float getBreakProgress() {
        return breakProgress;
    }
}
