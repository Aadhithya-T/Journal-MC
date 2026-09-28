package com.mcjournal.physics;

/**
 * Axis-Aligned Bounding Box (AABB) for 3D physics collision and broad-phase queries.
 */
public class AABB {
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public static AABB fromPositionAndDimensions(float x, float y, float z, float width, float height) {
        float halfW = width / 2.0f;
        return new AABB(x - halfW, y, z - halfW, x + halfW, y + height, z + halfW);
    }

    public AABB offset(float dx, float dy, float dz) {
        return new AABB(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public AABB expand(float dx, float dy, float dz) {
        float nMinX = dx < 0 ? minX + dx : minX;
        float nMaxX = dx > 0 ? maxX + dx : maxX;
        float nMinY = dy < 0 ? minY + dy : minY;
        float nMaxY = dy > 0 ? maxY + dy : maxY;
        float nMinZ = dz < 0 ? minZ + dz : minZ;
        float nMaxZ = dz > 0 ? maxZ + dz : maxZ;
        return new AABB(nMinX, nMinY, nMinZ, nMaxX, nMaxY, nMaxZ);
    }

    public boolean intersects(AABB other) {
        if (other == null) return false;
        return this.maxX > other.minX && this.minX < other.maxX &&
               this.maxY > other.minY && this.minY < other.maxY &&
               this.maxZ > other.minZ && this.minZ < other.maxZ;
    }

    public boolean contains(float x, float y, float z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
}
