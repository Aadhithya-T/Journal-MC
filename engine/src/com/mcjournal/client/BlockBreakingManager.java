package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import com.mcjournal.FluidPhysicsManager;
import com.mcjournal.Item;
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

    public void update(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, ItemEntityManager itemEntities, FluidPhysicsManager fluidPhysics, FirstPersonHandRenderer hand, Player player, Camera camera, boolean lmbDown, boolean rmbDown) {
        // 1. Cast ray from player eye position along look direction
        Vector3f eyePos = player.getEyePosition(1.0f);
        Vector3f lookDir = camera.getLookDirection();

        currentHit = Raycast.cast(world, eyePos, lookDir, REACH_DISTANCE);

        if (currentHit == null) {
            resetBreak();
            if (hand != null) {
                hand.setMining(false);
                if (lmbDown && !wasLmbDown) {
                    hand.triggerSwing(); // Swing at air
                }
            }
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
                    if (hand != null) hand.triggerSwing();
                    breakTargetBlock(world, renderer, particles, itemEntities, fluidPhysics, player, currentHit.bx, currentHit.by, currentHit.bz);
                    resetBreak();
                } else if (hardness > 0.0f) {
                    if (hand != null) hand.setMining(true);

                    // Spawn mining chip particles periodically while hitting
                    hitParticleTick++;
                    if (hitParticleTick % 4 == 0 && particles != null) {
                        particles.spawnMiningHitParticles(currentHit.bx, currentHit.by, currentHit.bz, currentHit.blockType, currentHit.normalX, currentHit.normalY, currentHit.normalZ);
                    }

                    // Minecraft Java Edition Mining Formula:
                    byte heldTool = player.getSelectedBlock();
                    float toolSpeed = Item.getMiningSpeedMultiplier(heldTool, currentHit.blockType);
                    boolean canHarvest = Item.canHarvest(heldTool, currentHit.blockType);

                    // If can harvest: damage = toolSpeed / (hardness * 30)
                    // If cannot harvest (wrong tool for pickaxe blocks): damage = 1.0 / (hardness * 100)
                    float damagePerTick;
                    if (canHarvest) {
                        damagePerTick = toolSpeed / (hardness * 30.0f);
                    } else {
                        damagePerTick = 1.0f / (hardness * 100.0f);
                    }

                    breakProgress += damagePerTick;

                    if (breakProgress >= 1.0f) {
                        breakTargetBlock(world, renderer, particles, itemEntities, fluidPhysics, player, currentHit.bx, currentHit.by, currentHit.bz);
                        resetBreak();
                    }
                }
            } else {
                breakProgress = 0.0f;
                hitParticleTick = 0;
                if (hand != null) hand.setMining(false);
            }

            // 3. Handle Block Placement with Right Mouse Button (RMB Click)
            if (rmbDown && !wasRmbDown) {
                int placeX = currentHit.bx + currentHit.normalX;
                int placeY = currentHit.by + currentHit.normalY;
                int placeZ = currentHit.bz + currentHit.normalZ;

                byte blockToPlace = player.getSelectedBlock();

                // Tools cannot be placed into the world as blocks
                if (blockToPlace != Block.AIR && !Item.isTool(blockToPlace)) {
                    // Check plant placement validity (flowers and tall grass cannot be placed in air, on walls/ceilings, or on non-soil)
                    boolean canPlace = true;
                    if (Block.isPlant(blockToPlace)) {
                        // Plants can only be placed upright on top of soil (Grass Block or Dirt)
                        if (currentHit.normalY != 1) {
                            canPlace = false;
                        } else {
                            byte blockBelow = world.getBlockAt(placeX, placeY - 1, placeZ);
                            if (!Block.canPlantSurviveOn(blockBelow)) {
                                canPlace = false;
                            }
                        }
                    }

                    // Check that placed block doesn't intersect player bounding box
                    if (canPlace && !isIntersectingPlayer(player, placeX, placeY, placeZ)) {
                        byte targetBlock = world.getBlockAt(placeX, placeY, placeZ);
                        if (targetBlock == Block.AIR || targetBlock == Block.WATER) {
                            world.setBlockAt(placeX, placeY, placeZ, blockToPlace);
                            player.consumeSelected();
                            reuploadChunkMeshes(world, renderer, placeX, placeZ);
                            if (fluidPhysics != null) {
                                fluidPhysics.onBlockChanged(world, renderer, particles, placeX, placeY, placeZ);
                            }
                            System.out.println("[Building] 🧱 Placed " + Block.getName(blockToPlace) + " at (" + placeX + ", " + placeY + ", " + placeZ + ")");
                        }
                    }
                }
            }
        }

        wasLmbDown = lmbDown;
        wasRmbDown = rmbDown;
    }

    private void breakTargetBlock(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, ItemEntityManager itemEntities, FluidPhysicsManager fluidPhysics, Player player, int bx, int by, int bz) {
        byte brokenType = world.getBlockAt(bx, by, bz);
        if (brokenType != Block.AIR && brokenType != Block.BEDROCK) {
            world.setBlockAt(bx, by, bz, Block.AIR);
            reuploadChunkMeshes(world, renderer, bx, bz);

            // Spawn dropped block item entity if player can harvest this block with held tool
            byte heldTool = player.getSelectedBlock();
            boolean canHarvest = Item.canHarvest(heldTool, brokenType);
            if (canHarvest) {
                byte dropType = Block.getDrop(brokenType);
                if (dropType != Block.AIR && itemEntities != null) {
                    itemEntities.spawnItem(dropType, 1, bx + 0.5f, by + 0.3f, bz + 0.5f);
                }
            }

            // Spawn block disintegration debris particle cloud
            if (particles != null) {
                particles.spawnBlockBreakParticles(bx, by, bz, brokenType);
            }

            // If a plant was resting on top of this block, break it too & drop it
            byte blockAbove = world.getBlockAt(bx, by + 1, bz);
            if (Block.isPlant(blockAbove)) {
                world.setBlockAt(bx, by + 1, bz, Block.AIR);
                byte plantDrop = Block.getDrop(blockAbove);
                if (plantDrop != Block.AIR && itemEntities != null) {
                    itemEntities.spawnItem(plantDrop, 1, bx + 0.5f, by + 1.3f, bz + 0.5f);
                }
                if (particles != null) {
                    particles.spawnBlockBreakParticles(bx, by + 1, bz, blockAbove);
                }
                reuploadChunkMeshes(world, renderer, bx, bz);
            }

            // Trigger water filling & flow physics
            if (fluidPhysics != null) {
                fluidPhysics.onBlockChanged(world, renderer, particles, bx, by, bz);
            }

            String toolDesc = (heldTool == Block.AIR) ? "by hand" : ("with " + (Item.isTool(heldTool) ? Item.getName(heldTool) : Block.getName(heldTool)));
            System.out.println("[Mining] ⛏ Broke " + Block.getName(brokenType) + " " + toolDesc + " at (" + bx + ", " + by + ", " + bz + ")!");
        }
    }

    private void reuploadChunkMeshes(ChunkManager world, ChunkRenderer renderer, int wx, int wz) {
        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        renderer.uploadChunkMesh(cx, cz, world.getChunkMesh(cx, cz));

        if (lx == 0) renderer.uploadChunkMesh(cx - 1, cz, world.getChunkMesh(cx - 1, cz));
        if (lx == 15) renderer.uploadChunkMesh(cx + 1, cz, world.getChunkMesh(cx + 1, cz));
        if (lz == 0) renderer.uploadChunkMesh(cx, cz - 1, world.getChunkMesh(cx, cz - 1));
        if (lz == 15) renderer.uploadChunkMesh(cx, cz + 1, world.getChunkMesh(cx, cz + 1));

        if (lx == 0 && lz == 0) renderer.uploadChunkMesh(cx - 1, cz - 1, world.getChunkMesh(cx - 1, cz - 1));
        if (lx == 0 && lz == 15) renderer.uploadChunkMesh(cx - 1, cz + 1, world.getChunkMesh(cx - 1, cz + 1));
        if (lx == 15 && lz == 0) renderer.uploadChunkMesh(cx + 1, cz - 1, world.getChunkMesh(cx + 1, cz - 1));
        if (lx == 15 && lz == 15) renderer.uploadChunkMesh(cx + 1, cz + 1, world.getChunkMesh(cx + 1, cz + 1));
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
