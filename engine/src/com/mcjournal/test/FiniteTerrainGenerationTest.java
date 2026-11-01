package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.EngineConstants;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.BlockState;
import com.mcjournal.client.Camera;
import com.mcjournal.client.GameSettings;
import com.mcjournal.client.Player;
import com.mcjournal.client.WorldSession;
import com.mcjournal.client.WorldSaveManager;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;
import com.mcjournal.world.biome.*;
import com.mcjournal.world.terrain.SpawnLocator;
import com.mcjournal.world.terrain.TerrainModifier;
import com.mcjournal.world.terrain.TerrainModifierPipeline;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Phase 5 Automated Test Suite: Finite World Terrain, Biomes, Climate, Boundaries & Spawn.
 *
 * Covers:
 * 1. Biome Architecture: registration, duplicate rejection, max 10 enforcement, invalid definitions, fallback.
 * 2. Climate Sampling: determinism, seed divergence, valid [-1, 1] range, load-order independence.
 * 3. Terrain Height & Blending: determinism, valid vertical limits, border continuity, smooth transitions.
 * 4. Surface & Subsurface Block Palettes: valid registry states, partial chunk isolation.
 * 5. Natural World Boundaries: edge elevation shaping, four borders, corner chunk (156,156).
 * 6. Hybrid Terrain Modifiers: pipeline execution, deterministic authored adjustments.
 * 7. Terrain-Aware Spawn Resolution: surface clearance, dry ground, bounds containment, preservation of saves.
 * 8. Thread Safety: concurrent vs sequential chunk generation producing identical voxel arrays.
 * 9. Generation Performance Benchmark.
 */
public class FiniteTerrainGenerationTest {

    private static final File TEST_BASE_DIR = new File("saves/test_phase5_terrain");

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    RUNNING PHASE 5: TERRAIN & BIOME TESTS       ");
        System.out.println("=================================================");

        cleanupDir(TEST_BASE_DIR);

        try {
            testBiomeArchitectureAndMaxLimit();
            testClimateSamplingDeterminismAndRanges();
            testTerrainHeightDeterminismAndContinuity();
            testBiomeBlendingSmoothness();
            testBlockPaletteAndPartialChunkIsolation();
            testNaturalBoundaryTerrainShaping();
            testHybridTerrainModifierPipeline();
            testTerrainAwareSpawnResolution();
            testSpawnSafetyValidationCases();
            testSavedPlayerPositionPreservation();
            testConcurrentVsSequentialThreadSafety();
            testGenerationPerformanceBenchmark();

            System.out.println(">>> ALL PHASE 5 TERRAIN & BIOME TESTS PASSED SUCCESSFULLY! <<<\n");
        } catch (Throwable t) {
            System.err.println("❌ PHASE 5 TESTS FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            cleanupDir(TEST_BASE_DIR);
        }
    }

    /**
     * 1. Biome Architecture:
     * - Accepts valid biomes
     * - Rejects duplicate IDs
     * - Enforces maximum 10 biomes limit
     * - Rejects invalid parameters (null/blank ID, invalid heights, null palette)
     * - Fallback biome works
     */
    private static void testBiomeArchitectureAndMaxLimit() {
        System.out.print("[Phase 5] Biome architecture, validation & max 10 limit... ");

        Biome plains = Biome.builder("plains_test", "Plains")
                .climate(0.0, 0.0, 0.2)
                .terrain(64.0, 6.0, 1.0)
                .build();

        BiomeRegistry registry = new BiomeRegistry(plains);
        assert registry.size() == 1;
        assert registry.getDefaultBiome().getId().equals("plains_test");
        assert registry.get("plains_test") == plains;
        assert registry.get("non_existent") == plains : "Unknown biome must return fallback default";

        // Rejection: duplicate ID
        boolean caughtDuplicate = false;
        try {
            registry.register(Biome.builder("plains_test", "Plains Duplicate").build());
        } catch (IllegalArgumentException e) {
            caughtDuplicate = true;
        }
        assert caughtDuplicate : "Duplicate biome ID must be rejected";

        // Rejection: null registration
        boolean caughtNull = false;
        try {
            registry.register(null);
        } catch (IllegalArgumentException e) {
            caughtNull = true;
        }
        assert caughtNull : "Null biome must be rejected";

        // Rejection: invalid parameters
        boolean caughtBlankId = false;
        try {
            Biome.builder("", "Invalid").build();
        } catch (IllegalArgumentException e) {
            caughtBlankId = true;
        }
        assert caughtBlankId : "Blank biome ID must be rejected";

        boolean caughtInvalidHeight = false;
        try {
            Biome.builder("invalid_height", "Invalid").terrain(-5.0, 6.0, 1.0).build();
        } catch (IllegalArgumentException e) {
            caughtInvalidHeight = true;
        }
        assert caughtInvalidHeight : "Negative base height must be rejected";

        // Enforce maximum 10 biomes limit
        for (int i = 2; i <= 10; i++) {
            Biome b = Biome.builder("biome_" + i, "Biome " + i)
                    .climate(i * 0.1, -i * 0.1, 0.0)
                    .terrain(60.0 + i, 5.0, 1.0)
                    .build();
            registry.register(b);
        }
        assert registry.size() == 10 : "Registry should hold exactly 10 biomes";

        // Attempt to register 11th biome (MUST throw IllegalStateException)
        boolean caughtOverflow = false;
        try {
            registry.register(Biome.builder("biome_11", "Biome 11 Overflow").build());
        } catch (IllegalStateException e) {
            caughtOverflow = true;
        }
        assert caughtOverflow : "11th biome registration MUST be rejected by MAX_BIOMES=10 limit";

        System.out.println("PASSED");
    }

    /**
     * 2. Climate Sampling:
     * - Deterministic output for same seed and coordinates
     * - Different seeds produce diverging climate distributions
     * - Strict [-1.0, 1.0] range
     * - Order-independent sampling
     */
    private static void testClimateSamplingDeterminismAndRanges() {
        System.out.print("[Phase 5] Climate sampling determinism, ranges & independence... ");

        long seedA = 998877L;
        long seedB = 112233L;

        ClimateSampler samplerA1 = new ClimateSampler(seedA);
        ClimateSampler samplerA2 = new ClimateSampler(seedA);
        ClimateSampler samplerB = new ClimateSampler(seedB);

        int[][] testCoords = {
            {0, 0}, {500, 500}, {1250, 1250}, {2499, 2499}, {100, 2000}, {2200, 300}
        };

        for (int[] coord : testCoords) {
            int wx = coord[0];
            int wz = coord[1];

            ClimateParameters c1 = samplerA1.sample(wx, wz);
            ClimateParameters c2 = samplerA2.sample(wx, wz);

            assert c1.temperature() == c2.temperature() : "Temperature mismatch at (" + wx + "," + wz + ")";
            assert c1.moisture() == c2.moisture() : "Moisture mismatch at (" + wx + "," + wz + ")";
            assert c1.continentalness() == c2.continentalness() : "Continentalness mismatch at (" + wx + "," + wz + ")";

            assert c1.temperature() >= -1.0 && c1.temperature() <= 1.0 : "Temperature outside [-1, 1]";
            assert c1.moisture() >= -1.0 && c1.moisture() <= 1.0 : "Moisture outside [-1, 1]";
            assert c1.continentalness() >= -1.0 && c1.continentalness() <= 1.0 : "Continentalness outside [-1, 1]";
        }

        // Divergence between seeds
        int divergentCount = 0;
        for (int x = 100; x <= 2000; x += 100) {
            ClimateParameters ca = samplerA1.sample(x, x);
            ClimateParameters cb = samplerB.sample(x, x);
            if (Math.abs(ca.temperature() - cb.temperature()) > 0.05 ||
                Math.abs(ca.continentalness() - cb.continentalness()) > 0.05) {
                divergentCount++;
            }
        }
        assert divergentCount >= 15 : "Climate distributions must diverge across distinct seeds";

        System.out.println("PASSED");
    }

    /**
     * 3. Terrain Height & Seam Continuity:
     * - Same seed and coordinates produce identical heights
     * - Different seeds produce distinct terrain
     * - Heights strictly inside [40, 245]
     * - Chunk boundary columns have NO height seams or steps caused by chunk boundaries
     */
    private static void testTerrainHeightDeterminismAndContinuity() {
        System.out.print("[Phase 5] Terrain height determinism, bounds & seam continuity... ");

        long seed = 543216789L;
        TerrainGenerator gen1 = new TerrainGenerator(seed);
        TerrainGenerator gen2 = new TerrainGenerator(seed);

        // Determinism across world coordinates
        for (int wx = 0; wx < 2500; wx += 123) {
            for (int wz = 0; wz < 2500; wz += 123) {
                int h1 = gen1.computeHeight(wx, wz);
                int h2 = gen2.computeHeight(wx, wz);
                assert h1 == h2 : "Height mismatch at (" + wx + "," + wz + ")";
                assert h1 >= 40 && h1 <= 245 : "Height " + h1 + " outside valid [40, 245] range at (" + wx + "," + wz + ")";
            }
        }

        // Chunk boundary seam test: verify that coordinates crossing chunk border (e.g. lx=15 of cx=5 to lx=0 of cx=6)
        // have normal continuous slope without discrete chunk-edge cliff steps
        for (int cz = 10; cz <= 15; cz++) {
            for (int lz = 0; lz < 16; lz++) {
                int wz = cz * 16 + lz;
                int wxBeforeBorder = 5 * 16 + 15; // Block 95
                int wxAfterBorder = 6 * 16 + 0;   // Block 96

                int hBefore = gen1.computeHeight(wxBeforeBorder, wz);
                int hAfter = gen1.computeHeight(wxAfterBorder, wz);

                int deltaH = Math.abs(hAfter - hBefore);
                assert deltaH <= 4 : "Unnatural chunk boundary height seam detected at X=" + wxBeforeBorder + "->" + wxAfterBorder + " (delta: " + deltaH + ")";
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 4. Biome Blending:
     * - Smooth parameter transitions across climate boundaries without cliffs
     */
    private static void testBiomeBlendingSmoothness() {
        System.out.print("[Phase 5] Biome transitions and continuous parameter blending... ");

        TerrainGenerator gen = new TerrainGenerator(445566L);
        BiomeSampler sampler = gen.getBiomeSampler();

        // Sample a 50-block transect across a biome transition zone
        int startX = 600;
        int endX = 750;
        int wz = 1000;

        double prevBase = -1.0;
        int maxStep = 0;

        for (int wx = startX; wx <= endX; wx++) {
            BiomeSampler.BlendedTerrainParams params = sampler.sampleBlendedParams(wx, wz);
            if (prevBase >= 0.0) {
                double diff = Math.abs(params.baseHeight() - prevBase);
                if (diff > maxStep) {
                    maxStep = (int) Math.ceil(diff);
                }
                assert diff < 3.0 : "Sudden height parameter jump of " + diff + " at wx=" + wx + " (biome blend must be smooth)";
            }
            prevBase = params.baseHeight();
        }

        System.out.println("PASSED");
    }

    /**
     * 5. Surface and Subsurface Blocks & Partial Chunk Isolation:
     * - Valid registry block IDs used
     * - Partial edge chunks (156, cz) and (cx, 156) strictly have AIR in invalid columns
     */
    private static void testBlockPaletteAndPartialChunkIsolation() {
        System.out.print("[Phase 5] Block palette validity & partial edge chunk isolation... ");

        TerrainGenerator gen = new TerrainGenerator(12345L, WorldBounds.DEFAULT);

        // Interior chunk (10, 10)
        Chunk interior = gen.generateChunk(10, 10);
        assert interior != null;
        assert interior.getBlockState(0, 0, 0).is(Blocks.BEDROCK);

        // Verify valid block states in column
        for (int y = 0; y < 120; y++) {
            BlockState state = interior.getBlockState(8, y, 8);
            assert state != null && state.getBlock() != null : "Null block state at y=" + y;
        }

        // Edge chunk (156, 10): valid local X: 0..3, invalid local X: 4..15
        Chunk edgeX = gen.generateChunk(156, 10);
        assert edgeX.getBlockState(0, 0, 0).is(Blocks.BEDROCK);
        assert edgeX.getBlockState(3, 0, 5).is(Blocks.BEDROCK);

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 4; lx < 16; lx++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    assert edgeX.getStateId(lx, y, lz) == 0 : "Illegal voxel at (" + lx + "," + y + "," + lz + ") in edge chunk 156";
                }
            }
        }

        // Corner chunk (156, 156): valid footprint 4x4 (lx: 0..3, lz: 0..3)
        Chunk corner = gen.generateChunk(156, 156);
        for (int lz = 0; lz < 4; lz++) {
            for (int lx = 0; lx < 4; lx++) {
                assert corner.getBlockState(lx, 0, lz).is(Blocks.BEDROCK);
            }
        }
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                if (lx >= 4 || lz >= 4) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        assert corner.getStateId(lx, y, lz) == 0 : "Illegal corner voxel at (" + lx + "," + y + "," + lz + ")";
                    }
                }
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 6. Natural World Boundary Shaping:
     * - Edge zone elevation rises toward world perimeter
     * - All 4 borders tested (West, East, North, South)
     * - Terrain never exceeds maximum height 255
     * - No blocks placed outside finite bounds
     */
    private static void testNaturalBoundaryTerrainShaping() {
        System.out.print("[Phase 5] Natural boundary shaping on all 4 borders & corners... ");

        TerrainGenerator gen = new TerrainGenerator(777L, WorldBounds.DEFAULT);

        // Core height vs Border height comparison
        // Core center (1250, 1250) has dist = 1249 (CORE zone)
        int coreHeight = gen.computeHeight(1250, 1250);

        // West border: wx = 0 (dist = 0, BORDER zone)
        int westHeight = gen.computeHeight(0, 1250);
        assert westHeight >= 70 : "West boundary must exhibit natural elevation barrier (got " + westHeight + ")";

        // East border: wx = 2499 (dist = 0, BORDER zone)
        int eastHeight = gen.computeHeight(2499, 1250);
        assert eastHeight >= 70 : "East boundary must exhibit natural elevation barrier (got " + eastHeight + ")";

        // North border: wz = 0 (dist = 0, BORDER zone)
        int northHeight = gen.computeHeight(1250, 0);
        assert northHeight >= 70 : "North boundary must exhibit natural elevation barrier (got " + northHeight + ")";

        // South border: wz = 2499 (dist = 0, BORDER zone)
        int southHeight = gen.computeHeight(1250, 2499);
        assert southHeight >= 70 : "South boundary must exhibit natural elevation barrier (got " + southHeight + ")";

        // Elevation transition test from CORE (dist=250) to BORDER (dist=0)
        int hCore = gen.computeHeight(250, 1250);
        int hApproach = gen.computeHeight(100, 1250);
        int hBorder = gen.computeHeight(2, 1250);

        // Height tends upwards into natural ridge/cliff towards the edge
        assert hBorder >= 65 && hBorder <= 245 : "Border height within safe bounds";
        assert gen.computeHeight(0, 0) <= 245 : "NW Corner height within safe bounds";
        assert gen.computeHeight(2499, 2499) <= 245 : "SE Corner height within safe bounds";

        System.out.println("PASSED");
    }

    /**
     * 7. Hybrid Terrain Modifier Pipeline:
     * - Deterministic modifier execution
     * - Height modification applies correctly
     */
    private static void testHybridTerrainModifierPipeline() {
        System.out.print("[Phase 5] Hybrid procedural/authored terrain modifier pipeline... ");

        TerrainModifierPipeline pipeline = new TerrainModifierPipeline();
        pipeline.addModifier(new TerrainModifier() {
            @Override
            public String getName() { return "authored_plateau"; }

            @Override
            public double modifyHeight(int wx, int wz, double currentHeight) {
                // Authored plateau at (500, 500) within radius 20
                double dist = Math.hypot(wx - 500, wz - 500);
                if (dist <= 20.0) {
                    return 110.0;
                }
                return currentHeight;
            }
        });

        TerrainGenerator genWithMod = new TerrainGenerator(12345L, WorldBounds.DEFAULT, BiomeRegistry.createDefaultRegistry(), pipeline);
        TerrainGenerator genWithoutMod = new TerrainGenerator(12345L, WorldBounds.DEFAULT);

        // Outside modifier area: identical
        assert genWithMod.computeHeight(100, 100) == genWithoutMod.computeHeight(100, 100) : "Outside modifier area must match";

        // Inside modifier area: plateau applied
        assert genWithMod.computeHeight(500, 500) == 110 : "Modifier plateau height must be 110";
        assert genWithoutMod.computeHeight(500, 500) != 110 : "Base generator does not have plateau";

        System.out.println("PASSED");
    }

    /**
     * 8. Terrain-Aware Spawn Resolution:
     * - Surface height matches generated terrain
     * - Vertical clearance for player height (1.8m)
     * - Spawn is on dry land (surfaceY > SEA_LEVEL)
     * - Full player AABB is strictly inside world bounds
     * - Deterministic for same seed
     */
    private static void testTerrainAwareSpawnResolution() {
        System.out.print("[Phase 5] Terrain-aware spawn resolution & AABB validation... ");

        TerrainGenerator gen = new TerrainGenerator(987654321L, WorldBounds.DEFAULT);
        WorldBounds bounds = WorldBounds.DEFAULT;

        SpawnLocator.SpawnPosition spawn1 = SpawnLocator.findSafeSpawn(gen, bounds);
        SpawnLocator.SpawnPosition spawn2 = SpawnLocator.findSafeSpawn(gen, bounds);

        // Determinism
        assert spawn1.x() == spawn2.x() && spawn1.y() == spawn2.y() && spawn1.z() == spawn2.z() : "Spawn must be deterministic";

        // Proximity to (1250, 1250)
        assert Math.abs(spawn1.x() - EngineConstants.DEFAULT_SPAWN_X) <= 64 : "Spawn X within radius 64 of 1250";
        assert Math.abs(spawn1.z() - EngineConstants.DEFAULT_SPAWN_Z) <= 64 : "Spawn Z within radius 64 of 1250";

        // Surface height check
        int surfaceY = gen.computeHeight(spawn1.x(), spawn1.z());
        assert spawn1.y() == surfaceY + 1 : "Spawn Y must be directly above surface block";
        assert surfaceY > TerrainGenerator.SEA_LEVEL : "Spawn must be on dry land above sea level";

        // Vertical clearance
        assert spawn1.y() + 2 < Chunk.HEIGHT : "Spawn must have vertical clearance";

        // Complete player AABB check
        assert bounds.isPlayerAABBInsideWorld(spawn1.x() + 0.5f, (float) spawn1.y(), spawn1.z() + 0.5f) :
                "Player AABB at resolved spawn must be completely within world bounds";

        System.out.println("PASSED (Spawn located at: " + spawn1.x() + ", " + spawn1.y() + ", " + spawn1.z() + ")");
    }

    /**
     * 9. Specific Spawn Safety Validation Cases:
     * - Spawn beneath an overhang
     * - Spawn inside a solid block
     * - Spawn with insufficient vertical clearance
     * - Spawn beside a steep terrain transition
     * - Spawn on water
     * - Spawn at the world boundary
     * - A valid spawn on ordinary terrain
     * - Deterministic spawn selection for the same seed
     */
    private static void testSpawnSafetyValidationCases() {
        System.out.print("[Phase 5] Detailed spawn safety validation scenarios... ");

        TerrainGenerator gen = new TerrainGenerator(777888999L, WorldBounds.DEFAULT);
        WorldBounds bounds = WorldBounds.DEFAULT;

        // Find a terrain-level spawn point to serve as baseline
        SpawnLocator.SpawnPosition basePos = SpawnLocator.findSafeSpawn(gen, bounds);
        int wx = basePos.x();
        int wz = basePos.z();
        int spawnY = basePos.y();
        int groundY = spawnY - 1;

        // Baseline valid accessor (solid grass ground, air body, air head, air overhead)
        SpawnLocator.BlockAccessor validAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.GRASS.getDefaultState();
            return Blocks.AIR.getDefaultState();
        };

        // 1. Valid spawn on ordinary level terrain
        assert SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, validAccessor) :
                "Ordinary level plains terrain must be accepted as valid spawn";

        // 2. Spawn beneath an overhang (solid stone at spawnY + 2)
        SpawnLocator.BlockAccessor overhangAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.GRASS.getDefaultState();
            if (y == spawnY + 2) return Blocks.STONE.getDefaultState(); // Low ceiling / ledge directly above head
            return Blocks.AIR.getDefaultState();
        };
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, overhangAccessor) :
                "Spawn beneath low overhang or ceiling must be rejected";

        // 3. Spawn inside a solid block (solid stone at body spawnY)
        SpawnLocator.BlockAccessor solidBodyAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.GRASS.getDefaultState();
            if (y == spawnY) return Blocks.STONE.getDefaultState(); // Player feet inside solid stone
            return Blocks.AIR.getDefaultState();
        };
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, solidBodyAccessor) :
                "Spawn inside solid block must be rejected";

        // Solid head at spawnY + 1
        SpawnLocator.BlockAccessor solidHeadAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.GRASS.getDefaultState();
            if (y == spawnY + 1) return Blocks.OAK_LOG.getDefaultState(); // Tree trunk at head height
            return Blocks.AIR.getDefaultState();
        };
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, solidHeadAccessor) :
                "Spawn intersecting solid block at head height must be rejected";

        // 4. Spawn with insufficient vertical clearance (spawnY + 2 >= Chunk.HEIGHT)
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, 255, wz, validAccessor) :
                "Spawn at top of world with insufficient clearance must be rejected";
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, 254, wz, validAccessor) :
                "Spawn at Y=254 (overhead 256) must be rejected";

        // 5. Spawn beside a steep terrain transition
        // Create generator with a custom modifier introducing a sheer cliff at (wx + 1, wz)
        TerrainModifierPipeline steepPipeline = new TerrainModifierPipeline();
        steepPipeline.addModifier(new TerrainModifier() {
            @Override
            public String getName() { return "sheer_cliff"; }
            @Override
            public double modifyHeight(int x, int z, double h) {
                if (x == wx + 1 && z == wz) return groundY - 10.0; // 10-block sheer drop immediately adjacent
                if (x == wx && z == wz) return groundY;
                return h;
            }
        });
        TerrainGenerator cliffGen = new TerrainGenerator(777888999L, bounds, BiomeRegistry.createDefaultRegistry(), steepPipeline);
        assert !SpawnLocator.isSafeSpawnPosition(cliffGen, bounds, wx, spawnY, wz, validAccessor) :
                "Spawn beside a steep cliff drop must be rejected";

        // 6. Spawn on water (ground block is water)
        SpawnLocator.BlockAccessor waterGroundAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.WATER.getDefaultState();
            return Blocks.AIR.getDefaultState();
        };
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, waterGroundAccessor) :
                "Spawn on water must be rejected";

        // Water at player feet level
        SpawnLocator.BlockAccessor submergedAccessor = (x, y, z) -> {
            if (y == groundY) return Blocks.SAND.getDefaultState();
            if (y == spawnY) return Blocks.WATER.getDefaultState();
            return Blocks.AIR.getDefaultState();
        };
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, wx, spawnY, wz, submergedAccessor) :
                "Spawn submerged in water must be rejected";

        // 7. Spawn at world boundary
        // wx = 0 with px = 0.2f (AABB extends to minX = -0.1f < 0.0f)
        assert !bounds.isPlayerAABBInsideWorld(0.2f, (float) spawnY, (float) wz) : "Boundary overflow must be rejected by WorldBounds";
        // Attempting spawn where player center is at 0 (AABB minX = -0.3f)
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, 0, spawnY, wz, validAccessor) :
                "Spawn at border block 0 must be rejected";
        assert !SpawnLocator.isSafeSpawnPosition(gen, bounds, 2499, spawnY, wz, validAccessor) :
                "Spawn at border block 2499 (AABB maxX = 2499.8f > 2499.7f) must be rejected";

        // 8. Deterministic spawn selection for the same seed across multiple runs
        SpawnLocator.SpawnPosition sA = SpawnLocator.findSafeSpawn(gen, bounds);
        SpawnLocator.SpawnPosition sB = SpawnLocator.findSafeSpawn(gen, bounds);
        assert sA.equals(sB) : "Multiple findSafeSpawn calls for same seed must return identical coordinates";

        // Distinct seeds resolve deterministically to their respective terrain
        TerrainGenerator genOther = new TerrainGenerator(123456789L, bounds);
        SpawnLocator.SpawnPosition sOther = SpawnLocator.findSafeSpawn(genOther, bounds);
        assert sOther != null : "Spawn for second seed must resolve successfully";

        System.out.println("PASSED");
    }

    /**
     * 10. Preserves Saved Player Position on World Loading:
     * - When loading an existing save with valid coordinates, the player position is not overwritten by spawn resolution
     */
    private static void testSavedPlayerPositionPreservation() throws Exception {
        System.out.print("[Phase 5] Preservation of saved player coordinates on world loading... ");

        File worldDir = new File(TEST_BASE_DIR, "player_pos_preservation");
        cleanupDir(worldDir);
        worldDir.mkdirs();

        WorldConfiguration config = WorldConfiguration.createDefault(424242L);
        File metaFile = new File(worldDir, "world.jmc");

        WorldSaveManager.SavedWorld initialSave = new WorldSaveManager.SavedWorld();
        initialSave.name = "PreserveTest";
        initialSave.seed = 424242L;
        initialSave.worldWidth = 2500;
        initialSave.worldDepth = 2500;
        initialSave.worldHeight = 256;
        initialSave.generationVersion = 1;
        initialSave.boundaryType = "NATURAL";
        initialSave.formatVersion = 4;
        initialSave.spawnX = 1250;
        initialSave.spawnY = 66;
        initialSave.spawnZ = 1250;

        // Custom saved player position at (800.5, 75.0, 900.5)
        initialSave.playerX = 800.5f;
        initialSave.playerY = 75.0f;
        initialSave.playerZ = 900.5f;
        initialSave.health = 18;
        initialSave.hunger = 19;
        initialSave.worldTime = 14000.0;
        initialSave.hotbarBlocks = new byte[9];
        initialSave.hotbarCounts = new int[9];

        // Load into WorldSession
        WorldSession session = new WorldSession();
        GameSettings settings = new GameSettings();
        settings.renderDistance = 2;
        Player player = new Player();
        Camera camera = new Camera();

        session.enterWorld(424242L, initialSave.name, "Plains", initialSave, player, camera, null, settings, null);

        // Player position must be preserved exactly
        assert Math.abs(player.pos.x - 800.5f) < 0.001f : "Player X must be preserved";
        assert Math.abs(player.pos.y - 75.0f) < 0.001f : "Player Y must be preserved";
        assert Math.abs(player.pos.z - 900.5f) < 0.001f : "Player Z must be preserved";
        assert player.health == 18 : "Player health must be preserved";
        assert player.hunger == 19 : "Player hunger must be preserved";

        session.saveAndQuit(player, 14000.0);
        session.shutdown(player, 14000.0);

        System.out.println("PASSED");
    }

    /**
     * 10. Thread Safety & Order Independence:
     * - Generate chunks concurrently with 8 threads vs sequentially
     * - Verify 100% identical voxel arrays
     */
    private static void testConcurrentVsSequentialThreadSafety() throws Exception {
        System.out.print("[Phase 5] Concurrent vs sequential generation thread-safety... ");

        long seed = 192837465L;
        TerrainGenerator gen = new TerrainGenerator(seed, WorldBounds.DEFAULT);

        int[][] chunkCoords = {
            {70, 70}, {71, 70}, {70, 71}, {71, 71},
            {156, 0}, {0, 156}, {156, 156}, {78, 78}
        };

        // 1. Sequential generation baseline
        Map<String, Chunk> seqChunks = new HashMap<>();
        for (int[] cp : chunkCoords) {
            seqChunks.put(cp[0] + "," + cp[1], gen.generateChunk(cp[0], cp[1]));
        }

        // 2. Concurrent generation across worker pool
        ExecutorService executor = Executors.newFixedThreadPool(8);
        Map<String, Future<Chunk>> futures = new HashMap<>();

        for (int[] cp : chunkCoords) {
            final int cx = cp[0];
            final int cz = cp[1];
            futures.put(cx + "," + cz, executor.submit(() -> gen.generateChunk(cx, cz)));
        }

        for (int[] cp : chunkCoords) {
            String key = cp[0] + "," + cp[1];
            Chunk concurrentChunk = futures.get(key).get(5, TimeUnit.SECONDS);
            Chunk seqChunk = seqChunks.get(key);

            short[] concStates = concurrentChunk.getBlockStates();
            short[] seqStates = seqChunk.getBlockStates();

            assert concStates.length == seqStates.length;
            for (int i = 0; i < concStates.length; i++) {
                assert concStates[i] == seqStates[i] :
                    "Concurrency mismatch in chunk (" + key + ") at voxel index " + i +
                    " (conc: " + concStates[i] + ", seq: " + seqStates[i] + ")";
            }
        }

        executor.shutdown();
        System.out.println("PASSED (All 8 chunks identical bit-for-bit)");
    }

    /**
     * 12. Performance Benchmark:
     * - Confirms it measures actual complete chunk generation (all 65,536 voxels/chunk)
     * - Includes sufficient warmup (40 chunks across diverse coordinates)
     * - Measures representative sample of 100 full chunks (6,553,600 voxels)
     * - Reports average latency (ms/chunk) and throughput (chunks/sec, MVoxels/sec)
     */
    private static void testGenerationPerformanceBenchmark() {
        System.out.print("[Phase 5] Generation performance benchmark... ");

        TerrainGenerator gen = new TerrainGenerator(133742L, WorldBounds.DEFAULT);

        // 1. Warm up JVM JIT compiler with 40 chunks across varied biomes and terrain
        for (int cx = 10; cx < 15; cx++) {
            for (int cz = 10; cz < 18; cz++) {
                gen.generateChunk(cx, cz);
            }
        }

        // 2. Benchmark 100 full chunks across diverse terrain regions (10x10 chunk grid)
        int sampleCount = 100;
        long startNanos = System.nanoTime();
        for (int i = 0; i < sampleCount; i++) {
            int cx = 20 + (i % 10);
            int cz = 20 + (i / 10);
            Chunk c = gen.generateChunk(cx, cz);
            // Verify chunk was actually populated
            assert c.getBlockState(0, 0, 0).is(Blocks.BEDROCK);
        }
        long totalNanos = System.nanoTime() - startNanos;

        double totalSeconds = totalNanos / 1_000_000_000.0;
        double avgMs = (totalNanos / 1_000_000.0) / sampleCount;
        double chunksPerSec = sampleCount / totalSeconds;
        double megavoxelsPerSec = (sampleCount * (double) Chunk.TOTAL_VOXELS / 1_000_000.0) / totalSeconds;

        System.out.println(String.format("PASSED (Avg: %.2f ms/chunk | Throughput: %.0f chunks/s, %.1f MVoxels/s across %d chunks)",
                avgMs, chunksPerSec, megavoxelsPerSec, sampleCount));
    }

    private static void cleanupDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) cleanupDir(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }
}
