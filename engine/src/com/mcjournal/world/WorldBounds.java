package com.mcjournal.world;

import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.EngineConstants;

/**
 * Centralized spatial bounds validator and coordinate conversion authority for finite worlds.
 * Enforces valid block coordinates:
 *   0 <= x < width   (e.g. 0 to 2499)
 *   0 <= z < depth   (e.g. 0 to 2499)
 *   0 <= y < height  (e.g. 0 to 255)
 */
public final class WorldBounds {

    /**
     * Tri-state coordinate status differentiating between outside world,
     * valid inside world but unloaded, and loaded.
     */
    public enum CoordinateStatus {
        LOADED,
        UNLOADED,
        OUT_OF_WORLD
    }

    private final WorldConfiguration config;
    private final boolean bounded;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private final int totalChunksX;
    private final int totalChunksZ;
    private final int totalChunks;

    public static final WorldBounds DEFAULT = new WorldBounds(WorldConfiguration.createDefault(0L), true);
    public static final WorldBounds UNBOUNDED = new WorldBounds(
        new WorldConfiguration(Integer.MAX_VALUE, Integer.MAX_VALUE, 256, 0L, 1,
            WorldConfiguration.BoundaryType.NATURAL, 0, 66, 0),
        false
    );

    public WorldBounds(WorldConfiguration config) {
        this(config, true);
    }

    public WorldBounds(WorldConfiguration config, boolean bounded) {
        if (config == null) {
            throw new IllegalArgumentException("WorldConfiguration cannot be null");
        }
        this.config = config;
        this.bounded = bounded;
        this.minChunkX = 0;
        this.minChunkZ = 0;
        this.maxChunkX = bounded ? (Math.ceilDiv(config.width(), EngineConstants.CHUNK_SIZE) - 1) : Integer.MAX_VALUE;
        this.maxChunkZ = bounded ? (Math.ceilDiv(config.depth(), EngineConstants.CHUNK_SIZE) - 1) : Integer.MAX_VALUE;
        this.totalChunksX = bounded ? (maxChunkX + 1) : Integer.MAX_VALUE;
        this.totalChunksZ = bounded ? (maxChunkZ + 1) : Integer.MAX_VALUE;
        this.totalChunks = bounded ? (totalChunksX * totalChunksZ) : Integer.MAX_VALUE;
    }

    public boolean isBounded() {
        return bounded;
    }

    public WorldConfiguration getConfig() {
        return config;
    }

    public int getWidth() {
        return config.width();
    }

    public int getDepth() {
        return config.depth();
    }

    public int getHeight() {
        return config.height();
    }

    public int getMinChunkX() {
        return minChunkX;
    }

    public int getMaxChunkX() {
        return maxChunkX;
    }

    public int getMinChunkZ() {
        return minChunkZ;
    }

    public int getMaxChunkZ() {
        return maxChunkZ;
    }

    public int getTotalChunksX() {
        return totalChunksX;
    }

    public int getTotalChunksZ() {
        return totalChunksZ;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    // --- Block Coordinate Validation ---

    /**
     * Checks if horizontal (X, Z) coordinates fall strictly within world boundaries.
     * Negative coordinates are strictly rejected when bounded.
     */
    public boolean isValidHorizontal(int x, int z) {
        if (!bounded) return true;
        return x >= 0 && x < config.width() && z >= 0 && z < config.depth();
    }

    /**
     * Checks if vertical Y coordinate falls strictly within world height bounds [0, height - 1].
     */
    public boolean isValidVertical(int y) {
        return y >= 0 && y < config.height();
    }

    /**
     * Checks if 3D block coordinate (X, Y, Z) falls strictly within world boundaries.
     */
    public boolean isValidBlock(int x, int y, int z) {
        return isValidHorizontal(x, z) && isValidVertical(y);
    }

    // --- Chunk Coordinate Validation ---

    /**
     * Checks if chunk coordinate (cx, cz) falls strictly within valid chunk range.
     * For 2500x2500, cx in [0, 156] and cz in [0, 156].
     */
    public boolean isValidChunk(int cx, int cz) {
        if (!bounded) return true;
        return cx >= minChunkX && cx <= maxChunkX && cz >= minChunkZ && cz <= maxChunkZ;
    }

    public boolean isValidChunk(ChunkPos pos) {
        return pos != null && isValidChunk(pos.x(), pos.z());
    }

    // --- Partial Edge Chunk Queries ---

    /**
     * A chunk is partial along X if it is the boundary chunk and width is not a multiple of 16.
     * For 2500 width: 2500 % 16 = 4, so chunk 156 is partial (valid local X: 0..3).
     */
    public boolean isPartialChunkX(int cx) {
        if (!bounded) return false;
        return cx == maxChunkX && (config.width() % EngineConstants.CHUNK_SIZE != 0);
    }

    /**
     * A chunk is partial along Z if it is the boundary chunk and depth is not a multiple of 16.
     */
    public boolean isPartialChunkZ(int cz) {
        if (!bounded) return false;
        return cz == maxChunkZ && (config.depth() % EngineConstants.CHUNK_SIZE != 0);
    }

    /**
     * Checks if chunk (cx, cz) is partial along either horizontal axis.
     */
    public boolean isPartialChunk(int cx, int cz) {
        return isValidChunk(cx, cz) && (isPartialChunkX(cx) || isPartialChunkZ(cz));
    }

    public boolean isPartialChunk(ChunkPos pos) {
        return pos != null && isPartialChunk(pos.x(), pos.z());
    }

    /**
     * Returns the count of valid local blocks along X inside chunk cx.
     * For full interior chunks, returns 16.
     * For partial chunk cx=156 in a 2500-wide world, returns 4.
     * For invalid chunks, returns 0.
     */
    public int getValidLocalBlockCountX(int cx) {
        if (!bounded) return EngineConstants.CHUNK_SIZE;
        if (cx < minChunkX || cx > maxChunkX) return 0;
        if (cx == maxChunkX) {
            int rem = config.width() % EngineConstants.CHUNK_SIZE;
            return (rem == 0) ? EngineConstants.CHUNK_SIZE : rem;
        }
        return EngineConstants.CHUNK_SIZE;
    }

    /**
     * Returns the count of valid local blocks along Z inside chunk cz.
     */
    public int getValidLocalBlockCountZ(int cz) {
        if (!bounded) return EngineConstants.CHUNK_SIZE;
        if (cz < minChunkZ || cz > maxChunkZ) return 0;
        if (cz == maxChunkZ) {
            int rem = config.depth() % EngineConstants.CHUNK_SIZE;
            return (rem == 0) ? EngineConstants.CHUNK_SIZE : rem;
        }
        return EngineConstants.CHUNK_SIZE;
    }

    /**
     * Returns the maximum valid local X coordinate in chunk cx (inclusive).
     * For full chunks: 15. For partial chunk 156 in 2500-wide world: 3.
     * For invalid chunks: -1.
     */
    public int getValidLocalMaxX(int cx) {
        int count = getValidLocalBlockCountX(cx);
        return count > 0 ? count - 1 : -1;
    }

    /**
     * Returns the maximum valid local Z coordinate in chunk cz (inclusive).
     */
    public int getValidLocalMaxZ(int cz) {
        int count = getValidLocalBlockCountZ(cz);
        return count > 0 ? count - 1 : -1;
    }

    /**
     * Checks whether local voxel coordinates (lx, y, lz) in chunk (cx, cz) are inside the finite world.
     */
    public boolean isLocalBlockValid(int cx, int cz, int lx, int y, int lz) {
        if (!isValidVertical(y) || !isValidChunk(cx, cz)) return false;
        return lx >= 0 && lx < getValidLocalBlockCountX(cx)
            && lz >= 0 && lz < getValidLocalBlockCountZ(cz);
    }

    // --- Coordinate Conversions ---

    /**
     * Converts world block X to chunk X using standard floor division.
     * Note: Does NOT clamp. Caller should verify with {@link #isValidBlock} or {@link #isValidHorizontal}.
     */
    public static int toChunkX(int wx) {
        return Math.floorDiv(wx, EngineConstants.CHUNK_SIZE);
    }

    /**
     * Converts world block Z to chunk Z using standard floor division.
     */
    public static int toChunkZ(int wz) {
        return Math.floorDiv(wz, EngineConstants.CHUNK_SIZE);
    }

    /**
     * Converts world block X to local chunk block X in [0..15] using floorMod.
     */
    public static int toLocalX(int wx) {
        return Math.floorMod(wx, EngineConstants.CHUNK_SIZE);
    }

    /**
     * Converts world block Z to local chunk block Z in [0..15] using floorMod.
     */
    public static int toLocalZ(int wz) {
        return Math.floorMod(wz, EngineConstants.CHUNK_SIZE);
    }

    /**
     * Converts chunk coordinate and local offset to world block X.
     */
    public static int toWorldX(int cx, int lx) {
        return cx * EngineConstants.CHUNK_SIZE + lx;
    }

    /**
     * Converts chunk coordinate and local offset to world block Z.
     */
    public static int toWorldZ(int cz, int lz) {
        return cz * EngineConstants.CHUNK_SIZE + lz;
    }

    // --- Distance to World Edges & Natural Boundary Presentation Hooks ---

    /**
     * Edge zone classification for future natural boundary presentation.
     * Can be queried by terrain/biome generators, atmospheric distance fog, and ambient audio.
     */
    public enum EdgeZone {
        CORE,           // Far from any boundary (> 200 blocks)
        APPROACH,       // Transitioning towards edge (64 to 200 blocks)
        PERIMETER,      // Near boundary cliffs/ocean (16 to 64 blocks)
        BORDER          // Immediately on the boundary (0 to 16 blocks)
    }

    /**
     * Categorizes an (X, Z) coordinate into an EdgeZone for boundary visual transitions.
     */
    public EdgeZone getEdgeZone(int wx, int wz) {
        if (!bounded) return EdgeZone.CORE;
        int dist = distanceToEdge(wx, wz);
        if (dist <= 16) return EdgeZone.BORDER;
        if (dist <= 64) return EdgeZone.PERIMETER;
        if (dist <= 200) return EdgeZone.APPROACH;
        return EdgeZone.CORE;
    }

    /**
     * Calculates the minimum block distance to any of the four world borders.
     * If coordinate is inside world, returns a non-negative distance:
     *   distance == 0: exactly on border (e.g. x=0, x=2499, z=0, z=2499)
     *   distance > 0: inside the world
     * If coordinate is outside the world, returns the negative distance to the nearest border.
     */
    public int distanceToEdge(int wx, int wz) {
        int distWest = wx;                         // x = 0
        int distEast = (config.width() - 1) - wx;   // x = max
        int distNorth = wz;                        // z = 0
        int distSouth = (config.depth() - 1) - wz; // z = max

        return Math.min(Math.min(distWest, distEast), Math.min(distNorth, distSouth));
    }

    /**
     * Normalized distance from edge (0.0 at borders, 1.0 at world center).
     * Returns 0.0 for any position outside or exactly on the border.
     */
    public double distanceToEdgeNormalized(int wx, int wz) {
        int dist = distanceToEdge(wx, wz);
        if (dist <= 0) return 0.0;
        double maxDist = Math.min(config.width(), config.depth()) / 2.0;
        return Math.clamp(dist / maxDist, 0.0, 1.0);
    }

    // --- Access Semantics (OUT_OF_WORLD vs UNLOADED vs LOADED) ---

    /**
     * Differentiates whether a coordinate is out of the world, valid but in an unloaded chunk,
     * or valid and loaded in memory.
     */
    public CoordinateStatus getCoordinateStatus(ChunkManager chunkManager, int wx, int wy, int wz) {
        if (!isValidBlock(wx, wy, wz)) {
            return CoordinateStatus.OUT_OF_WORLD;
        }
        if (chunkManager == null) {
            return CoordinateStatus.UNLOADED;
        }
        int cx = toChunkX(wx);
        int cz = toChunkZ(wz);
        return chunkManager.isChunkLoaded(cx, cz) ? CoordinateStatus.LOADED : CoordinateStatus.UNLOADED;
    }

    /**
     * Differentiates whether a chunk coordinate is out of the world, in an unloaded state,
     * or loaded in memory.
     */
    public CoordinateStatus getChunkCoordinateStatus(ChunkManager chunkManager, int cx, int cz) {
        if (!isValidChunk(cx, cz)) {
            return CoordinateStatus.OUT_OF_WORLD;
        }
        if (chunkManager == null) {
            return CoordinateStatus.UNLOADED;
        }
        return chunkManager.isChunkLoaded(cx, cz) ? CoordinateStatus.LOADED : CoordinateStatus.UNLOADED;
    }

    // --- Explicit Clamping Helpers (For safe camera/spawn positioning) ---

    public int clampBlockX(int x) {
        return Math.clamp(x, 0, config.width() - 1);
    }

    public int clampBlockZ(int z) {
        return Math.clamp(z, 0, config.depth() - 1);
    }

    public int clampBlockY(int y) {
        return Math.clamp(y, 0, config.height() - 1);
    }

    public float clampEntityX(float x, float margin) {
        return Math.clamp(x, margin, (float) config.width() - margin);
    }

    public float clampEntityZ(float z, float margin) {
        return Math.clamp(z, margin, (float) config.depth() - margin);
    }
}
