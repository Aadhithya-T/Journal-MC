package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.world.WorldBounds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles terrain generation and voxel delta overlay for newly generated chunks.
 * Enforces finite world bounds so invalid chunk positions and invalid partial voxels
 * cannot be populated or mutated.
 */
public class ChunkGenerator {
    private final WorldBounds bounds;
    private final TerrainGenerator terrainGenerator;
    private final ChunkPersistence persistence;
    private final ConcurrentMap<ChunkPos, Map<Integer, BlockState>> chunkDeltas = new ConcurrentHashMap<>();

    public ChunkGenerator(long seed, ChunkPersistence persistence) {
        this(seed, persistence, WorldBounds.DEFAULT);
    }

    public ChunkGenerator(long seed, ChunkPersistence persistence, WorldBounds bounds) {
        this.bounds = (bounds != null) ? bounds : WorldBounds.DEFAULT;
        this.terrainGenerator = new TerrainGenerator(seed, this.bounds);
        this.persistence = persistence;
    }

    public WorldBounds getWorldBounds() {
        return bounds;
    }

    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    public Chunk generateChunk(ChunkPos pos) {
        if (!bounds.isValidChunk(pos)) {
            return new Chunk(pos.x(), pos.z());
        }

        Chunk chunk = null;
        if (persistence != null) {
            chunk = persistence.loadFromDisk(pos.x(), pos.z());
        }
        if (chunk == null) {
            chunk = terrainGenerator.generateChunk(pos.x(), pos.z());
        }

        int maxLx = bounds.getValidLocalBlockCountX(pos.x());
        int maxLz = bounds.getValidLocalBlockCountZ(pos.z());

        // Sanitize any invalid partial chunk voxels from disk loads
        if (bounds.isPartialChunk(pos)) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    for (int x = 0; x < Chunk.SIZE; x++) {
                        if (x >= maxLx || z >= maxLz) {
                            if (chunk.getStateId(x, y, z) != 0) {
                                chunk.setStateId(x, y, z, 0);
                            }
                        }
                    }
                }
            }
        }

        // Apply any saved block state deltas for this chunk, bounded strictly by valid coordinates
        Map<Integer, BlockState> deltas = chunkDeltas.get(pos);
        if (deltas != null) {
            short[] blockStates = chunk.getBlockStates();
            for (Map.Entry<Integer, BlockState> entry : deltas.entrySet()) {
                int idx = entry.getKey();
                if (idx >= 0 && idx < blockStates.length) {
                    int lx = idx % 16;
                    int rem = idx / 16;
                    int lz = rem % 16;
                    if (lx < maxLx && lz < maxLz) {
                        blockStates[idx] = (short) entry.getValue().getStateId();
                    }
                }
            }
            chunk.setDirty(true);
        }

        return chunk;
    }

    public void registerDelta(ChunkPos cpos, int idx, BlockState state) {
        if (cpos == null || !bounds.isValidChunk(cpos)) {
            return;
        }

        int lx = idx % 16;
        int rem = idx / 16;
        int lz = rem % 16;
        int y = rem / 16;

        if (!bounds.isLocalBlockValid(cpos.x(), cpos.z(), lx, y, lz)) {
            return; // Reject delta outside valid local partial chunk bounds
        }

        chunkDeltas.computeIfAbsent(cpos, k -> new ConcurrentHashMap<>()).put(idx, state);
    }

    public ConcurrentMap<ChunkPos, Map<Integer, BlockState>> getChunkDeltas() {
        return chunkDeltas;
    }
}
