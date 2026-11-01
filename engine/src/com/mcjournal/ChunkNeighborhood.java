package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;
import com.mcjournal.world.WorldBounds;

/**
 * Caches a 3x3 grid of chunks centered around a chunk being meshed.
 * Provides direct array access for voxel and neighbor queries,
 * eliminating all ConcurrentHashMap lookups, Math.floorDiv calls,
 * and ChunkPos object allocations during chunk meshing.
 *
 * Fully bounds-aware:
 * - Recognizes out-of-world coordinates and neighbor boundaries.
 * - Restricts partial chunks along the world perimeter.
 * - Out-of-world space is transparent air that does NOT darken AO.
 */
public class ChunkNeighborhood {
    private final Chunk center;
    private final Chunk[][] chunks = new Chunk[3][3];
    private final short[][][] stateArrays = new short[3][3][];
    private final WorldBounds bounds;
    private final int centerCx;
    private final int centerCz;
    private final int maxLx;
    private final int maxLz;

    public ChunkNeighborhood(Chunk center, ChunkManager manager) {
        this(center, manager, (manager != null) ? manager.getWorldBounds() : WorldBounds.DEFAULT);
    }

    public ChunkNeighborhood(Chunk center, ChunkManager manager, WorldBounds bounds) {
        this.center = center;
        this.bounds = (bounds != null) ? bounds : WorldBounds.DEFAULT;
        this.centerCx = center.getCx();
        this.centerCz = center.getCz();
        this.maxLx = this.bounds.getValidLocalBlockCountX(centerCx);
        this.maxLz = this.bounds.getValidLocalBlockCountZ(centerCz);

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int neighborCx = centerCx + dx;
                int neighborCz = centerCz + dz;

                Chunk chunk;
                if (dx == 0 && dz == 0) {
                    chunk = center;
                } else if (!this.bounds.isValidChunk(neighborCx, neighborCz)) {
                    chunk = null; // Strictly do not look up or query out-of-world chunks
                } else if (manager != null) {
                    chunk = manager.getChunk(neighborCx, neighborCz);
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

    public int getMaxLx() {
        return maxLx;
    }

    public int getMaxLz() {
        return maxLz;
    }

    public WorldBounds getWorldBounds() {
        return bounds;
    }

    /**
     * Checks if a neighbor chunk in cardinal direction (dirX, dirZ) is outside world bounds.
     * e.g. for center (0, 0), dirX = -1 returns true.
     */
    public boolean isNeighborOutOfWorld(int dirX, int dirZ) {
        return !bounds.isValidChunk(centerCx + dirX, centerCz + dirZ);
    }

    /**
     * Checks if a neighbor chunk in direction (dirX, dirZ) is loaded in memory.
     * Out-of-world chunks always return false.
     */
    public boolean isNeighborLoaded(int dirX, int dirZ) {
        if (dirX < -1 || dirX > 1 || dirZ < -1 || dirZ > 1) {
            return false;
        }
        if (dirX == 0 && dirZ == 0) return true;
        if (isNeighborOutOfWorld(dirX, dirZ)) return false;
        return chunks[dirX + 1][dirZ + 1] != null;
    }

    /**
     * Checks whether local coordinates relative to the center chunk are outside the finite world.
     */
    public boolean isOutOfWorld(int lx, int y, int lz) {
        if (y < 0 || y >= Chunk.HEIGHT) return true;
        int wx = centerCx * Chunk.SIZE + lx;
        int wz = centerCz * Chunk.SIZE + lz;
        return !bounds.isValidHorizontal(wx, wz);
    }

    /**
     * Fast direct-array lookup for state IDs.
     * Coordinates are local to the center chunk (lx in [-1..16], lz in [-1..16], y in [0..255]).
     * Automatically treats out-of-world blocks and unloaded chunks as Air (0).
     */
    public int getStateId(int lx, int y, int lz) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0; // Air
        }

        // Fast path: inside center chunk
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            if (lx >= maxLx || lz >= maxLz) {
                return 0; // Partial chunk invalid voxel is strictly Air
            }
            short[] raw = stateArrays[1][1];
            if (raw == null) return 0;
            return raw[(y * Chunk.SIZE + lz) * Chunk.SIZE + lx] & 0xFFFF;
        }

        int chunkIdxX = 1;
        int chunkIdxZ = 1;
        int targetLx = lx;
        int targetLz = lz;

        if (targetLx < 0) {
            chunkIdxX = 0;
            targetLx += Chunk.SIZE;
        } else if (targetLx >= Chunk.SIZE) {
            chunkIdxX = 2;
            targetLx -= Chunk.SIZE;
        }

        if (targetLz < 0) {
            chunkIdxZ = 0;
            targetLz += Chunk.SIZE;
        } else if (targetLz >= Chunk.SIZE) {
            chunkIdxZ = 2;
            targetLz -= Chunk.SIZE;
        }

        // Out of 3x3 neighborhood bounds safety check
        if (targetLx < 0 || targetLx >= Chunk.SIZE || targetLz < 0 || targetLz >= Chunk.SIZE) {
            return 0;
        }

        int targetCx = centerCx + (chunkIdxX - 1);
        int targetCz = centerCz + (chunkIdxZ - 1);

        if (!bounds.isValidChunk(targetCx, targetCz)) {
            return 0; // Out-of-world neighbor chunk treated as Air
        }

        if (targetLx >= bounds.getValidLocalBlockCountX(targetCx) ||
            targetLz >= bounds.getValidLocalBlockCountZ(targetCz)) {
            return 0; // Partial chunk invalid voxel in neighbor chunk treated as Air
        }

        short[] raw = stateArrays[chunkIdxX][chunkIdxZ];
        if (raw == null) {
            return 0; // Unloaded neighbor treated as Air
        }
        return raw[(y * Chunk.SIZE + targetLz) * Chunk.SIZE + targetLx] & 0xFFFF;
    }

    public BlockState getBlockState(int lx, int y, int lz) {
        int id = getStateId(lx, y, lz);
        return BlockStateRegistry.getStateById(id);
    }

    /**
     * Fast check whether voxel is solid for Ambient Occlusion.
     * (Solid and not foliage leaves).
     * Out-of-world space returns false so it does NOT darken edges.
     */
    public boolean isAOSolid(int lx, int y, int lz) {
        int stateId = getStateId(lx, y, lz);
        if (stateId == 0) return false;
        BlockState state = BlockStateRegistry.getStateById(stateId);
        return state.isSolid() && !state.is(Blocks.OAK_LEAVES) && !state.is(Blocks.BIRCH_LEAVES);
    }

    /**
     * Fast check whether voxel is transparent.
     * Out-of-world space is treated as transparent Air so perimeter faces render.
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
