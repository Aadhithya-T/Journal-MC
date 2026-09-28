package com.mcjournal.client;

import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.WorldBlockPos;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.property.Direction;
import org.joml.Vector3f;

public class Raycast {

    /**
     * Backward-compatible alias for RaycastHit (P8.2).
     */
    public static class Hit extends RaycastHit {
        public Hit(ChunkPos chunk, WorldBlockPos block, Direction face, Vector3f hitPosition, float distance, BlockState state) {
            super(chunk, block, face, hitPosition, distance, state);
        }

        public Hit(int bx, int by, int bz, int normalX, int normalY, int normalZ, BlockState state, float distance) {
            super(bx, by, bz, normalX, normalY, normalZ, state, distance);
        }

        @Deprecated
        public Hit(int bx, int by, int bz, int normalX, int normalY, int normalZ, byte blockType, float distance) {
            super(bx, by, bz, normalX, normalY, normalZ, com.mcjournal.block.BlockStateRegistry.getDefaultState(blockType), distance);
        }

        public com.mcjournal.block.BlockType getBlockTypeLegacy() {
            return getBlockType();
        }
    }

    /**
     * Amanatides & Woo Fast Voxel Traversal (DDA Raycast Algorithm) (P8.1 & P8.2)
     * Performs exact Minecraft voxel raycasting to find targeted block and hit face normal.
     *
     * @param world The chunk manager / world provider
     * @param origin Starting point of the ray
     * @param dir Direction vector (does not need to be normalized, but must not be zero)
     * @param maxDistance Maximum reach distance in blocks
     * @return Structured RaycastHit if an obstacle was struck, or null if no hit occurred
     */
    public static Hit cast(ChunkManager world, Vector3f origin, Vector3f dir, float maxDistance) {
        if (world == null || origin == null || dir == null) return null;

        float dx = dir.x;
        float dy = dir.y;
        float dz = dir.z;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.00001f || Float.isNaN(len)) return null;
        dx /= len;
        dy /= len;
        dz /= len;

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Immediate check: is the ray origin already inside a solid block?
        BlockState startState = world.getBlockStateAt(x, y, z);
        if (!startState.isAir() && !startState.isWater()) {
            return new Hit(
                new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16)),
                new WorldBlockPos(x, y, z),
                Direction.UP,
                new Vector3f(origin),
                0.0f,
                startState
            );
        }

        int stepX = (dx > 0) ? 1 : (dx < 0 ? -1 : 0);
        int stepY = (dy > 0) ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = (dz > 0) ? 1 : (dz < 0 ? -1 : 0);

        float tDeltaX = (dx != 0) ? Math.abs(1.0f / dx) : Float.MAX_VALUE;
        float tDeltaY = (dy != 0) ? Math.abs(1.0f / dy) : Float.MAX_VALUE;
        float tDeltaZ = (dz != 0) ? Math.abs(1.0f / dz) : Float.MAX_VALUE;

        float tMaxX = (dx > 0) ? (x + 1.0f - origin.x) * tDeltaX : ((dx < 0) ? (origin.x - x) * tDeltaX : Float.MAX_VALUE);
        float tMaxY = (dy > 0) ? (y + 1.0f - origin.y) * tDeltaY : ((dy < 0) ? (origin.y - y) * tDeltaY : Float.MAX_VALUE);
        float tMaxZ = (dz > 0) ? (z + 1.0f - origin.z) * tDeltaZ : ((dz < 0) ? (origin.z - z) * tDeltaZ : Float.MAX_VALUE);

        int normalX = 0, normalY = 0, normalZ = 0;
        float dist = 0.0f;

        while (dist <= maxDistance) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                    x += stepX;
                    normalX = -stepX;
                    normalY = 0;
                    normalZ = 0;
                } else {
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                    y += stepY;
                    normalX = 0;
                    normalY = -stepY;
                    normalZ = 0;
                } else {
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    normalX = 0;
                    normalY = 0;
                    normalZ = -stepZ;
                }
            }

            if (dist > maxDistance) break;

            BlockState state = world.getBlockStateAt(x, y, z);
            if (!state.isAir() && !state.isWater()) {
                Vector3f hitPos = new Vector3f(
                    origin.x + dx * dist,
                    origin.y + dy * dist,
                    origin.z + dz * dist
                );
                Direction face = Direction.fromNormal(normalX, normalY, normalZ);
                return new Hit(
                    new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16)),
                    new WorldBlockPos(x, y, z),
                    face,
                    hitPos,
                    dist,
                    state
                );
            }
        }

        return null;
    }
}
