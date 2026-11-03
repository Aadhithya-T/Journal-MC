package com.mcjournal.client;

import com.mcjournal.ChunkManager;
import com.mcjournal.FluidPhysicsManager;
import com.mcjournal.Item;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Axis;
import org.joml.Vector3f;

/**
 * Handles non-mining block interactions: raycast block targeting and
 * right-click block placement from the player's inventory.
 *
 * Mining and block destruction mechanics are permanently removed.
 */
public class BlockPlacementManager {
    public static final float REACH_DISTANCE = 4.5f;

    private Raycast.Hit currentHit;
    private boolean wasRmbDown = false;

    public void update(ChunkManager world, ChunkRenderer renderer, ParticleManager particles,
                       FluidPhysicsManager fluidPhysics, FirstPersonHandRenderer hand,
                       Player player, Camera camera, boolean rmbDown) {
        if (player == null || camera == null || world == null) {
            currentHit = null;
            wasRmbDown = rmbDown;
            return;
        }

        // 1. Cast ray from player eye position along look direction to find targeted block
        Vector3f eyePos = player.getEyePosition(1.0f);
        Vector3f lookDir = camera.getLookDirection();
        currentHit = Raycast.cast(world, eyePos, lookDir, REACH_DISTANCE);

        // 2. Handle Block Placement with Right Mouse Button (RMB Click)
        if (currentHit != null && rmbDown && !wasRmbDown) {
            int placeX = currentHit.bx + currentHit.normalX;
            int placeY = currentHit.by + currentHit.normalY;
            int placeZ = currentHit.bz + currentHit.normalZ;

            byte blockToPlace = player.getSelectedBlock();

            // Validate item is a placeable block
            if (blockToPlace != 0 && !Item.isLegacyTool(blockToPlace)) {
                boolean canPlace = true;
                BlockType typeToPlace = BlockStateRegistry.getBlockType(blockToPlace);

                // Plant placement constraint: plants only on top of soil
                if (typeToPlace != null && typeToPlace.isPlant()) {
                    if (currentHit.normalY != 1) {
                        canPlace = false;
                    } else {
                        BlockState blockBelow = world.getBlockStateAt(placeX, placeY - 1, placeZ);
                        if (!blockBelow.is(Blocks.GRASS) && !blockBelow.is(Blocks.DIRT)) {
                            canPlace = false;
                        }
                    }
                }

                // Check collision intersection with player AABB
                if (canPlace && !isIntersectingPlayer(player, placeX, placeY, placeZ)) {
                    BlockState targetBlock = world.getBlockStateAt(placeX, placeY, placeZ);
                    if (targetBlock.isAir() || targetBlock.isWater()) {
                        BlockState stateToPlace = BlockStateRegistry.getDefaultState(blockToPlace);
                        if (stateToPlace.contains(BlockProperties.AXIS)) {
                            Axis axis = Axis.fromNormal(currentHit.normalX, currentHit.normalY, currentHit.normalZ);
                            stateToPlace = stateToPlace.with(BlockProperties.AXIS, axis);
                        }

                        if (world.setBlockStateAt(placeX, placeY, placeZ, stateToPlace)) {
                            player.consumeSelected();
                            if (renderer != null) {
                                reuploadChunkMeshes(world, renderer, placeX, placeZ);
                            }
                            if (fluidPhysics != null) {
                                fluidPhysics.onBlockChanged(world, renderer, particles, placeX, placeY, placeZ);
                            }
                            if (hand != null) {
                                hand.triggerSwing();
                            }
                            System.out.println("[Building] 🧱 Placed " + stateToPlace.getSerializedName() + " at (" + placeX + ", " + placeY + ", " + placeZ + ")");
                        }
                    }
                }
            }
        }

        wasRmbDown = rmbDown;
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

    public Raycast.Hit getCurrentHit() {
        return currentHit;
    }

    public void reset() {
        currentHit = null;
        wasRmbDown = false;
    }
}

