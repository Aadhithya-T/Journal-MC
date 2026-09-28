package com.mcjournal;

import java.io.File;
import java.util.Collection;

/**
 * Handles chunk disk persistence via RegionManager.
 */
public class ChunkPersistence {
    private final RegionManager regionManager;

    public ChunkPersistence(File worldDir) {
        this.regionManager = (worldDir != null) ? new RegionManager(worldDir) : null;
    }

    public Chunk loadFromDisk(int cx, int cz) {
        if (regionManager == null) return null;
        return regionManager.loadChunk(cx, cz);
    }

    public void saveToDisk(Chunk chunk) {
        if (regionManager != null && chunk != null) {
            regionManager.saveChunk(chunk);
        }
    }

    public void saveAllDirty(Collection<Chunk> chunks) {
        if (regionManager == null || chunks == null) return;
        for (Chunk chunk : chunks) {
            if (chunk != null && chunk.isDirty()) {
                regionManager.saveChunk(chunk);
            }
        }
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public void close() {
        if (regionManager != null) {
            regionManager.close();
        }
    }
}
