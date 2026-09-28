package com.mcjournal;

import java.util.*;

/**
 * Handles chunk streaming offsets, distance calculations, and player tracking.
 */
public class ChunkStreamer {
    public static final int UNLOAD_PADDING = 3;

    private int renderDistance;
    private int unloadDistance;
    private volatile List<ChunkOffset> spiralOffsets;
    private volatile ChunkPos lastPlayerChunkPos = null;

    public record ChunkOffset(int dx, int dz, int distSq) implements Comparable<ChunkOffset> {
        @Override
        public int compareTo(ChunkOffset o) {
            return Integer.compare(this.distSq, o.distSq);
        }
    }

    public ChunkStreamer(int initialRenderDistance) {
        setRenderDistance(initialRenderDistance);
    }

    public synchronized void setRenderDistance(int newDistance) {
        this.renderDistance = Math.clamp(newDistance, 2, 24);
        this.unloadDistance = this.renderDistance + UNLOAD_PADDING;
        this.spiralOffsets = computeSpiralOffsets(this.renderDistance);
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public int getUnloadDistance() {
        return unloadDistance;
    }

    public List<ChunkOffset> getSpiralOffsets() {
        return spiralOffsets;
    }

    public ChunkPos getLastPlayerChunkPos() {
        return lastPlayerChunkPos;
    }

    public void setLastPlayerChunkPos(ChunkPos pos) {
        this.lastPlayerChunkPos = pos;
    }

    private static List<ChunkOffset> computeSpiralOffsets(int radius) {
        List<ChunkOffset> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= radius) {
                    offsets.add(new ChunkOffset(dx, dz, dx * dx + dz * dz));
                }
            }
        }
        Collections.sort(offsets);
        return Collections.unmodifiableList(offsets);
    }

    public double computePriority(ChunkPos candidate, ChunkPos playerPos, float lookDirX, float lookDirZ) {
        double dx = candidate.x() - playerPos.x();
        double dz = candidate.z() - playerPos.z();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return -1000.0; // Highest priority: player's current standing chunk

        // Normalize direction to candidate
        double ndx = dx / dist;
        double ndz = dz / dist;

        // Dot product with look direction (1.0 = directly ahead, -1.0 = behind)
        double lookLen = Math.sqrt(lookDirX * lookDirX + lookDirZ * lookDirZ);
        double dot = (lookLen > 0.001) ? (ndx * lookDirX + ndz * lookDirZ) / lookLen : 0.0;

        // Direction bonus prioritizes chunks ahead of the player
        double directionBonus = dot * 0.3 * renderDistance;
        return dist * 0.6 - directionBonus;
    }

    public PriorityQueue<ChunkPos> createPriorityQueue(ChunkPos playerPos, float lookDirX, float lookDirZ) {
        return new PriorityQueue<>(Comparator.comparingDouble(pos -> computePriority(pos, playerPos, lookDirX, lookDirZ)));
    }

    public List<ChunkPos> computeChunksToUnload(ChunkPos currentPos, Set<ChunkPos> loadedPositions) {
        List<ChunkPos> unloads = new ArrayList<>();
        for (ChunkPos pos : loadedPositions) {
            if (pos.distanceChebyshev(currentPos) > unloadDistance) {
                unloads.add(pos);
            }
        }
        return unloads;
    }
}
