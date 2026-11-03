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
    private static final int ID_GRAVEL = Blocks.GRAVEL.getDefaultState().getStateId();
    private static final int ID_ANDESITE = Blocks.ANDESITE.getDefaultState().getStateId();
    private static final int ID_DIORITE = Blocks.DIORITE.getDefaultState().getStateId();
    private static final int ID_OAK_LOG = Blocks.OAK_LOG.getDefaultState().getStateId();
    private static final int ID_OAK_LEAVES = Blocks.OAK_LEAVES.getDefaultState().getStateId();
    private static final int ID_BIRCH_LOG = Blocks.BIRCH_LOG.getDefaultState().getStateId();
    private static final int ID_BIRCH_LEAVES = Blocks.BIRCH_LEAVES.getDefaultState().getStateId();
    private static final int ID_SPRUCE_LOG = Blocks.SPRUCE_LOG.getDefaultState().getStateId();
    private static final int ID_SPRUCE_LEAVES = Blocks.SPRUCE_LEAVES.getDefaultState().getStateId();
    private static final int ID_DARK_OAK_LOG = Blocks.DARK_OAK_LOG.getDefaultState().getStateId();
    private static final int ID_DARK_OAK_LEAVES = Blocks.DARK_OAK_LEAVES.getDefaultState().getStateId();

    public enum TreeType {
        OAK,
        BIRCH,
        DARK_OAK,
        SPRUCE
    }

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

                        // Natural Mineral Vein Clusters in Deep Mantle (Y <= 128)
                        // In survival, Andesite, Diorite, and Gravel generate in veins embedded within Stone
                        double andesiteNoise = detailNoise.noise2D(wx * 0.07 + 311.0, wz * 0.07 + y * 0.08 + 177.0);
                        double dioriteNoise = baseNoise.noise2D(wx * 0.07 - 723.0, wz * 0.07 + y * 0.08 - 419.0);
                        double gravelNoise = detailNoise.noise2D(wx * 0.06 + 841.0, wz * 0.06 + y * 0.07 + 953.0);
                        double cobbleNoise = detailNoise.noise2D(wx * 0.15, wz * 0.15 + y * 0.1);

                        if (andesiteNoise > 0.72) {
                            chunk.setStateId(lx, y, lz, ID_ANDESITE);
                        } else if (dioriteNoise > 0.72) {
                            chunk.setStateId(lx, y, lz, ID_DIORITE);
                        } else if (gravelNoise > 0.74) {
                            chunk.setStateId(lx, y, lz, ID_GRAVEL);
                        } else if (cobbleNoise > 0.75) {
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
                    // Underwater Basin / Riverbed (Gravel deposits along riverbeds and ocean floor)
                    double riverGravel = detailNoise.noise2D(wx * 0.15 + 531.0, wz * 0.15 + 619.0);
                    if (riverGravel > 0.68) {
                        chunk.setStateId(lx, surfaceY, lz, ID_GRAVEL);
                    } else {
                        chunk.setStateId(lx, surfaceY, lz, underwaterBlockId);
                    }

                    // Water column filling up to Sea Level (Y = 62)
                    for (int wy = surfaceY + 1; wy <= SEA_LEVEL; wy++) {
                        chunk.setStateId(lx, wy, lz, ID_WATER);
                    }
                } else if (surfaceY <= SEA_LEVEL + 2) {
                    // Beach / Shoreline (Natural sand shores with occasional gravel deposits)
                    double shorelineGravelNoise = detailNoise.noise2D(wx * 0.12 + 109.0, wz * 0.12 + 277.0);
                    if (shorelineGravelNoise > 0.65) {
                        chunk.setStateId(lx, surfaceY, lz, ID_GRAVEL);
                    } else {
                        chunk.setStateId(lx, surfaceY, lz, underwaterBlockId);
                    }
                } else if (surfaceY > 105 && biome.getId().equals("mountains")) {
                    // High Craggy Peaks (Stone peaks with exposed andesite and diorite strata)
                    double peakRockNoise = detailNoise.noise2D(wx * 0.1, wz * 0.1);
                    if (peakRockNoise > 0.65) {
                        chunk.setStateId(lx, surfaceY, lz, ID_ANDESITE);
                    } else if (peakRockNoise < -0.65) {
                        chunk.setStateId(lx, surfaceY, lz, ID_DIORITE);
                    } else {
                        chunk.setStateId(lx, surfaceY, lz, deepBlockId);
                    }
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

        // 5. Forest & Biome Tree Generation (Oak, Birch, Dark Oak, Spruce)
        int seedHash = Math.abs((cx * 31 + cz) ^ (int) (seed & 0x7FFFFFFF));
        int sampleWx = cx * 16 + 8;
        int sampleWz = cz * 16 + 8;
        Biome centerBiome = biomeSampler.getBiome(sampleWx, sampleWz);

        if (centerBiome.getTreeDensity() > 0.0 && seedHash % 3 <= 1) {
            int t1x = (seedHash >> 2) % 8 + 4;
            int t1z = (seedHash >> 5) % 8 + 4;
            TreeType type1 = pickTreeType(centerBiome, cx * 16 + t1x, cz * 16 + t1z, seedHash);
            plantTree(chunk, cx, cz, t1x, t1z, type1, seedHash, maxLx, maxLz);

            if (centerBiome.getTreeDensity() >= 0.50 && seedHash % 3 == 0) {
                int t2x = ((seedHash >> 7) % 6) + 2;
                int t2z = ((seedHash >> 9) % 6) + 8;
                TreeType type2 = pickTreeType(centerBiome, cx * 16 + t2x, cz * 16 + t2z, seedHash ^ 0x55555555);
                plantTree(chunk, cx, cz, t2x, t2z, type2, seedHash ^ 0xAAAAAAAA, maxLx, maxLz);
            }
        }

        // 6. Deterministic Structure Block Generation (clipping to requested chunk)
        structureManager.generateStructureBlocks(chunk, cx, cz);

        return chunk;
    }

    public TreeType pickTreeType(Biome biome, int wx, int wz, int seedHash) {
        if ("mountains".equals(biome.getId())) {
            return TreeType.SPRUCE; // Coniferous alpine trees in cold high crags
        }
        if ("forest".equals(biome.getId())) {
            double speciesNoise = detailNoise.noise2D(wx * 0.03 + 127.0, wz * 0.03 + 311.0);
            if (speciesNoise > 0.35) {
                return TreeType.SPRUCE; // Tall boreal spruce stands
            } else if (speciesNoise < -0.30) {
                return TreeType.DARK_OAK; // Dense dark oak canopy
            } else if ((seedHash & 1) == 0) {
                return TreeType.BIRCH;
            } else {
                return TreeType.OAK;
            }
        }
        // Plains or other biomes:
        int pick = Math.abs(seedHash >> 3) % 10;
        if (pick < 5) return TreeType.OAK;
        if (pick < 7) return TreeType.BIRCH;
        if (pick < 9) return TreeType.SPRUCE;
        return TreeType.DARK_OAK;
    }

    public void plantTree(Chunk chunk, int cx, int cz, int tx, int tz, TreeType type, int seedHash, int maxLx, int maxLz) {
        if (tx < 0 || tx >= maxLx || tz < 0 || tz >= maxLz) {
            return;
        }

        int wx = cx * 16 + tx;
        int wz = cz * 16 + tz;
        int ty = computeHeight(wx, wz);

        if (ty < SEA_LEVEL + 2 || ty > 115) {
            return;
        }

        // Soil requirement: grass or dirt
        BlockState groundState = chunk.getBlockState(tx, ty, tz);
        if (!groundState.is(Blocks.GRASS) && !groundState.is(Blocks.DIRT)) {
            return;
        }

        int logId;
        int leafId;
        int trunkHeight;

        if (type == TreeType.DARK_OAK) {
            // Dark Oak: strictly generates as a thick 2x2 trunk structure with sprawling 8x8 umbrella canopy
            if (tx + 1 >= maxLx) tx = maxLx - 2;
            if (tz + 1 >= maxLz) tz = maxLz - 2;
            if (tx < 0 || tz < 0) return;

            logId = ID_DARK_OAK_LOG;
            leafId = ID_DARK_OAK_LEAVES;
            trunkHeight = 6 + (Math.abs(seedHash) % 3); // 6, 7, or 8 blocks trunk

            // Convert soil under 2x2 trunk footprint to dirt
            for (int ox = 0; ox <= 1; ox++) {
                for (int oz = 0; oz <= 1; oz++) {
                    chunk.setStateId(tx + ox, ty, tz + oz, Blocks.DIRT.getDefaultState().getStateId());
                }
            }

            // 1. Thick 2x2 Wood Trunk (4 columns)
            for (int ox = 0; ox <= 1; ox++) {
                for (int oz = 0; oz <= 1; oz++) {
                    for (int y = 1; y <= trunkHeight; y++) {
                        if (ty + y < Chunk.HEIGHT) {
                            chunk.setStateId(tx + ox, ty + y, tz + oz, logId);
                        }
                    }
                }
            }

            int topY = ty + trunkHeight;

            // 2. Thick irregular side branch logs extending outward beneath/inside the canopy
            int[][] branches = {
                {tx - 1, topY - 2, tz},
                {tx + 2, topY - 2, tz + 1},
                {tx, topY - 1, tz - 1},
                {tx + 1, topY - 1, tz + 2}
            };
            for (int[] b : branches) {
                int bx = b[0], by = b[1], bz = b[2];
                if (bx >= 0 && bx < maxLx && bz >= 0 && bz < maxLz && by < Chunk.HEIGHT) {
                    chunk.setStateId(bx, by, bz, logId);
                }
            }

            // 3. Sprawling 8x8 to 9x9 multi-layered dense umbrella canopy
            // Layer 1 (bottom skirt): 6x6 disc
            setCanopyDisc2x2(chunk, tx, tz, topY - 2, 2, true, leafId, maxLx, maxLz);
            // Layer 2 (main umbrella spread): 8x8 dense disc
            setCanopyDisc2x2(chunk, tx, tz, topY - 1, 3, true, leafId, maxLx, maxLz);
            // Layer 3 (upper canopy roof): 8x8 dense disc
            setCanopyDisc2x2(chunk, tx, tz, topY, 3, true, leafId, maxLx, maxLz);
            // Layer 4 (roof crown): 6x6 disc
            setCanopyDisc2x2(chunk, tx, tz, topY + 1, 2, true, leafId, maxLx, maxLz);
            // Layer 5 (center crest): 4x4 disc
            setCanopyDisc2x2(chunk, tx, tz, topY + 2, 1, false, leafId, maxLx, maxLz);
            return;
        }

        switch (type) {
            case SPRUCE -> {
                // Spruce: the tallest tree (11 to 14 blocks high!), conifer pagoda/spire structure
                logId = ID_SPRUCE_LOG;
                leafId = ID_SPRUCE_LEAVES;
                trunkHeight = 11 + (Math.abs(seedHash) % 4); // 11, 12, 13, or 14 blocks tall!
            }
            case BIRCH -> {
                // Birch: 5 to 6 blocks tall slender tree
                logId = ID_BIRCH_LOG;
                leafId = ID_BIRCH_LEAVES;
                trunkHeight = 5 + (Math.abs(seedHash) % 2); // 5 or 6 blocks tall
            }
            default -> {
                // Oak: 4 to 5 blocks tall classic rounded tree
                logId = ID_OAK_LOG;
                leafId = ID_OAK_LEAVES;
                trunkHeight = 4 + (Math.abs(seedHash) % 2); // 4 or 5 blocks tall
            }
        }

        // Convert soil under trunk to dirt
        chunk.setStateId(tx, ty, tz, Blocks.DIRT.getDefaultState().getStateId());

        // 1. Wood Trunk
        for (int y = 1; y <= trunkHeight; y++) {
            if (ty + y < Chunk.HEIGHT) {
                chunk.setStateId(tx, ty + y, tz, logId);
            }
        }

        // 2. Canopy Foliage by Tree Type
        if (type == TreeType.SPRUCE) {
            // Coniferous spire: bare lower trunk (first 3-4 blocks), then alternating conical tiers tapering to top
            int topY = ty + trunkHeight;

            // Tier 1 (Lowest wide tier, 5x5 clipped corners)
            setCanopyDisc(chunk, tx, tz, topY - 7, 2, true, leafId, maxLx, maxLz);
            // Tier 2 (3x3 cross)
            setCanopyCross(chunk, tx, tz, topY - 5, 1, leafId, maxLx, maxLz);
            // Tier 3 (5x5 clipped corners)
            setCanopyDisc(chunk, tx, tz, topY - 3, 2, true, leafId, maxLx, maxLz);
            // Tier 4 (3x3 cross)
            setCanopyCross(chunk, tx, tz, topY - 1, 1, leafId, maxLx, maxLz);
            // Tier 5 (3x3 square at top of trunk)
            setCanopyDisc(chunk, tx, tz, topY, 1, false, leafId, maxLx, maxLz);
            // Spire Peak (Pointy needle top)
            if (topY + 1 < Chunk.HEIGHT) chunk.setStateId(tx, topY + 1, tz, leafId);
            if (topY + 2 < Chunk.HEIGHT) chunk.setStateId(tx, topY + 2, tz, leafId);

        } else if (type == TreeType.BIRCH) {
            // Slender column canopy
            int leafBase = ty + trunkHeight - 2;
            setCanopyCross(chunk, tx, tz, leafBase, 2, leafId, maxLx, maxLz);
            setCanopyDisc(chunk, tx, tz, leafBase + 1, 1, false, leafId, maxLx, maxLz);
            setCanopyDisc(chunk, tx, tz, leafBase + 2, 1, false, leafId, maxLx, maxLz);
            setCanopyCross(chunk, tx, tz, leafBase + 3, 1, leafId, maxLx, maxLz);
            if (leafBase + 4 < Chunk.HEIGHT) chunk.setStateId(tx, leafBase + 4, tz, leafId);

        } else {
            // Classic Oak rounded balloon canopy
            int leafBase = ty + trunkHeight - 1;
            setCanopyDisc(chunk, tx, tz, leafBase, 2, true, leafId, maxLx, maxLz);
            setCanopyDisc(chunk, tx, tz, leafBase + 1, 2, true, leafId, maxLx, maxLz);
            setCanopyDisc(chunk, tx, tz, leafBase + 2, 1, false, leafId, maxLx, maxLz);
            setCanopyCross(chunk, tx, tz, leafBase + 3, 1, leafId, maxLx, maxLz);
        }
    }

    private void setCanopyDisc(Chunk chunk, int cx, int cz, int y, int radius, boolean clipCorners, int leafId, int maxLx, int maxLz) {
        if (y < 0 || y >= Chunk.HEIGHT) return;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (clipCorners && Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
                int lx = cx + dx;
                int lz = cz + dz;
                if (lx >= 0 && lx < maxLx && lz >= 0 && lz < maxLz) {
                    if (chunk.getStateId(lx, y, lz) == ID_AIR) {
                        chunk.setStateId(lx, y, lz, leafId);
                    }
                }
            }
        }
    }

    private void setCanopyCross(Chunk chunk, int cx, int cz, int y, int radius, int leafId, int maxLx, int maxLz) {
        if (y < 0 || y >= Chunk.HEIGHT) return;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius) continue;
                int lx = cx + dx;
                int lz = cz + dz;
                if (lx >= 0 && lx < maxLx && lz >= 0 && lz < maxLz) {
                    if (chunk.getStateId(lx, y, lz) == ID_AIR) {
                        chunk.setStateId(lx, y, lz, leafId);
                    }
                }
            }
        }
    }
    private void setCanopyDisc2x2(Chunk chunk, int tx, int tz, int y, int radius, boolean clipCorners, int leafId, int maxLx, int maxLz) {
        if (y < 0 || y >= Chunk.HEIGHT) return;
        for (int dx = -radius; dx <= 1 + radius; dx++) {
            for (int dz = -radius; dz <= 1 + radius; dz++) {
                int distX = (dx < 0) ? -dx : (dx > 1 ? dx - 1 : 0);
                int distZ = (dz < 0) ? -dz : (dz > 1 ? dz - 1 : 0);
                if (clipCorners && distX == radius && distZ == radius) continue;
                int lx = tx + dx;
                int lz = tz + dz;
                if (lx >= 0 && lx < maxLx && lz >= 0 && lz < maxLz) {
                    if (chunk.getStateId(lx, y, lz) == ID_AIR) {
                        chunk.setStateId(lx, y, lz, leafId);
                    }
                }
            }
        }
    }

}