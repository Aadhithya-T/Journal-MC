package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import org.joml.Vector3f;

public class Raycast {
    public static class Hit {
        public final int bx, by, bz;
        public final int normalX, normalY, normalZ;
        public final byte blockType;
        public final float distance;

        public Hit(int bx, int by, int bz, int normalX, int normalY, int normalZ, byte blockType, float distance) {
            this.bx = bx;
            this.by = by;
            this.bz = bz;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.blockType = blockType;
            this.distance = distance;
        }
    }

    /**
     * Amanatides & Woo Fast Voxel Traversal (DDA Raycast Algorithm)
     * Performs exact Minecraft voxel raycasting to find targeted block and hit face normal.
     */
    public static Hit cast(ChunkManager world, Vector3f origin, Vector3f dir, float maxDistance) {
        float dx = dir.x;
        float dy = dir.y;
        float dz = dir.z;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) return null;
        dx /= len;
        dy /= len;
        dz /= len;

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        float tDeltaX = (dx != 0) ? Math.abs(1.0f / dx) : Float.MAX_VALUE;
        float tDeltaY = (dy != 0) ? Math.abs(1.0f / dy) : Float.MAX_VALUE;
        float tDeltaZ = (dz != 0) ? Math.abs(1.0f / dz) : Float.MAX_VALUE;

        float tMaxX = (dx > 0) ? (x + 1.0f - origin.x) * tDeltaX : (origin.x - x) * tDeltaX;
        float tMaxY = (dy > 0) ? (y + 1.0f - origin.y) * tDeltaY : (origin.y - y) * tDeltaY;
        float tMaxZ = (dz > 0) ? (z + 1.0f - origin.z) * tDeltaZ : (origin.z - z) * tDeltaZ;

        int normalX = 0, normalY = 0, normalZ = 0;
        float dist = 0;

        while (dist <= maxDistance) {
            byte block = world.getBlockAt(x, y, z);
            if (Block.isSolid(block)) {
                return new Hit(x, y, z, normalX, normalY, normalZ, block, dist);
            }

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
        }

        return null;
    }
}
