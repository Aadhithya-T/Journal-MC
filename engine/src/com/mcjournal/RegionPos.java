package com.mcjournal;

/**
 * Immutable value type representing a Region file coordinate (rx, rz).
 * Each region file contains a 32x32 grid of chunks (512x512 blocks).
 */
public record RegionPos(int rx, int rz) {

    public static RegionPos fromChunkCoords(int cx, int cz) {
        return new RegionPos(Math.floorDiv(cx, 32), Math.floorDiv(cz, 32));
    }

    public static RegionPos fromWorldCoords(int wx, int wz) {
        return fromChunkCoords(Math.floorDiv(wx, 16), Math.floorDiv(wz, 16));
    }

    public int getLocalChunkX(int cx) {
        return Math.floorMod(cx, 32);
    }

    public int getLocalChunkZ(int cz) {
        return Math.floorMod(cz, 32);
    }

    public String getFileName() {
        return "r." + rx + "." + rz + ".jmc";
    }

    @Override
    public String toString() {
        return "Region(" + rx + ", " + rz + ")";
    }
}
