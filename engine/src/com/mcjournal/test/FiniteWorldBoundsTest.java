package com.mcjournal.test;

import com.mcjournal.ChunkPos;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;

/**
 * Comprehensive test suite for Phase 1: Finite World Configuration and Bounds.
 */
public class FiniteWorldBoundsTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING PHASE 1: WORLD CONFIG & BOUNDS TESTS  ");
        System.out.println("=================================================");

        testConfigurationImmutabilityAndDefaults();
        testMinMaxAndCornerCoordinates();
        testNegativeCoordinatesRejection();
        testX2500AndZ2500Rejection();
        testYNegativeAnd256Rejection();
        testChunkBoundsAndCounts();
        testPartialEdgeChunkCalculations();
        testCoordinateConversionsAndMath();
        testDistanceToEdgeCalculations();
        testAccessSemanticsAndStatus();

        System.out.println(">>> ALL PHASE 1 WORLD BOUNDS TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testConfigurationImmutabilityAndDefaults() {
        System.out.print("[Phase 1] WorldConfiguration defaults & immutability... ");

        WorldConfiguration config = WorldConfiguration.createDefault(98765L);
        assert config.width() == 2500 : "Expected default width 2500, got: " + config.width();
        assert config.depth() == 2500 : "Expected default depth 2500, got: " + config.depth();
        assert config.height() == 256 : "Expected default height 256, got: " + config.height();
        assert config.seed() == 98765L : "Seed mismatch";
        assert config.generationVersion() == 1 : "Expected generationVersion 1";
        assert config.boundaryType() == WorldConfiguration.BoundaryType.NATURAL : "Expected NATURAL boundary type";
        assert config.spawnX() == 1250 : "Expected spawnX 1250, got: " + config.spawnX();
        assert config.spawnZ() == 1250 : "Expected spawnZ 1250, got: " + config.spawnZ();
        assert config.spawnY() == 66 : "Expected spawnY 66";

        // Validate illegal arguments
        try {
            new WorldConfiguration(0, 2500, 256, 1L, 1, WorldConfiguration.BoundaryType.NATURAL, 0, 0, 0);
            assert false : "Expected exception for width=0";
        } catch (IllegalArgumentException expected) {}

        try {
            new WorldConfiguration(2500, -1, 256, 1L, 1, WorldConfiguration.BoundaryType.NATURAL, 0, 0, 0);
            assert false : "Expected exception for depth < 0";
        } catch (IllegalArgumentException expected) {}

        System.out.println("PASSED");
    }

    private static void testMinMaxAndCornerCoordinates() {
        System.out.print("[Phase 1] All 4 world corners and min/max coordinates... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // Minimum valid coordinate (0, 0, 0)
        assert bounds.isValidBlock(0, 0, 0) : "Origin (0,0,0) must be valid";
        assert bounds.isValidHorizontal(0, 0) : "(0,0) horizontal must be valid";
        assert bounds.isValidVertical(0) : "Y=0 must be valid";

        // Maximum valid coordinate (2499, 255, 2499)
        assert bounds.isValidBlock(2499, 255, 2499) : "Max corner (2499, 255, 2499) must be valid";
        assert bounds.isValidHorizontal(2499, 2499) : "(2499, 2499) horizontal must be valid";
        assert bounds.isValidVertical(255) : "Y=255 must be valid";

        // All 4 World Corners
        int[][] corners = {
            {0, 0},         // North-West
            {2499, 0},      // North-East
            {0, 2499},      // South-West
            {2499, 2499}    // South-East
        };

        for (int[] corner : corners) {
            int cx = corner[0];
            int cz = corner[1];
            assert bounds.isValidHorizontal(cx, cz) : "Corner (" + cx + ", " + cz + ") must be valid";
            assert bounds.isValidBlock(cx, 0, cz) : "Corner (" + cx + ", 0, " + cz + ") must be valid";
            assert bounds.isValidBlock(cx, 128, cz) : "Corner (" + cx + ", 128, " + cz + ") must be valid";
            assert bounds.isValidBlock(cx, 255, cz) : "Corner (" + cx + ", 255, " + cz + ") must be valid";
        }

        // Center Coordinate
        assert bounds.isValidBlock(1250, 64, 1250) : "Center (1250, 64, 1250) must be valid";

        System.out.println("PASSED");
    }

    private static void testNegativeCoordinatesRejection() {
        System.out.print("[Phase 1] Negative coordinate strict rejection... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // Negative X
        assert !bounds.isValidHorizontal(-1, 0) : "X=-1 must be rejected";
        assert !bounds.isValidHorizontal(-100, 1000) : "X=-100 must be rejected";
        assert !bounds.isValidBlock(-1, 64, 0) : "(-1, 64, 0) must be rejected";

        // Negative Z
        assert !bounds.isValidHorizontal(0, -1) : "Z=-1 must be rejected";
        assert !bounds.isValidHorizontal(1000, -500) : "Z=-500 must be rejected";
        assert !bounds.isValidBlock(0, 64, -1) : "(0, 64, -1) must be rejected";

        // Both negative
        assert !bounds.isValidHorizontal(-1, -1) : "(-1, -1) must be rejected";
        assert !bounds.isValidBlock(-2500, 64, -2500) : "(-2500, 64, -2500) must be rejected";

        System.out.println("PASSED");
    }

    private static void testX2500AndZ2500Rejection() {
        System.out.print("[Phase 1] X=2500 and Z=2500 boundary rejection... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // X = 2500 (one past world width 0..2499)
        assert !bounds.isValidHorizontal(2500, 100) : "X=2500 must be rejected";
        assert !bounds.isValidBlock(2500, 64, 100) : "(2500, 64, 100) must be rejected";

        // Z = 2500 (one past world depth 0..2499)
        assert !bounds.isValidHorizontal(100, 2500) : "Z=2500 must be rejected";
        assert !bounds.isValidBlock(100, 64, 2500) : "(100, 64, 2500) must be rejected";

        // Both X=2500 and Z=2500
        assert !bounds.isValidHorizontal(2500, 2500) : "(2500, 2500) must be rejected";
        assert !bounds.isValidBlock(2500, 64, 2500) : "(2500, 64, 2500) must be rejected";

        // Large positive coordinates
        assert !bounds.isValidHorizontal(5000, 5000) : "(5000, 5000) must be rejected";

        System.out.println("PASSED");
    }

    private static void testYNegativeAnd256Rejection() {
        System.out.print("[Phase 1] Y=-1 and Y=256 vertical bound rejection... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        assert !bounds.isValidVertical(-1) : "Y=-1 must be invalid";
        assert !bounds.isValidVertical(-100) : "Y=-100 must be invalid";
        assert !bounds.isValidVertical(256) : "Y=256 must be invalid (height is 256, indices 0..255)";
        assert !bounds.isValidVertical(512) : "Y=512 must be invalid";

        assert bounds.isValidVertical(0) : "Y=0 must be valid";
        assert bounds.isValidVertical(255) : "Y=255 must be valid";

        // 3D block validations
        assert !bounds.isValidBlock(100, -1, 100) : "(100, -1, 100) must be invalid";
        assert !bounds.isValidBlock(100, 256, 100) : "(100, 256, 100) must be invalid";

        System.out.println("PASSED");
    }

    private static void testChunkBoundsAndCounts() {
        System.out.print("[Phase 1] Chunk bounds [0..156] and chunk counts... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        assert bounds.getMinChunkX() == 0 : "MinChunkX must be 0";
        assert bounds.getMinChunkZ() == 0 : "MinChunkZ must be 0";
        assert bounds.getMaxChunkX() == 156 : "MaxChunkX must be 156 (ceil(2500/16)-1)";
        assert bounds.getMaxChunkZ() == 156 : "MaxChunkZ must be 156 (ceil(2500/16)-1)";
        assert bounds.getTotalChunksX() == 157 : "TotalChunksX must be 157";
        assert bounds.getTotalChunksZ() == 157 : "TotalChunksZ must be 157";
        assert bounds.getTotalChunks() == 24649 : "Total chunks must be 157*157 = 24,649";

        // Valid chunks
        assert bounds.isValidChunk(0, 0) : "Chunk (0,0) must be valid";
        assert bounds.isValidChunk(new ChunkPos(0, 0)) : "ChunkPos (0,0) must be valid";
        assert bounds.isValidChunk(156, 156) : "Chunk (156,156) must be valid";
        assert bounds.isValidChunk(new ChunkPos(156, 156)) : "ChunkPos (156,156) must be valid";
        assert bounds.isValidChunk(78, 78) : "Center chunk (78,78) must be valid";

        // Invalid chunks
        assert !bounds.isValidChunk(157, 0) : "Chunk (157,0) must be invalid";
        assert !bounds.isValidChunk(0, 157) : "Chunk (0,157) must be invalid";
        assert !bounds.isValidChunk(157, 157) : "Chunk (157,157) must be invalid";
        assert !bounds.isValidChunk(-1, 0) : "Chunk (-1,0) must be invalid";
        assert !bounds.isValidChunk(0, -1) : "Chunk (0,-1) must be invalid";
        assert !bounds.isValidChunk(-1, -1) : "Chunk (-1,-1) must be invalid";

        System.out.println("PASSED");
    }

    private static void testPartialEdgeChunkCalculations() {
        System.out.print("[Phase 1] Partial edge chunk calculations & local block counts... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // Interior chunk (0, 0): full 16x16 chunk
        assert !bounds.isPartialChunk(0, 0) : "Chunk (0,0) is not partial";
        assert bounds.getValidLocalBlockCountX(0) == 16 : "Chunk (0) local count X must be 16";
        assert bounds.getValidLocalBlockCountZ(0) == 16 : "Chunk (0) local count Z must be 16";
        assert bounds.getValidLocalMaxX(0) == 15 : "Chunk (0) local max X must be 15";
        assert bounds.getValidLocalMaxZ(0) == 15 : "Chunk (0) local max Z must be 15";

        // Interior chunk (50, 50): full 16x16 chunk
        assert !bounds.isPartialChunk(50, 50) : "Chunk (50,50) is not partial";
        assert bounds.getValidLocalBlockCountX(50) == 16 : "Chunk (50) local count X must be 16";
        assert bounds.getValidLocalBlockCountZ(50) == 16 : "Chunk (50) local count Z must be 16";

        // Boundary chunk (156, 0): partial along X only
        assert bounds.isPartialChunkX(156) : "Chunk 156 must be partial along X";
        assert !bounds.isPartialChunkZ(0) : "Chunk 0 is not partial along Z";
        assert bounds.isPartialChunk(156, 0) : "Chunk (156, 0) is partial";
        // 2500 - 156 * 16 = 2500 - 2496 = 4 blocks (local X = 0, 1, 2, 3)
        assert bounds.getValidLocalBlockCountX(156) == 4 : "Chunk 156 valid local X count must be 4, got: " + bounds.getValidLocalBlockCountX(156);
        assert bounds.getValidLocalBlockCountZ(0) == 16 : "Chunk 0 valid local Z count must be 16";
        assert bounds.getValidLocalMaxX(156) == 3 : "Chunk 156 valid local max X must be 3";
        assert bounds.getValidLocalMaxZ(0) == 15 : "Chunk 0 valid local max Z must be 15";

        // Boundary chunk (0, 156): partial along Z only
        assert !bounds.isPartialChunkX(0) : "Chunk 0 is not partial along X";
        assert bounds.isPartialChunkZ(156) : "Chunk 156 must be partial along Z";
        assert bounds.isPartialChunk(0, 156) : "Chunk (0, 156) is partial";
        assert bounds.getValidLocalBlockCountX(0) == 16 : "Chunk 0 valid local X count must be 16";
        assert bounds.getValidLocalBlockCountZ(156) == 4 : "Chunk 156 valid local Z count must be 4";

        // Corner boundary chunk (156, 156): partial along BOTH X and Z
        assert bounds.isPartialChunk(156, 156) : "Chunk (156, 156) must be partial";
        assert bounds.getValidLocalBlockCountX(156) == 4 : "Chunk 156 countX must be 4";
        assert bounds.getValidLocalBlockCountZ(156) == 4 : "Chunk 156 countZ must be 4";
        assert bounds.getValidLocalMaxX(156) == 3 : "Chunk 156 maxX must be 3";
        assert bounds.getValidLocalMaxZ(156) == 3 : "Chunk 156 maxZ must be 3";

        // Local voxel queries for partial chunk 156
        assert bounds.isLocalBlockValid(156, 156, 0, 64, 0) : "Local (0, 64, 0) in (156, 156) must be valid";
        assert bounds.isLocalBlockValid(156, 156, 3, 64, 3) : "Local (3, 64, 3) in (156, 156) must be valid";
        assert !bounds.isLocalBlockValid(156, 156, 4, 64, 0) : "Local (4, 64, 0) in (156, 156) must be INVALID (out of world)";
        assert !bounds.isLocalBlockValid(156, 156, 0, 64, 4) : "Local (0, 64, 4) in (156, 156) must be INVALID (out of world)";
        assert !bounds.isLocalBlockValid(156, 156, 15, 64, 15) : "Local (15, 64, 15) in (156, 156) must be INVALID (out of world)";

        System.out.println("PASSED");
    }

    private static void testCoordinateConversionsAndMath() {
        System.out.print("[Phase 1] FloorDiv, FloorMod & coordinate conversions... ");

        // World to chunk X/Z
        assert WorldBounds.toChunkX(0) == 0;
        assert WorldBounds.toChunkX(15) == 0;
        assert WorldBounds.toChunkX(16) == 1;
        assert WorldBounds.toChunkX(2495) == 155;
        assert WorldBounds.toChunkX(2496) == 156;
        assert WorldBounds.toChunkX(2499) == 156;
        assert WorldBounds.toChunkX(2500) == 156; // 2500 / 16 = 156 with rem 4

        // Negative floorDiv behavior
        assert WorldBounds.toChunkX(-1) == -1;
        assert WorldBounds.toChunkX(-16) == -1;
        assert WorldBounds.toChunkX(-17) == -2;

        // World to local X/Z (floorMod)
        assert WorldBounds.toLocalX(0) == 0;
        assert WorldBounds.toLocalX(15) == 15;
        assert WorldBounds.toLocalX(16) == 0;
        assert WorldBounds.toLocalX(2496) == 0;
        assert WorldBounds.toLocalX(2499) == 3;
        assert WorldBounds.toLocalX(2500) == 4;

        // Chunk + Local to World X/Z
        assert WorldBounds.toWorldX(0, 0) == 0;
        assert WorldBounds.toWorldX(0, 15) == 15;
        assert WorldBounds.toWorldX(1, 0) == 16;
        assert WorldBounds.toWorldX(156, 0) == 2496;
        assert WorldBounds.toWorldX(156, 3) == 2499;

        System.out.println("PASSED");
    }

    private static void testDistanceToEdgeCalculations() {
        System.out.print("[Phase 1] World edge distance calculations... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // Exactly on border
        assert bounds.distanceToEdge(0, 100) == 0 : "Distance at x=0 must be 0";
        assert bounds.distanceToEdge(2499, 100) == 0 : "Distance at x=2499 must be 0";
        assert bounds.distanceToEdge(100, 0) == 0 : "Distance at z=0 must be 0";
        assert bounds.distanceToEdge(100, 2499) == 0 : "Distance at z=2499 must be 0";

        // Corners
        assert bounds.distanceToEdge(0, 0) == 0 : "Corner (0,0) distance must be 0";
        assert bounds.distanceToEdge(2499, 2499) == 0 : "Corner (2499,2499) distance must be 0";

        // Normalized distance at borders
        assert bounds.distanceToEdgeNormalized(0, 500) == 0.0 : "Normalized at border must be 0.0";
        assert bounds.distanceToEdgeNormalized(2499, 500) == 0.0 : "Normalized at border must be 0.0";

        // Center: (1249, 1249) or (1250, 1250)
        // distance from 1249 to 0 is 1249, to 2499 is 1250
        assert bounds.distanceToEdge(1249, 1249) == 1249 : "Center distance mismatch";
        double normCenter = bounds.distanceToEdgeNormalized(1250, 1250);
        assert normCenter >= 0.99 && normCenter <= 1.0 : "Normalized at center must be ~1.0, got: " + normCenter;

        // Negative coordinates outside world
        assert bounds.distanceToEdge(-10, 500) == -10 : "Outside distance must be negative";
        assert bounds.distanceToEdgeNormalized(-10, 500) == 0.0 : "Normalized outside must be 0.0";

        System.out.println("PASSED");
    }

    private static void testAccessSemanticsAndStatus() {
        System.out.print("[Phase 1] Access semantics (OUT_OF_WORLD vs UNLOADED vs LOADED)... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // OUT_OF_WORLD for negative coordinates
        assert bounds.getCoordinateStatus(null, -1, 64, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getCoordinateStatus(null, 0, 64, -1) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getCoordinateStatus(null, 0, -1, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;

        // OUT_OF_WORLD for coordinates >= 2500 or Y >= 256
        assert bounds.getCoordinateStatus(null, 2500, 64, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getCoordinateStatus(null, 0, 64, 2500) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getCoordinateStatus(null, 0, 256, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;

        // UNLOADED for valid coordinates when chunkManager is null or chunk not in memory
        assert bounds.getCoordinateStatus(null, 1250, 64, 1250) == WorldBounds.CoordinateStatus.UNLOADED;
        assert bounds.getCoordinateStatus(null, 0, 64, 0) == WorldBounds.CoordinateStatus.UNLOADED;
        assert bounds.getCoordinateStatus(null, 2499, 64, 2499) == WorldBounds.CoordinateStatus.UNLOADED;

        // Chunk coordinate status
        assert bounds.getChunkCoordinateStatus(null, -1, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getChunkCoordinateStatus(null, 157, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD;
        assert bounds.getChunkCoordinateStatus(null, 0, 0) == WorldBounds.CoordinateStatus.UNLOADED;
        assert bounds.getChunkCoordinateStatus(null, 156, 156) == WorldBounds.CoordinateStatus.UNLOADED;

        System.out.println("PASSED");
    }
}
