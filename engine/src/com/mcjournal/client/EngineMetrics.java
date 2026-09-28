package com.mcjournal.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance telemetry and engine metrics tracker (P12).
 * Tracks real-time FPS, authoritative TPS, asynchronous generation/meshing/upload timings,
 * visible and loaded chunk counts, draw calls, and rendered vertices.
 */
public class EngineMetrics {
    private static final EngineMetrics INSTANCE = new EngineMetrics();

    public static EngineMetrics getInstance() {
        return INSTANCE;
    }

    // Frame & Tick rates
    private volatile int fps = 60;
    private volatile int tps = 20;

    // Asynchronous Pipeline Durations (running exponential moving averages in ms)
    private volatile double chunkGenTimeMs = 0.0;
    private volatile double meshGenTimeMs = 0.0;
    private volatile double gpuUploadTimeMs = 0.0;

    // Chunk Lifecycle Counts
    private volatile int chunksLoaded = 0;
    private volatile int chunksGenerating = 0;
    private volatile int chunksMeshing = 0;
    private volatile int chunksVisible = 0;
    private volatile int dirtyChunks = 0;

    // Frame Draw Statistics
    private final AtomicInteger frameDrawCalls = new AtomicInteger(0);
    private final AtomicLong frameVerticesRendered = new AtomicLong(0);
    private final AtomicInteger frameGpuUploads = new AtomicInteger(0);
    private final AtomicInteger frameFluidUpdates = new AtomicInteger(0);

    // Latched stats for current frame display
    private volatile int lastDrawCalls = 0;
    private volatile long lastVerticesRendered = 0;
    private volatile int lastGpuUploads = 0;
    private volatile int lastFluidUpdates = 0;

    // Debug overlay toggle (F3 key)
    private boolean debugOverlayVisible = false;

    public void toggleDebugOverlay() {
        this.debugOverlayVisible = !this.debugOverlayVisible;
    }

    public boolean isDebugOverlayVisible() {
        return debugOverlayVisible;
    }

    public void setDebugOverlayVisible(boolean visible) {
        this.debugOverlayVisible = visible;
    }

    public void updateRates(int fps, int tps) {
        this.fps = fps;
        this.tps = tps;
    }

    public void recordChunkGenTime(double ms) {
        this.chunkGenTimeMs = (this.chunkGenTimeMs == 0.0) ? ms : (this.chunkGenTimeMs * 0.90 + ms * 0.10);
    }

    public void recordMeshGenTime(double ms) {
        this.meshGenTimeMs = (this.meshGenTimeMs == 0.0) ? ms : (this.meshGenTimeMs * 0.90 + ms * 0.10);
    }

    public void recordGpuUploadTime(double ms) {
        this.gpuUploadTimeMs = (this.gpuUploadTimeMs == 0.0) ? ms : (this.gpuUploadTimeMs * 0.90 + ms * 0.10);
    }

    public void recordGpuUpload() {
        frameGpuUploads.incrementAndGet();
    }

    public void recordFluidUpdates(int count) {
        frameFluidUpdates.addAndGet(count);
    }

    public void recordDrawCall(int vertexCount) {
        frameDrawCalls.incrementAndGet();
        frameVerticesRendered.addAndGet(vertexCount);
    }

    public void updateChunkCounts(int loaded, int visible, int generating, int meshing, int dirty) {
        this.chunksLoaded = loaded;
        this.chunksVisible = visible;
        this.chunksGenerating = generating;
        this.chunksMeshing = meshing;
        this.dirtyChunks = dirty;
    }

    public void endFrame() {
        this.lastDrawCalls = frameDrawCalls.getAndSet(0);
        this.lastVerticesRendered = frameVerticesRendered.getAndSet(0);
        this.lastGpuUploads = frameGpuUploads.getAndSet(0);
        this.lastFluidUpdates = frameFluidUpdates.getAndSet(0);
    }

    public int getFps() {
        return fps;
    }

    public int getTps() {
        return tps;
    }

    public int getChunksLoaded() {
        return chunksLoaded;
    }

    public int getChunksVisible() {
        return chunksVisible;
    }

    public int getChunksGenerating() {
        return chunksGenerating;
    }

    public int getChunksMeshing() {
        return chunksMeshing;
    }

    public int getDirtyChunks() {
        return dirtyChunks;
    }

    public int getLastDrawCalls() {
        return lastDrawCalls;
    }

    public long getLastVerticesRendered() {
        return lastVerticesRendered;
    }

    public int getLastGpuUploads() {
        return lastGpuUploads;
    }

    public int getLastFluidUpdates() {
        return lastFluidUpdates;
    }

    public double getChunkGenTimeMs() {
        return chunkGenTimeMs;
    }

    public double getMeshGenTimeMs() {
        return meshGenTimeMs;
    }

    public double getGpuUploadTimeMs() {
        return gpuUploadTimeMs;
    }

    /**
     * Formats vertex count in thousands (k) or millions (M).
     */
    public static String formatVertices(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fk", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}
