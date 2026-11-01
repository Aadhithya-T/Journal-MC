package com.mcjournal;

import com.mcjournal.world.WorldBounds;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Handles chunk disk persistence via RegionManager (.jmc) with asynchronous save support (P9.4)
 * and bounds-aware write protection (Phase 4).
 */
public class ChunkPersistence {
    private final WorldBounds bounds;
    private final RegionManager regionManager;
    private final ExecutorService saveExecutor;

    public ChunkPersistence(File worldDir) {
        this(worldDir, WorldBounds.UNBOUNDED);
    }

    public ChunkPersistence(File worldDir, WorldBounds bounds) {
        this.bounds = (bounds != null) ? bounds : WorldBounds.UNBOUNDED;
        this.regionManager = (worldDir != null) ? new RegionManager(worldDir, this.bounds) : null;
        this.saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ChunkPersistence-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public WorldBounds getWorldBounds() {
        return bounds;
    }

    public Chunk loadFromDisk(int cx, int cz) {
        if (regionManager == null) return null;
        if (bounds != null && bounds.isBounded() && !bounds.isValidChunk(cx, cz)) {
            return null;
        }
        return regionManager.loadChunk(cx, cz);
    }

    public void saveToDisk(Chunk chunk) {
        if (chunk == null) return;
        if (bounds != null && bounds.isBounded() && !bounds.isValidChunk(chunk.getCx(), chunk.getCz())) {
            System.err.println("[ChunkPersistence] Refusing to persist invalid chunk: (" + chunk.getCx() + ", " + chunk.getCz() + ")");
            return;
        }
        if (regionManager != null) {
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
                if (bounds != null && bounds.isBounded() && !bounds.isValidChunk(chunk.getCx(), chunk.getCz())) {
                    continue; // Skip out-of-world chunks
                }
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
