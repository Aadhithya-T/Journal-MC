package com.mcjournal;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handles chunk disk persistence via RegionManager (.jmc) with asynchronous save support (P9.4).
 */
public class ChunkPersistence {
    private final RegionManager regionManager;
    private final ExecutorService saveExecutor;

    public ChunkPersistence(File worldDir) {
        this.regionManager = (worldDir != null) ? new RegionManager(worldDir) : null;
        this.saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ChunkPersistence-Worker");
            t.setDaemon(true);
            return t;
        });
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

    /**
     * Synchronous dirty chunk save (flushes to disk immediately).
     */
    public void saveAllDirty(Collection<Chunk> chunks) {
        try {
            saveAllDirtyAsync(chunks).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[ChunkPersistence] Synchronous save failed: " + e.getMessage());
        }
    }

    /**
     * Asynchronous dirty chunk saving (P9.4).
     * 1. Synchronously snapshots dirty chunk state arrays on the caller thread (fast, non-blocking).
     * 2. Clears dirty flag on active world chunks immediately.
     * 3. Dispatches compression and disk writes to a background worker.
     *
     * @param chunks Collection of chunks to inspect for dirty states
     * @return CompletableFuture completing when disk write is finalized
     */
    public CompletableFuture<Void> saveAllDirtyAsync(Collection<Chunk> chunks) {
        if (regionManager == null || chunks == null || chunks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 1. Snapshot dirty chunks synchronously on the caller thread
        List<Chunk> snapshots = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (chunk != null && chunk.isDirty()) {
                short[] rawStates = chunk.getStateIds();
                short[] copy = Arrays.copyOf(rawStates, rawStates.length);
                snapshots.add(new Chunk(chunk.getCx(), chunk.getCz(), copy));
                chunk.setDirty(false); // Clear dirty on game thread immediately
            }
        }

        if (snapshots.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 2. Offload serialization and disk write to background I/O thread
        return CompletableFuture.runAsync(() -> {
            for (Chunk snapshot : snapshots) {
                regionManager.saveChunk(snapshot);
            }
        }, saveExecutor);
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public void close() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (regionManager != null) {
            regionManager.close();
        }
    }
}
