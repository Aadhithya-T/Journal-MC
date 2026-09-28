package com.mcjournal;

/**
 * Immutable value type representing a Region file coordinate (rx, rz).
 * Each region file contains a 32x32 grid of chunks (512x512 blocks).
 */
public record RegionPos(int rx, int rz) {

    public static RegionPos fromChunkCoords(int cx, int cz) {
        return new RegionPos(Math.floorDiv(cx, EngineConstants.REGION_SIZE_CHUNKS), Math.floorDiv(cz, EngineConstants.REGION_SIZE_CHUNKS));
    }

    public static RegionPos fromWorldCoords(int wx, int wz) {
        return fromChunkCoords(Math.floorDiv(wx, EngineConstants.CHUNK_SIZE), Math.floorDiv(wz, EngineConstants.CHUNK_SIZE));
    }

    public int getLocalChunkX(int cx) {
        return Math.floorMod(cx, EngineConstants.REGION_SIZE_CHUNKS);
    }

    public int getLocalChunkZ(int cz) {
        return Math.floorMod(cz, EngineConstants.REGION_SIZE_CHUNKS);
    }

    public String getFileName() {
        return "r." + rx + "." + rz + ".jmc";
    }

    @Override
    public String toString() {
        return "Region(" + rx + ", " + rz + ")";
    }
}
