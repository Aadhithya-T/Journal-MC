package com.mcjournal;

/**
 * Formal lifecycle states for chunks in the voxel world.
 */
public enum ChunkStatus {
    UNLOADED,
    QUEUED,
    GENERATING,
    GENERATED,
    MESHING,
    MESHED,
    GPU_LOADED,
    // Unload path
    UNLOAD_QUEUED,
    GPU_UNLOADED;

    public boolean isLoading() {
        return this == QUEUED || this == GENERATING || this == MESHING;
    }

    public boolean isReady() {
        return this == MESHED || this == GPU_LOADED;
    }

    public boolean isUnloading() {
        return this == UNLOAD_QUEUED || this == GPU_UNLOADED;
    }
}
