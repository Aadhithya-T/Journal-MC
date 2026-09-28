package com.mcjournal;

import com.mcjournal.block.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles terrain generation and voxel delta overlay for newly generated chunks.
 */
public class ChunkGenerator {
    private final TerrainGenerator terrainGenerator;
    private final ChunkPersistence persistence;
    private final ConcurrentMap<ChunkPos, Map<Integer, BlockState>> chunkDeltas = new ConcurrentHashMap<>();

    public ChunkGenerator(long seed, ChunkPersistence persistence) {
        this.terrainGenerator = new TerrainGenerator(seed);
        this.persistence = persistence;
    }

    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    public Chunk generateChunk(ChunkPos pos) {
        Chunk chunk = null;
        if (persistence != null) {
            chunk = persistence.loadFromDisk(pos.x(), pos.z());
        }
        if (chunk == null) {
            chunk = terrainGenerator.generateChunk(pos.x(), pos.z());
        }

        // Apply any saved block state deltas for this chunk
        Map<Integer, BlockState> deltas = chunkDeltas.get(pos);
        if (deltas != null) {
            short[] blockStates = chunk.getBlockStates();
            for (Map.Entry<Integer, BlockState> entry : deltas.entrySet()) {
                int idx = entry.getKey();
                if (idx >= 0 && idx < blockStates.length) {
                    blockStates[idx] = (short) entry.getValue().getStateId();
                }
            }
            chunk.setDirty(true);
        }

        return chunk;
    }

    public void registerDelta(ChunkPos cpos, int idx, BlockState state) {
        chunkDeltas.computeIfAbsent(cpos, k -> new ConcurrentHashMap<>()).put(idx, state);
    }

    public ConcurrentMap<ChunkPos, Map<Integer, BlockState>> getChunkDeltas() {
        return chunkDeltas;
    }
}
