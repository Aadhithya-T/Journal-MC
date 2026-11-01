package com.mcjournal;

import java.util.*;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.BlockState;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.biome.Biome;
import com.mcjournal.world.biome.BiomeRegistry;
import com.mcjournal.world.biome.BiomeSampler;
import com.mcjournal.world.biome.ClimateSampler;
import com.mcjournal.world.structure.StructurePlacementManager;
import com.mcjournal.world.structure.StructureRegistry;
import com.mcjournal.world.terrain.TerrainModifierPipeline;

/**
 * Scalable, deterministic procedural terrain generator for the finite 2500x2500x256 world.
 * Incorporates:
 * - Deterministic climate sampling (temperature, moisture, continentalness)
 * - Extensible Biome architecture (<= 10 biomes) with continuous height blending
 * - Natural world boundary terrain shaping (WorldBounds.EdgeZone)
 * - Deterministic modifier pipeline for hybrid procedural/authored terrain
 * - 3D cave carving, mineral ore veins, and foliage placement
 * - Strict partial edge chunk isolation
 */
public class TerrainGenerator {
    public static final int SEA_LEVEL = 62; // Minecraft 1.17 Standard Sea Level
    public static final int BASE_GROUND_LEVEL = 64; // Minecraft 1.17 Plains Ground Level

    private final long seed;
    private final WorldBounds bounds;
    private final BiomeRegistry biomeRegistry;
    private final ClimateSampler climateSampler;
    private final BiomeSampler biomeSampler;
    private final TerrainModifierPipeline modifierPipeline;
    private final StructurePlacementManager structureManager;

    // Multi-octave terrain and feature noise generators
    private final SimplexNoise baseNoise;
    private final SimplexNoise detailNoise;
    private final SimplexNoise mountainNoise;
    private final SimplexNoise boundaryNoise;
    private final SimplexNoise caveNoise1;
    private final SimplexNoise caveNoise2;
    private final List<POI> pois = new ArrayList<>();

    private static final int ID_AIR = Blocks.AIR.getDefaultState().getStateId();
    private static final int ID_BEDROCK = Blocks.BEDROCK.getDefaultState().getStateId();
    private static final int ID_STONE = Blocks.STONE.getDefaultState().getStateId();
    private static final int ID_DIRT = Blocks.DIRT.getDefaultState().getStateId();
    private static final int ID_GRASS = Blocks.GRASS.getDefaultState().getStateId();
    private static final int ID_SAND = Blocks.SAND.getDefaultState().getStateId();
    private static final int ID_WATER = Blocks.WATER.getDefaultState().getStateId();
    private static final int ID_DIAMOND_ORE = Blocks.DIAMOND_ORE.getDefaultState().getStateId();
    private static final int ID_COBBLESTONE = Blocks.COBBLESTONE.getDefaultState().getStateId();
    private static final int ID_POPPY = Blocks.POPPY.getDefaultState().getStateId();
    private static final int ID_DANDELION = Blocks.DANDELION.getDefaultState().getStateId();
    private static final int ID_TALL_GRASS = Blocks.TALL_GRASS.getDefaultState().getStateId();

    public record POI(String id, String name, String subtitle, String icon, int x, int z, String excerpt) {}

    public TerrainGenerator(long seed) {
        this(seed, WorldBounds.DEFAULT, BiomeRegistry.createDefaultRegistry(), TerrainModifierPipeline.empty(), new StructureRegistry());
    }

    public TerrainGenerator(long seed, WorldBounds bounds) {
        this(seed, bounds, BiomeRegistry.createDefaultRegistry(), TerrainModifierPipeline.empty(), new StructureRegistry());
    }

    public TerrainGenerator(long seed, WorldBounds bounds, BiomeRegistry biomeRegistry, TerrainModifierPipeline pipeline) {
        this(seed, bounds, biomeRegistry, pipeline, new StructureRegistry());
    }

    public TerrainGenerator(long seed, WorldBounds bounds, BiomeRegistry biomeRegistry,
                            TerrainModifierPipeline pipeline, StructureRegistry structureRegistry) {
        this.seed = seed;
        this.bounds = (bounds != null) ? bounds : WorldBounds.DEFAULT;
        this.biomeRegistry = (biomeRegistry != null) ? biomeRegistry : BiomeRegistry.createDefaultRegistry();
        this.climateSampler = new ClimateSampler(seed);
        this.biomeSampler = new BiomeSampler(this.biomeRegistry, this.climateSampler);
        this.modifierPipeline = (pipeline != null) ? pipeline : TerrainModifierPipeline.empty();
        StructureRegistry structReg = (structureRegistry != null) ? structureRegistry : new StructureRegistry();
        this.structureManager = new StructurePlacementManager(seed, this.bounds, this, structReg);

        this.baseNoise = new SimplexNoise(seed);
        this.detailNoise = new SimplexNoise(seed + 1337L);
        this.mountainNoise = new SimplexNoise(seed + 9999L);
        this.boundaryNoise = new SimplexNoise(seed + 5555L);
        this.caveNoise1 = new SimplexNoise(seed + 4242L);
        this.caveNoise2 = new SimplexNoise(seed + 8484L);

        initPOIs();
    }

    public long getSeed() {
        return seed;
    }

    public WorldBounds getWorldBounds() {
        return bounds;
    }

    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    public ClimateSampler getClimateSampler() {
        return climateSampler;
    }

    public BiomeSampler getBiomeSampler() {
        return biomeSampler;
    }

    public TerrainModifierPipeline getModifierPipeline() {
        return modifierPipeline;
    }

    public StructurePlacementManager getStructureManager() {
        return structureManager;
    }

    private void initPOIs() {
        pois.add(new POI("spawn_lectern", "Adventurer's Lectern", "River Clearing", "📜", 6, -10, "A hand-carved oak pedestal holding an open journal."));
        pois.add(new POI("crystal_lake", "Crystal Lake Study", "Mirror Water's Edge", "🌊", 14, 28, "A tranquil research post set up beside clear spring waters."));
        pois.add(new POI("shrine", "Forgotten Stone Shrine", "Eastern Overlook", "🏛️", 22, 16, "An ancient stone altar flanked by moss-weathered pillars."));
        pois.add(new POI("wanderer_cache", "Wanderer's Cache", "Dense Birch Border", "📦", -18, -24, "A sturdy chest resting beside a cluster of oak logs."));
        pois.add(new POI("mineshaft_entrance", "Abandoned Mine Entrance", "Deep Ravine Edge", "⛏️", 32, -28, "Weathered oak beam scaffolding leading into a dark mineral-rich tunnel."));
        pois.add(new POI("deep_ridge", "Deep Ridge Mineral Pocket", "Southern Granite Crags", "💎", -26, 32, "Exposed veins of blue diamond ore glittering under the mountain sun."));
    }

    public List<POI> getPois() {
        return Collections.unmodifiableList(pois);
    }

    /**
     * Computes deterministic, continuous terrain height at world block coordinate (wx, wz).
     * Integrates:
     * 1. Multi-biome parameter blending (base height, variation, roughness)
     * 2. Continental and mountain noise octaves
     * 3. Natural river carving channels
     * 4. Natural world boundary elevation shaping (EdgeZone)
     * 5. Authored terrain modifiers pipeline
     */
    public int computeHeight(int wx, int wz) {
        // 1. Sample blended biome characteristics
        BiomeSampler.BlendedTerrainParams params = biomeSampler.sampleBlendedParams(wx, wz);
        double baseHeight = params.baseHeight();
        double variation = params.heightVariation();
        double roughness = params.roughness();

        // 2. Continental & Mountain Octaves
        double continental = baseNoise.noise2D(wx * 0.012, wz * 0.012) * variation;
        double detail = detailNoise.noise2D(wx * 0.035, wz * 0.035) * (variation * 0.40) * roughness;

        double ridge = Math.max(0, mountainNoise.noise2D(wx * 0.008, wz * 0.008));
        double mountain = Math.pow(ridge, 2.2) * (variation * 1.5) * roughness;

        double height = baseHeight + continental + detail + mountain;

        // 3. River Channel Carving
        double riverCurve = Math.sin(wz * 0.025) * 22.0 + Math.cos(wz * 0.012) * 8.0;
        double distToRiver = Math.abs(wx - riverCurve);

        // Damp river depth near world boundaries to prevent boundary breaches
        double riverDamp = 1.0;
        if (bounds.isBounded()) {
            int edgeDist = bounds.distanceToEdge(wx, wz);
            if (edgeDist < 64) {
                riverDamp = Math.clamp(edgeDist / 64.0, 0.0, 1.0);
            }
        }

        if (distToRiver < 7.0) {
            double depth = (1.0 - (distToRiver / 7.0)) * riverDamp;
            height -= depth * 16.0;
        } else if (distToRiver < 11.0) {
            double slope = (1.0 - ((distToRiver - 7.0) / 4.0)) * riverDamp;
            height -= slope * 6.0;
        }

        // 4. Natural World Boundary Shaping (WorldBounds.EdgeZone)
        if (bounds.isBounded()) {
            int edgeDist = bounds.distanceToEdge(wx, wz);
            if (edgeDist <= 200) {
                // Smooth transition towards boundary: 0.0 at edgeDist=200, 1.0 at edgeDist=0
                double t = (200.0 - edgeDist) / 200.0;
                double smoothT = t * t * (3.0 - 2.0 * t); // Hermite smoothstep

                // Natural variation along perimeter edges (mountains, cliffs, crags)
                double boundaryFeatureNoise = boundaryNoise.noise2D(wx * 0.007, wz * 0.007);
                double edgeRise = smoothT * (32.0 + Math.max(0.0, boundaryFeatureNoise) * 38.0);
                height += edgeRise;
            }
        }

        // 5. Authored Terrain Modifier Pipeline
        height = modifierPipeline.applyHeightModifiers(wx, wz, height);

        // Final safety clamp: vertical limits [40, 245]
        return Math.clamp((int) Math.round(height), 40, 245);
    }

    /**
     * Generates all voxels for chunk (cx, cz).
     * Enforces finite bounds and strictly maintains AIR in invalid partial chunk columns.
     */
    public Chunk generateChunk(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        if (!bounds.isValidChunk(cx, cz)) {
            return chunk; // Out-of-world chunk remains empty air
        }

        int maxLx = bounds.getValidLocalBlockCountX(cx);
        int maxLz = bounds.getValidLocalBlockCountZ(cz);
        if (maxLx <= 0 || maxLz <= 0) {
            return chunk;
        }

        // Column-level generation
        for (int lz = 0; lz < maxLz; lz++) {
            for (int lx = 0; lx < maxLx; lx++) {
                int wx = cx * 16 + lx;
                int wz = cz * 16 + lz;

                // 1. Column climate, biome, and surface height
                Biome biome = biomeSampler.getBiome(wx, wz);
                biome = modifierPipeline.applyBiomeModifiers(wx, wz, biome);

                int surfaceY = computeHeight(wx, wz);

                // Palette selection
                int surfaceBlockId = biome.getSurfaceBlock().getStateId();
                int subsurfaceBlockId = biome.getSubsurfaceBlock().getStateId();
                int deepBlockId = biome.getDeepBlock().getStateId();
                int underwaterBlockId = biome.getUnderwaterBlock().getStateId();

                // 2. Bedrock Floor (Y = 0 always bedrock; bumps Y = 1..3 deterministic from column hash)
                chunk.setStateId(lx, 0, lz, ID_BEDROCK);
                long colHash = ((long) wx * 341873128712L + (long) wz * 132897987541L) ^ seed;
                for (int by = 1; by <= 3; by++) {
                    float randFloat = (float) (((colHash ^ (by * 0x9E3779B9L)) & 0xFFFF) / 65536.0);
                    if (randFloat < (1.0f - by * 0.28f)) {
                        chunk.setStateId(lx, by, lz, ID_BEDROCK);
                    }
                }

                // 3. Underground Mantle (Caves, Ores, Stone, Subsurface)
                for (int y = 1; y < surfaceY; y++) {
                    if (chunk.getStateId(lx, y, lz) == ID_BEDROCK) continue;

                    if (y < surfaceY - 4) {
                        // 3D Cave & Cavern Carving
                        if (y >= 5) {
                            double c1 = caveNoise1.noise3D(wx * 0.042, y * 0.062, wz * 0.042);
                            double c2 = caveNoise2.noise3D(wx * 0.042, y * 0.062, wz * 0.042);

                            double tunnelMetric = c1 * c1 + c2 * c2;
                            boolean isWormCave = (tunnelMetric < 0.016);
                            boolean isCavern = (c1 > 0.60 && Math.abs(c2) < 0.32);

                            if (isWormCave || isCavern) {
                                if (y <= 11 && y >= 9 && !isWormCave) {
                                    chunk.setStateId(lx, y, lz, ID_WATER);
                                } else {
                                    chunk.setStateId(lx, y, lz, ID_AIR);
                                }
                                continue;
                            }
                        }

                        // Diamond Ore (peak density near bedrock, Y <= 16)
                        if (y <= 16) {
                            double diamondNoise = detailNoise.noise2D(wx * 0.45, wz * 0.45 + y * 0.3);
                            if (diamondNoise > 0.72) {
                                chunk.setStateId(lx, y, lz, ID_DIAMOND_ORE);
                                continue;
                            }
                        }

                        // Cobblestone vein clusters in deep mantle
                        double cobbleNoise = detailNoise.noise2D(wx * 0.15, wz * 0.15 + y * 0.1);
                        if (cobbleNoise > 0.75) {
                            chunk.setStateId(lx, y, lz, ID_COBBLESTONE);
                        } else {
                            chunk.setStateId(lx, y, lz, deepBlockId);
                        }
                    } else {
                        // Subsurface Layer (3-4 blocks below surface)
                        if (surfaceY <= SEA_LEVEL + 1) {
                            chunk.setStateId(lx, y, lz, underwaterBlockId);
                        } else {
                            chunk.setStateId(lx, y, lz, subsurfaceBlockId);
                        }
                    }
                }

                // 4. Surface & Shoreline Layering
                if (surfaceY <= SEA_LEVEL) {
                    // Underwater Basin / Riverbed
                    chunk.setStateId(lx, surfaceY, lz, underwaterBlockId);

                    // Water column filling up to Sea Level (Y = 62)
                    for (int wy = surfaceY + 1; wy <= SEA_LEVEL; wy++) {
                        chunk.setStateId(lx, wy, lz, ID_WATER);
                    }
                } else if (surfaceY <= SEA_LEVEL + 2) {
                    // Beach / Shoreline
                    chunk.setStateId(lx, surfaceY, lz, underwaterBlockId);
                } else if (surfaceY > 105 && biome.getId().equals("mountains")) {
                    // High Craggy Peaks
                    chunk.setStateId(lx, surfaceY, lz, deepBlockId);
                } else {
                    // Biome Surface Block
                    chunk.setStateId(lx, surfaceY, lz, surfaceBlockId);

                    // Foliage & Flower Scatter (plains, forest)
                    if (biome.getFoliageDensity() > 0.0 && surfaceY + 1 < Chunk.HEIGHT) {
                        double vegNoise = detailNoise.noise2D(wx * 0.28, wz * 0.28);
                        if (vegNoise > 0.83) {
                            chunk.setStateId(lx, surfaceY + 1, lz, ID_POPPY);
                        } else if (vegNoise > 0.76) {
                            chunk.setStateId(lx, surfaceY + 1, lz, ID_DANDELION);
                        } else if (vegNoise > 0.52) {
                            chunk.setStateId(lx, surfaceY + 1, lz, ID_TALL_GRASS);
                        }
                    }
                }
            }
        }

        // 5. Forest Tree Generation
        int seedHash = Math.abs((cx * 31 + cz) ^ (int) (seed & 0x7FFFFFFF));
        int sampleWx = cx * 16 + 8;
        int sampleWz = cz * 16 + 8;
        Biome centerBiome = biomeSampler.getBiome(sampleWx, sampleWz);

        if (centerBiome.getTreeDensity() > 0.0 && seedHash % 3 <= 1) {
            int t1x = (seedHash >> 2) % 8 + 4;
            int t1z = (seedHash >> 5) % 8 + 4;
            plantTree(chunk, cx, cz, t1x, t1z, seedHash % 2 == 0, maxLx, maxLz);

            if (centerBiome.getTreeDensity() >= 0.50 && seedHash % 3 == 0) {
                int t2x = ((seedHash >> 7) % 6) + 2;
                int t2z = ((seedHash >> 9) % 6) + 8;
                plantTree(chunk, cx, cz, t2x, t2z, seedHash % 4 == 0, maxLx, maxLz);
            }
        }

        // 6. Deterministic Structure Block Generation (clipping to requested chunk)
        structureManager.generateStructureBlocks(chunk, cx, cz);

        return chunk;
    }

    private void plantTree(Chunk chunk, int cx, int cz, int tx, int tz, boolean isBirch, int maxLx, int maxLz) {
        if (tx < 0 || tx >= maxLx || tz < 0 || tz >= maxLz) {
            return;
        }

        int wx = cx * 16 + tx;
        int wz = cz * 16 + tz;
        int ty = computeHeight(wx, wz);

        if (ty >= SEA_LEVEL + 2 && ty <= 95 && chunk.getBlockState(tx, ty, tz).is(Blocks.GRASS)) {
            BlockState logType = isBirch ? Blocks.BIRCH_LOG.getDefaultState() : Blocks.OAK_LOG.getDefaultState();
            BlockState leafType = isBirch ? Blocks.BIRCH_LEAVES.getDefaultState() : Blocks.OAK_LEAVES.getDefaultState();
            int trunkHeight = isBirch ? 5 : 4;

            chunk.setBlockState(tx, ty + 1, tz, Blocks.AIR.getDefaultState());

            // Wood Trunk
            for (int y = 1; y <= trunkHeight; y++) {
                if (ty + y < Chunk.HEIGHT) {
                    chunk.setBlockState(tx, ty + y, tz, logType);
                }
            }

            // Canopy Leaves strictly within valid local bounds
            int leafBase = ty + trunkHeight - 1;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    int lx = tx + dx;
                    int lz = tz + dz;
                    if (lx >= 0 && lx < maxLx && lz >= 0 && lz < maxLz) {
                        if (leafBase < Chunk.HEIGHT) chunk.setBlockState(lx, leafBase, lz, leafType);
                        if (leafBase + 1 < Chunk.HEIGHT) chunk.setBlockState(lx, leafBase + 1, lz, leafType);
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = tx + dx;
                    int lz = tz + dz;
                    if (lx >= 0 && lx < maxLx && lz >= 0 && lz < maxLz) {
                        if (leafBase + 2 < Chunk.HEIGHT) chunk.setBlockState(lx, leafBase + 2, lz, leafType);
                    }
                }
            }
            if (leafBase + 3 < Chunk.HEIGHT) {
                chunk.setBlockState(tx, leafBase + 3, tz, leafType);
                if (tx + 1 < maxLx) chunk.setBlockState(tx + 1, leafBase + 3, tz, leafType);
                if (tx - 1 >= 0) chunk.setBlockState(tx - 1, leafBase + 3, tz, leafType);
                if (tz + 1 < maxLz) chunk.setBlockState(tx, leafBase + 3, tz + 1, leafType);
                if (tz - 1 >= 0) chunk.setBlockState(tx, leafBase + 3, tz - 1, leafType);
            }
        }
    }
}
