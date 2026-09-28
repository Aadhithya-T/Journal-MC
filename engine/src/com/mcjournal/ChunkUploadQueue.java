package com.mcjournal;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages pending chunk mesh GPU uploads and unloads with explicit budgeting
 * (max uploads per frame and max bytes per frame) to prevent framerate hitching.
 */
public class ChunkUploadQueue {
    public static final int DEFAULT_MAX_UPLOADS_PER_FRAME = 8;
    public static final int DEFAULT_MAX_BYTES_PER_FRAME = 4 * 1024 * 1024; // 4 MB per frame

    private final int maxUploadsPerFrame;
    private final int maxBytesPerFrame;

    private final ConcurrentLinkedQueue<UploadTask> pendingMeshUploads = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChunkPos> pendingMeshUnloads = new ConcurrentLinkedQueue<>();

    public record UploadTask(ChunkPos pos, int byteSize) {}

    public ChunkUploadQueue() {
        this(DEFAULT_MAX_UPLOADS_PER_FRAME, DEFAULT_MAX_BYTES_PER_FRAME);
    }

    public ChunkUploadQueue(int maxUploadsPerFrame, int maxBytesPerFrame) {
        this.maxUploadsPerFrame = maxUploadsPerFrame;
        this.maxBytesPerFrame = maxBytesPerFrame;
    }

    public void queueUpload(ChunkPos pos) {
        queueUpload(pos, 0);
    }

    public void queueUpload(ChunkPos pos, int byteSize) {
        if (pos != null) {
            pendingMeshUploads.offer(new UploadTask(pos, byteSize));
        }
    }

    public void queueUnload(ChunkPos pos) {
        if (pos != null) {
            pendingMeshUnloads.offer(pos);
        }
    }

    public UploadTask pollUploadTask() {
        return pendingMeshUploads.poll();
    }

    public ChunkPos pollUpload() {
        UploadTask task = pendingMeshUploads.poll();
        return task != null ? task.pos() : null;
    }

    public ChunkPos pollUnload() {
        return pendingMeshUnloads.poll();
    }

    public int getMaxUploadsPerFrame() {
        return maxUploadsPerFrame;
    }

    public int getMaxBytesPerFrame() {
        return maxBytesPerFrame;
    }

    public boolean hasPendingUploads() {
        return !pendingMeshUploads.isEmpty();
    }

    public boolean hasPendingUnloads() {
        return !pendingMeshUnloads.isEmpty();
    }

    public int getPendingUploadCount() {
        return pendingMeshUploads.size();
    }

    public int getPendingUnloadCount() {
        return pendingMeshUnloads.size();
    }

    public void clear() {
        pendingMeshUploads.clear();
        pendingMeshUnloads.clear();
    }
}
