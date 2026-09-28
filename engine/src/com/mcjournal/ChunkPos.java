package com.mcjournal;

/**
 * Immutable value type representing a chunk coordinate (cx, cz).
 * Replaces string key allocations for high-performance chunk lookups.
 */
public record ChunkPos(int x, int z) {

    public static ChunkPos fromWorldCoords(int wx, int wz) {
        return new ChunkPos(Math.floorDiv(wx, 16), Math.floorDiv(wz, 16));
    }

    public static ChunkPos fromWorldCoords(float wx, float wz) {
        return new ChunkPos(Math.floorDiv((int) Math.floor(wx), 16), Math.floorDiv((int) Math.floor(wz), 16));
    }

    public int cx() {
        return x;
    }

    public int cz() {
        return z;
    }

    public int distanceChebyshev(ChunkPos other) {
        return Math.max(Math.abs(this.x - other.x), Math.abs(this.z - other.z));
    }

    public double distanceSquared(ChunkPos other) {
        double dx = this.x - other.x;
        double dz = this.z - other.z;
        return dx * dx + dz * dz;
    }

    public long asLong() {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    public static ChunkPos fromLong(long key) {
        return new ChunkPos((int) (key >> 32), (int) key);
    }

    @Override
    public String toString() {
        return x + "," + z;
    }
}
