package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

/**
 * Caches a 3x3 grid of chunks centered around a chunk being meshed.
 * Provides direct array access for voxel and neighbor queries,
 * eliminating all ConcurrentHashMap lookups, Math.floorDiv calls,
 * and ChunkPos object allocations during chunk meshing.
 */
public class ChunkNeighborhood {
    private final Chunk center;
    private final Chunk[][] chunks = new Chunk[3][3];
    private final short[][][] stateArrays = new short[3][3][];
    private final int centerCx;
    private final int centerCz;

    public ChunkNeighborhood(Chunk center, ChunkManager manager) {
        this.center = center;
        this.centerCx = center.getCx();
        this.centerCz = center.getCz();

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Chunk chunk;
                if (dx == 0 && dz == 0) {
                    chunk = center;
                } else if (manager != null) {
                    chunk = manager.getChunk(centerCx + dx, centerCz + dz);
                } else {
                    chunk = null;
                }
                chunks[dx + 1][dz + 1] = chunk;
                if (chunk != null) {
                    stateArrays[dx + 1][dz + 1] = chunk.getStateIds();
                }
            }
        }
    }

    public static ChunkNeighborhood of(Chunk center, ChunkManager manager) {
        return new ChunkNeighborhood(center, manager);
    }

    public Chunk getCenter() {
        return center;
    }

    public int getCenterCx() {
        return centerCx;
    }

    public int getCenterCz() {
        return centerCz;
    }

    /**
     * Fast direct-array lookup for state IDs.
     * Coordinates are local to the center chunk (lx in [-1..16], lz in [-1..16], y in [0..255]).
     */
    public int getStateId(int lx, int y, int lz) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0; // Air
        }

        // Fast path: inside center chunk
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
            short[] raw = stateArrays[1][1];
            if (raw == null) return 0;
            return raw[(y * 16 + lz) * 16 + lx] & 0xFFFF;
        }

        int chunkIdxX = 1;
        int chunkIdxZ = 1;

        if (lx < 0) {
            chunkIdxX = 0;
            lx += 16;
        } else if (lx >= 16) {
            chunkIdxX = 2;
            lx -= 16;
        }

        if (lz < 0) {
            chunkIdxZ = 0;
            lz += 16;
        } else if (lz >= 16) {
            chunkIdxZ = 2;
            lz -= 16;
        }

        // Out of neighborhood bounds safety check
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) {
            return 0;
        }

        short[] raw = stateArrays[chunkIdxX][chunkIdxZ];
        if (raw == null) {
            return 0; // Unloaded neighbor treated as Air
        }
        return raw[(y * 16 + lz) * 16 + lx] & 0xFFFF;
    }

    public BlockState getBlockState(int lx, int y, int lz) {
        int id = getStateId(lx, y, lz);
        return BlockStateRegistry.getStateById(id);
    }

    /**
     * Fast check whether voxel is solid for Ambient Occlusion.
     * (Solid and not foliage leaves).
     */
    public boolean isAOSolid(int lx, int y, int lz) {
        int stateId = getStateId(lx, y, lz);
        if (stateId == 0) return false;
        BlockState state = BlockStateRegistry.getStateById(stateId);
        return state.isSolid() && !state.is(Blocks.OAK_LEAVES) && !state.is(Blocks.BIRCH_LEAVES);
    }

    /**
     * Fast check whether voxel is transparent.
     */
    public boolean isTransparent(int lx, int y, int lz) {
        int stateId = getStateId(lx, y, lz);
        if (stateId == 0) return true;
        return BlockStateRegistry.getStateById(stateId).isTransparent();
    }

    /**
     * Fast check whether voxel is solid.
     */
    public boolean isSolid(int lx, int y, int lz) {
        int stateId = getStateId(lx, y, lz);
        if (stateId == 0) return false;
        return BlockStateRegistry.getStateById(stateId).isSolid();
    }

    /**
     * Fast check whether voxel is water.
     */
    public boolean isWater(int lx, int y, int lz) {
        int stateId = getStateId(lx, y, lz);
        if (stateId == 0) return false;
        return BlockStateRegistry.getStateById(stateId).is(Blocks.WATER);
    }

    public float computeWaterColumnDepth(int lx, int y, int lz) {
        int depth = 1;
        for (int dy = 1; dy <= 16; dy++) {
            int checkY = y - dy;
            if (checkY < 0) break;
            if (isWater(lx, checkY, lz)) {
                depth++;
            } else {
                break;
            }
        }
        return (float) depth;
    }

    public float computeWaterShoreline(int lx, int y, int lz) {
        if (isSolid(lx + 1, y, lz) ||
            isSolid(lx - 1, y, lz) ||
            isSolid(lx, y, lz + 1) ||
            isSolid(lx, y, lz - 1)) {
            return 1.0f;
        }
        return 0.0f;
    }
}
