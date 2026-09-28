package com.mcjournal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks chunks whose voxel meshes need rebuilding following block mutations or fluid simulation ticks.
 * Deduplicates multiple modifications to the same chunk and automatically flags neighboring chunks
 * when boundary voxels (lx = 0, 15 or lz = 0, 15) are modified.
 */
public class DirtyChunkSet {
    private final Set<ChunkPos> dirtyChunks = ConcurrentHashMap.newKeySet();

    public void markDirty(ChunkPos pos) {
        if (pos != null) {
            dirtyChunks.add(pos);
        }
    }

    public void markDirty(int cx, int cz) {
        dirtyChunks.add(new ChunkPos(cx, cz));
    }

    /**
     * Marks the chunk containing the modified world block dirty, as well as any adjacent
     * neighbor chunks if the modified block lies on a chunk boundary or corner.
     */
    public void markBlockModified(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);
        int lx = Math.floorMod(wx, Chunk.SIZE);
        int lz = Math.floorMod(wz, Chunk.SIZE);

        markDirty(cx, cz);

        // Cardinal chunk boundaries
        if (lx == 0) markDirty(cx - 1, cz);
        if (lx == 15) markDirty(cx + 1, cz);
        if (lz == 0) markDirty(cx, cz - 1);
        if (lz == 15) markDirty(cx, cz + 1);

        // Diagonal chunk corners
        if (lx == 0 && lz == 0) markDirty(cx - 1, cz - 1);
        if (lx == 0 && lz == 15) markDirty(cx - 1, cz + 1);
        if (lx == 15 && lz == 0) markDirty(cx + 1, cz - 1);
        if (lx == 15 && lz == 15) markDirty(cx + 1, cz + 1);
    }

    /**
     * Atomically drains and returns all currently dirty chunk positions,
     * resetting the dirty tracking set for the next tick.
     */
    public Set<ChunkPos> drainDirtyChunks() {
        if (dirtyChunks.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ChunkPos> result = new HashSet<>(dirtyChunks);
        dirtyChunks.removeAll(result);
        return result;
    }

    public int size() {
        return dirtyChunks.size();
    }

    public boolean isEmpty() {
        return dirtyChunks.isEmpty();
    }

    public void clear() {
        dirtyChunks.clear();
    }
}
