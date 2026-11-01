package com.mcjournal.world.structure;

import com.mcjournal.Chunk;
import com.mcjournal.EngineConstants;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.BlockState;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.biome.Biome;

import java.util.*;

/**
 * Deterministic structure placement engine for the finite voxel world.
 * Guarantees:
 * 1. Seed-driven determinism: same seed produces identical structure placements.
 * 2. Order independence: chunks generate their portion identically in any loading order.
 * 3. Multi-chunk structure integrity: structures spanning multiple chunks clip seamlessly.
 * 4. Zero global mutable state: safe for concurrent chunk worker generation.
 * 5. Respects WorldBounds and partial edge chunk footprints.
 */
public class StructurePlacementManager {

    private final long worldSeed;
    private final WorldBounds bounds;
    private final TerrainGenerator terrainGenerator;
    private final StructureRegistry registry;

    public StructurePlacementManager(long worldSeed, WorldBounds bounds,
                                     TerrainGenerator terrainGenerator, StructureRegistry registry) {
        this.worldSeed = worldSeed;
        this.bounds = (bounds != null) ? bounds : WorldBounds.DEFAULT;
        this.terrainGenerator = terrainGenerator;
        this.registry = (registry != null) ? registry : new StructureRegistry();
    }

    public StructureRegistry getRegistry() {
        return registry;
    }

    public WorldBounds getWorldBounds() {
        return bounds;
    }

    /**
     * Finds all authoritative structure placements whose horizontal bounding box intersects chunk (cx, cz).
     * Guaranteed to be globally consistent and completely independent of the query chunk or loading order.
     */
    public List<StructurePlacement> getPlacementsForChunk(int cx, int cz) {
        if (!bounds.isValidChunk(cx, cz)) {
            return Collections.emptyList();
        }

        Collection<StructureDefinition> definitions = registry.getAll();
        if (definitions.isEmpty()) {
            return Collections.emptyList();
        }

        int chunkMinX = cx * EngineConstants.CHUNK_SIZE;
        int chunkMaxX = chunkMinX + EngineConstants.CHUNK_SIZE - 1;
        int chunkMinZ = cz * EngineConstants.CHUNK_SIZE;
        int chunkMaxZ = chunkMinZ + EngineConstants.CHUNK_SIZE - 1;

        List<StructurePlacement> accepted = new ArrayList<>();

        for (StructureDefinition def : definitions) {
            int cellSize = def.getCellSize();
            int maxDim = Math.max(def.getTemplate().getWidth(), def.getTemplate().getDepth());

            int minCellX = Math.floorDiv(chunkMinX - maxDim, cellSize);
            int maxCellX = Math.floorDiv(chunkMaxX, cellSize);
            int minCellZ = Math.floorDiv(chunkMinZ - maxDim, cellSize);
            int maxCellZ = Math.floorDiv(chunkMaxZ, cellSize);

            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                    StructurePlacement candidate = evaluateCell(def, cellX, cellZ);
                    if (candidate != null && candidate.intersectsChunk(cx, cz)) {
                        if (isCandidateAccepted(candidate)) {
                            accepted.add(candidate);
                        }
                    }
                }
            }
        }

        return accepted;
    }

    /**
     * Deterministically populates structure blocks into the given chunk.
     * Respects partial chunk local limits and finite world bounds.
     */
    public void generateStructureBlocks(Chunk chunk, int cx, int cz) {
        if (chunk == null || !bounds.isValidChunk(cx, cz)) {
            return;
        }

        List<StructurePlacement> placements = getPlacementsForChunk(cx, cz);
        for (StructurePlacement placement : placements) {
            generatePlacementBlocks(chunk, cx, cz, placement);
        }
    }

    /**
     * Generates block placements for a specific structure within chunk (cx, cz).
     * Clips blocks strictly to the chunk's valid local boundaries and WorldBounds.
     */
    public void generatePlacementBlocks(Chunk chunk, int cx, int cz, StructurePlacement placement) {
        if (chunk == null || placement == null || !bounds.isValidChunk(cx, cz)) {
            return;
        }

        int chunkBaseX = cx * EngineConstants.CHUNK_SIZE;
        int chunkBaseZ = cz * EngineConstants.CHUNK_SIZE;

        StructureTemplate template = placement.getTransformedTemplate();
        int origX = placement.getOriginX();
        int origY = placement.getOriginY();
        int origZ = placement.getOriginZ();

        int tW = template.getWidth();
        int tH = template.getHeight();
        int tD = template.getDepth();

        for (int ty = 0; ty < tH; ty++) {
            int wy = origY + ty;
            if (!bounds.isValidVertical(wy)) continue;

            for (int tz = 0; tz < tD; tz++) {
                int wz = origZ + tz;
                if (wz < chunkBaseZ || wz >= chunkBaseZ + EngineConstants.CHUNK_SIZE) continue;
                int lz = wz - chunkBaseZ;

                for (int tx = 0; tx < tW; tx++) {
                    int wx = origX + tx;
                    if (wx < chunkBaseX || wx >= chunkBaseX + EngineConstants.CHUNK_SIZE) continue;
                    int lx = wx - chunkBaseX;

                    // Check valid finite coordinate and partial chunk footprint
                    if (bounds.isLocalBlockValid(cx, cz, lx, wy, lz)) {
                        BlockState block = template.getBlock(tx, ty, tz);
                        if (block != null) {
                            chunk.setBlockState(lx, wy, lz, block);
                        }
                    }
                }
            }
        }
    }

    /**
     * Evaluates whether a deterministic candidate placement should exist in (cellX, cellZ).
     */
    public StructurePlacement evaluateCell(StructureDefinition def, int cellX, int cellZ) {
        long cellHash = hashCell(worldSeed, def.getId(), cellX, cellZ);

        // 1. Spawn chance check
        double roll = hashToUnitDouble(cellHash, 0);
        if (roll > def.getSpawnChance()) {
            return null;
        }

        StructureTemplate template = def.getTemplate();
        int cellSize = def.getCellSize();

        // 2. Rotation & Mirror
        Rotation rotation = Rotation.values()[(int) (hashToLong(cellHash, 1) & 3)];
        Mirror mirror = (hashToLong(cellHash, 2) % 2 == 0) ? Mirror.NONE : Mirror.LEFT_RIGHT;

        int transW = rotation.transformedWidth(template.getWidth(), template.getDepth());
        int transD = rotation.transformedDepth(template.getWidth(), template.getDepth());

        // 3. Anchor coordinate inside cell
        int margin = 4;
        int maxOffsetX = Math.max(0, cellSize - transW - margin * 2);
        int maxOffsetZ = Math.max(0, cellSize - transD - margin * 2);

        int anchorX = cellX * cellSize + margin + (int) (hashToUnitDouble(cellHash, 3) * maxOffsetX);
        int anchorZ = cellZ * cellSize + margin + (int) (hashToUnitDouble(cellHash, 4) * maxOffsetZ);

        PlacementConstraints constraints = def.getConstraints();

        // 4. Horizontal WorldBounds check
        if (!bounds.isValidHorizontal(anchorX, anchorZ) ||
            !bounds.isValidHorizontal(anchorX + transW - 1, anchorZ + transD - 1)) {
            return null;
        }

        // 5. World boundary distance check
        if (bounds.distanceToEdge(anchorX, anchorZ) < constraints.getMinBoundaryDistance() ||
            bounds.distanceToEdge(anchorX + transW - 1, anchorZ + transD - 1) < constraints.getMinBoundaryDistance()) {
            return null;
        }

        // 6. Spawn exclusion zone
        double distToSpawn = Math.hypot(anchorX - EngineConstants.DEFAULT_SPAWN_X, anchorZ - EngineConstants.DEFAULT_SPAWN_Z);
        if (distToSpawn < constraints.getSpawnExclusionRadius()) {
            return null;
        }

        // 7. Biome compatibility check
        Biome centerBiome = terrainGenerator.getBiomeSampler().getBiome(anchorX + transW / 2, anchorZ + transD / 2);
        if (!constraints.getAllowedBiomes().isEmpty() && !constraints.getAllowedBiomes().contains(centerBiome.getId())) {
            return null;
        }
        if (constraints.getDisallowedBiomes().contains(centerBiome.getId())) {
            return null;
        }

        // 8. Elevation and terrain slope check
        int h00 = terrainGenerator.computeHeight(anchorX, anchorZ);
        int h10 = terrainGenerator.computeHeight(anchorX + transW - 1, anchorZ);
        int h01 = terrainGenerator.computeHeight(anchorX, anchorZ + transD - 1);
        int h11 = terrainGenerator.computeHeight(anchorX + transW - 1, anchorZ + transD - 1);

        int minH = Math.min(Math.min(h00, h10), Math.min(h01, h11));
        int maxH = Math.max(Math.max(h00, h10), Math.max(h01, h11));

        int anchorY = minH + 1; // Sit immediately on the ground

        if (anchorY < constraints.getMinElevation() || anchorY > constraints.getMaxElevation()) {
            return null;
        }

        if (anchorY + template.getHeight() - 1 >= Chunk.HEIGHT) {
            return null;
        }

        if ((maxH - minH) > constraints.getMaxSlope()) {
            return null;
        }

        if (!constraints.isAllowWater() && minH <= TerrainGenerator.SEA_LEVEL) {
            return null;
        }

        return new StructurePlacement(def, anchorX, anchorY, anchorZ, rotation, mirror);
    }

    /**
     * Determines whether a candidate placement is accepted under the global greedy independent set rule.
     * Evaluates all potentially conflicting candidates in its spatial neighborhood.
     * Independent of the chunk that initiated the query, guaranteeing world-wide determinism.
     */
    public boolean isCandidateAccepted(StructurePlacement candidate) {
        return isCandidateAccepted(candidate, 0);
    }

    private boolean isCandidateAccepted(StructurePlacement candidate, int depth) {
        if (candidate == null) return false;
        if (depth >= 8) {
            return true; // Depth limit reached, accept candidate
        }

        int pSpacing = candidate.getDefinition().getConstraints().getMinSpacing();

        for (StructureDefinition def : registry.getAll()) {
            int effSpacing = Math.max(pSpacing, def.getConstraints().getMinSpacing());
            int maxDim = Math.max(def.getTemplate().getWidth(), def.getTemplate().getDepth());
            int cellSize = def.getCellSize();

            int minCellX = Math.floorDiv(candidate.getMinX() - effSpacing - maxDim, cellSize);
            int maxCellX = Math.floorDiv(candidate.getMaxX() + effSpacing, cellSize);
            int minCellZ = Math.floorDiv(candidate.getMinZ() - effSpacing - maxDim, cellSize);
            int maxCellZ = Math.floorDiv(candidate.getMaxZ() + effSpacing, cellSize);

            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                for (int cx = minCellX; cx <= maxCellX; cx++) {
                    StructurePlacement other = evaluateCell(def, cx, cz);
                    if (other == null || isSamePlacement(candidate, other)) {
                        continue;
                    }

                    if (candidate.conflictsWith(other, effSpacing)) {
                        // If 'other' has higher rank under strict total ordering:
                        if (compareRank(other, candidate, worldSeed) > 0) {
                            // If the higher-ranking competitor is itself accepted, it eliminates 'candidate'
                            if (isCandidateAccepted(other, depth + 1)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Strict total ordering for candidates:
     * 1. Priority (descending)
     * 2. Cell hash (descending)
     * 3. Coordinate tie-breakers (X, Z, ID)
     */
    public static int compareRank(StructurePlacement a, StructurePlacement b, long seed) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        int pA = a.getDefinition().getConstraints().getPriority();
        int pB = b.getDefinition().getConstraints().getPriority();
        if (pA != pB) {
            return Integer.compare(pA, pB);
        }

        int cellXA = Math.floorDiv(a.getOriginX(), a.getDefinition().getCellSize());
        int cellZA = Math.floorDiv(a.getOriginZ(), a.getDefinition().getCellSize());
        long hashA = hashCell(seed, a.getDefinition().getId(), cellXA, cellZA);

        int cellXB = Math.floorDiv(b.getOriginX(), b.getDefinition().getCellSize());
        int cellZB = Math.floorDiv(b.getOriginZ(), b.getDefinition().getCellSize());
        long hashB = hashCell(seed, b.getDefinition().getId(), cellXB, cellZB);

        if (hashA != hashB) {
            return Long.compare(hashA, hashB);
        }

        if (a.getOriginX() != b.getOriginX()) return Integer.compare(a.getOriginX(), b.getOriginX());
        if (a.getOriginZ() != b.getOriginZ()) return Integer.compare(a.getOriginZ(), b.getOriginZ());
        return a.getDefinition().getId().compareTo(b.getDefinition().getId());
    }

    private static boolean isSamePlacement(StructurePlacement a, StructurePlacement b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getDefinition().getId().equals(b.getDefinition().getId()) &&
               a.getOriginX() == b.getOriginX() &&
               a.getOriginY() == b.getOriginY() &&
               a.getOriginZ() == b.getOriginZ();
    }

    /**
     * Resolves overlapping and conflicting candidate placements deterministically.
     */
    public List<StructurePlacement> resolveConflicts(List<StructurePlacement> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<StructurePlacement> accepted = new ArrayList<>();
        for (StructurePlacement c : candidates) {
            if (isCandidateAccepted(c)) {
                accepted.add(c);
            }
        }
        return accepted;
    }

    // --- Deterministic Pseudo-Random Hashing Functions ---

    public static long hashCell(long seed, String defId, int cellX, int cellZ) {
        long h = seed ^ (defId != null ? (long) defId.hashCode() * 0x9E3779B97F4A7C15L : 0L);
        h ^= (long) cellX * 0xC6A4A7935BD1E995L;
        h = Long.rotateLeft(h, 31);
        h ^= (long) cellZ * 0x85EBCA6B;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h;
    }

    private static long hashToLong(long baseHash, int salt) {
        long h = baseHash ^ ((long) salt * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h;
    }

    private static double hashToUnitDouble(long baseHash, int salt) {
        long val = hashToLong(baseHash, salt) & 0x7FFFFFFFFFFFFFFFL;
        return (double) val / (double) Long.MAX_VALUE;
    }
}
