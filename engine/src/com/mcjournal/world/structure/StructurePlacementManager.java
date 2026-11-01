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

        List<StructurePlacement> candidates = new ArrayList<>();

        for (StructureDefinition def : definitions) {
            int cellSize = def.getCellSize();
            // Maximum possible transformed footprint
            int maxDim = Math.max(def.getTemplate().getWidth(), def.getTemplate().getDepth());

            int minCellX = Math.floorDiv(chunkMinX - maxDim, cellSize);
            int maxCellX = Math.floorDiv(chunkMaxX + maxDim, cellSize);
            int minCellZ = Math.floorDiv(chunkMinZ - maxDim, cellSize);
            int maxCellZ = Math.floorDiv(chunkMaxZ + maxDim, cellSize);

            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                    StructurePlacement placement = evaluateCell(def, cellX, cellZ);
                    if (placement != null) {
                        candidates.add(placement);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Deterministic conflict resolution across all candidate placements in the vicinity
        List<StructurePlacement> resolved = resolveConflicts(candidates);

        // Filter only those that actually intersect this chunk
        List<StructurePlacement> intersecting = new ArrayList<>();
        for (StructurePlacement p : resolved) {
            if (p.intersectsChunk(cx, cz)) {
                intersecting.add(p);
            }
        }

        return intersecting;
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
     * Resolves overlapping and conflicting candidate placements deterministically.
     * Orders by priority (descending), then by deterministic cell hash.
     */
    private List<StructurePlacement> resolveConflicts(List<StructurePlacement> candidates) {
        if (candidates.size() <= 1) {
            return candidates;
        }

        // Deterministic sort: higher priority wins; if equal, higher cell hash wins
        candidates.sort((a, b) -> {
            int pA = a.getDefinition().getConstraints().getPriority();
            int pB = b.getDefinition().getConstraints().getPriority();
            if (pA != pB) return Integer.compare(pB, pA); // Descending

            long hashA = hashCell(worldSeed, a.getDefinition().getId(), a.getOriginX(), a.getOriginZ());
            long hashB = hashCell(worldSeed, b.getDefinition().getId(), b.getOriginX(), b.getOriginZ());
            return Long.compare(hashB, hashA);
        });

        List<StructurePlacement> accepted = new ArrayList<>();
        for (StructurePlacement candidate : candidates) {
            boolean hasConflict = false;
            int reqSpacing = candidate.getDefinition().getConstraints().getMinSpacing();

            for (StructurePlacement existing : accepted) {
                int effectiveSpacing = Math.max(reqSpacing, existing.getDefinition().getConstraints().getMinSpacing());
                if (candidate.conflictsWith(existing, effectiveSpacing)) {
                    hasConflict = true;
                    break;
                }
            }

            if (!hasConflict) {
                accepted.add(candidate);
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
