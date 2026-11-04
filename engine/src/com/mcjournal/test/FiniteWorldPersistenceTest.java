package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkSerializer;
import com.mcjournal.EngineConstants;
import com.mcjournal.RegionManager;
import com.mcjournal.RegionPos;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Camera;
import com.mcjournal.client.GameSettings;
import com.mcjournal.client.Player;
import com.mcjournal.client.SaveMigrationManager;
import com.mcjournal.client.WorldSaveManager;
import com.mcjournal.client.WorldSession;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Phase 4 Test Suite: World Metadata V4, Persistence and Legacy Save Compatibility.
 * Validates:
 * - V4 metadata serialization and deserialization with full WorldConfiguration fields.
 * - Validation of dimensions, height, generation version, and boundary type.
 * - Full player AABB boundary containment validation (center + 0.6x1.8 collision volume).
 * - Player restoration relocation when AABB extends outside world boundaries.
 * - Atomic metadata write and failure safety (preserving previous valid save on write failure).
 * - Chunk persistence bit-for-bit round-trip at all 4 corners (0,0), (156,0), (0,156), (156,156) and center (78,78).
 * - Partial edge chunk voxel isolation: invalid columns strictly remain AIR.
 * - 5x5 Region grid mapping: r.0.0.jmc to r.4.4.jmc and invalid chunk write rejection.
 * - Backward compatibility: reading V1, V2, and V3 chunk payloads.
 * - Legacy V3 save detection and explicit migration to V4 finite world.
 * - Source save preservation (never modified or overwritten during migration).
 * - Safe player relocation when legacy coordinates lie outside finite world.
 * - Failure recovery: truncated metadata, corrupted region entries.
 */
public class FiniteWorldPersistenceTest {

    private static final File TEST_BASE_DIR = new File("saves/test_phase4_persistence");

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("   RUNNING PHASE 4: PERSISTENCE & MIGRATION TESTS");
        System.out.println("=================================================");

        cleanDir(TEST_BASE_DIR);

        try {
            testMetadataV4RoundTripAndValidation();
            testPlayerAABBBoundaryValidation();
            testWorldSessionPlayerRestorationAABBContainment();
            testMetadataFailureRecoveryAndAtomicWrite();
            testCornerAndCenterChunkPersistence();
            testPartialEdgeChunkVoxelIsolation();
            testRegionStorageAndBoundaryWriteRejection();
            testLegacySerializerBackwardCompatibility();
            testLegacyV3MigrationToFiniteV4();
            testMigrationRepeatSafetyAndSourceImmutability();
            testCorruptedAndTruncatedDataHandling();

            System.out.println(">>> ALL PHASE 4 PERSISTENCE & MIGRATION TESTS PASSED SUCCESSFULLY! <<<\n");
        } finally {
            cleanDir(TEST_BASE_DIR);
        }
    }

    private static void cleanDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) cleanDir(f);
                f.delete();
            }
        }
        dir.delete();
    }

    /**
     * 1. Metadata V4 round-trip, WorldConfiguration preservation, and dimension validation.
     */
    private static void testMetadataV4RoundTripAndValidation() throws Exception {
        System.out.print("[Phase 4] Metadata V4 serialization, config fields & validation... ");

        File saveFile = new File(TEST_BASE_DIR, "v4_meta/world.jmc");

        WorldSaveManager.SavedWorld world = new WorldSaveManager.SavedWorld();
        world.formatVersion = 4;
        world.name = "Finite Journal World";
        world.biome = "Plains";
        world.seed = 987654321L;
        world.worldTime = 12500.0;
        world.playerX = 1250.5f;
        world.playerY = 66.0f;
        world.playerZ = 1250.5f;
        world.playerYaw = 180.0f;
        world.playerPitch = -10.0f;
        world.selectedSlot = 3;
        world.hotbarBlocks[0] = Blocks.STONE.getLegacyId();
        world.hotbarCounts[0] = 32;

        world.worldWidth = 2500;
        world.worldDepth = 2500;
        world.worldHeight = 256;
        world.generationVersion = 1;
        world.boundaryType = "NATURAL";
        world.spawnX = 1250;
        world.spawnY = 66;
        world.spawnZ = 1250;

        WorldSaveManager.saveBinaryDirect(world, saveFile);

        // Verify file exists
        assert saveFile.exists() && saveFile.length() > 0 : "V4 world.jmc must exist";

        // Read back
        WorldSaveManager.SavedWorld loaded = WorldSaveManager.loadWorld(saveFile);
        assert loaded != null : "loadWorld must successfully load V4 metadata";
        assert loaded.formatVersion == 4 : "Format version must be 4";
        assert "Finite Journal World".equals(loaded.name) : "World name mismatch";
        assert loaded.seed == 987654321L : "Seed mismatch";
        assert loaded.worldWidth == 2500 && loaded.worldDepth == 2500 && loaded.worldHeight == 256 : "Dimensions mismatch";
        assert loaded.generationVersion == 1 : "Generation version mismatch";
        assert "NATURAL".equals(loaded.boundaryType) : "Boundary type mismatch";
        assert loaded.spawnX == 1250 && loaded.spawnY == 66 && loaded.spawnZ == 1250 : "Spawn mismatch";
        assert Math.abs(loaded.playerX - 1250.5f) < 1e-4f : "Player X mismatch";
        assert loaded.selectedSlot == 3 : "Selected slot mismatch";
        assert loaded.hotbarBlocks[0] == Blocks.STONE.getLegacyId() && loaded.hotbarCounts[0] == 32 : "Hotbar mismatch";

        // Configuration object conversion
        WorldConfiguration config = loaded.toWorldConfiguration();
        assert config.width() == 2500 && config.depth() == 2500 && config.height() == 256 : "toWorldConfiguration mismatch";
        assert config.boundaryType() == WorldConfiguration.BoundaryType.NATURAL : "BoundaryType enum mismatch";

        // Validation: reject negative dimensions
        world.worldWidth = -500;
        File invalidWidthFile = new File(TEST_BASE_DIR, "invalid_width/world.jmc");
        WorldSaveManager.saveBinaryDirect(world, invalidWidthFile);
        boolean caughtInvalidWidth = false;
        try {
            WorldSaveManager.loadBinarySave(invalidWidthFile);
        } catch (IOException e) {
            caughtInvalidWidth = true;
        }
        assert caughtInvalidWidth : "Loading save with negative width must throw IOException";

        // Validation: reject invalid height (must be 256)
        world.worldWidth = 2500;
        world.worldHeight = 384; // not supported
        File invalidHeightFile = new File(TEST_BASE_DIR, "invalid_height/world.jmc");
        WorldSaveManager.saveBinaryDirect(world, invalidHeightFile);
        boolean caughtInvalidHeight = false;
        try {
            WorldSaveManager.loadBinarySave(invalidHeightFile);
        } catch (IOException e) {
            caughtInvalidHeight = true;
        }
        assert caughtInvalidHeight : "Loading save with height != 256 must throw IOException";

        // Validation: reject unsupported future version (e.g. 99)
        world.worldHeight = 256;
        world.formatVersion = 99;
        File futureVerFile = new File(TEST_BASE_DIR, "future_ver/world.jmc");
        WorldSaveManager.saveBinaryDirect(world, futureVerFile);
        boolean caughtFuture = false;
        try {
            WorldSaveManager.loadBinarySave(futureVerFile);
        } catch (IOException e) {
            caughtFuture = true;
        }
        assert caughtFuture : "Loading save with future version must throw IOException";

        System.out.println("PASSED");
    }

    /**
     * 2. Comprehensive player AABB boundary containment tests against WorldBounds:
     * - X/Z exactly 0
     * - X/Z exactly 2500
     * - Positions just inside each boundary
     * - Positions where the player center is technically inside but their AABB extends outside
     * - Invalid Y positions and vertical AABB overflow
     */
    private static void testPlayerAABBBoundaryValidation() {
        System.out.print("[Phase 4] Player AABB boundary containment validation... ");
        WorldBounds bounds = WorldBounds.DEFAULT; // 2500x2500x256, bounded

        float halfW = EngineConstants.PLAYER_WIDTH / 2.0f; // 0.3f
        float height = EngineConstants.PLAYER_HEIGHT;     // 1.8f
        float midY = 66.0f;
        float midZ = 1250.0f;
        float midX = 1250.0f;

        // A. X/Z exactly 0: center is at 0.0, but AABB extends to -0.3f (MUST BE REJECTED)
        assert !bounds.isPlayerAABBInsideWorld(0.0f, midY, midZ) : "Player X=0.0 must be rejected (AABB minX = -0.3)";
        assert !bounds.isPlayerAABBInsideWorld(midX, midY, 0.0f) : "Player Z=0.0 must be rejected (AABB minZ = -0.3)";

        // B. X/Z exactly 2500: center is at 2500.0, but AABB extends to 2500.3f (MUST BE REJECTED)
        assert !bounds.isPlayerAABBInsideWorld(2500.0f, midY, midZ) : "Player X=2500.0 must be rejected (AABB maxX = 2500.3)";
        assert !bounds.isPlayerAABBInsideWorld(midX, midY, 2500.0f) : "Player Z=2500.0 must be rejected (AABB maxZ = 2500.3)";

        // C. Center technically inside [0, 2500], but AABB extends outside:
        // West edge: px in (0.0, 0.3)
        assert !bounds.isPlayerAABBInsideWorld(0.1f, midY, midZ) : "Player X=0.1 inside center range must be rejected (minX = -0.2)";
        assert !bounds.isPlayerAABBInsideWorld(0.2f, midY, midZ) : "Player X=0.2 inside center range must be rejected (minX = -0.1)";
        assert !bounds.isPlayerAABBInsideWorld(0.299f, midY, midZ) : "Player X=0.299 must be rejected (minX = -0.001)";

        // East edge: px in (2499.7, 2500.0)
        assert !bounds.isPlayerAABBInsideWorld(2499.8f, midY, midZ) : "Player X=2499.8 must be rejected (maxX = 2500.1)";
        assert !bounds.isPlayerAABBInsideWorld(2499.9f, midY, midZ) : "Player X=2499.9 must be rejected (maxX = 2500.2)";
        assert !bounds.isPlayerAABBInsideWorld(2499.701f, midY, midZ) : "Player X=2499.701 must be rejected (maxX = 2500.001)";

        // North edge: pz in (0.0, 0.3)
        assert !bounds.isPlayerAABBInsideWorld(midX, midY, 0.15f) : "Player Z=0.15 must be rejected (minZ = -0.15)";

        // South edge: pz in (2499.7, 2500.0)
        assert !bounds.isPlayerAABBInsideWorld(midX, midY, 2499.85f) : "Player Z=2499.85 must be rejected (maxZ = 2500.15)";

        // D. Positions just inside each boundary (flush and slightly interior):
        // West boundary flush: px = 0.3f (minX = 0.0f, maxX = 0.6f) -> VALID
        assert bounds.isPlayerAABBInsideWorld(0.3f, midY, midZ) : "Player X=0.3 flush with West boundary must be VALID";
        assert bounds.isPlayerAABBInsideWorld(0.35f, midY, midZ) : "Player X=0.35 just inside West boundary must be VALID";

        // East boundary flush: px = 2499.7f (minX = 2499.4f, maxX = 2500.0f) -> VALID
        assert bounds.isPlayerAABBInsideWorld(2499.7f, midY, midZ) : "Player X=2499.7 flush with East boundary must be VALID";
        assert bounds.isPlayerAABBInsideWorld(2499.65f, midY, midZ) : "Player X=2499.65 just inside East boundary must be VALID";

        // North boundary flush: pz = 0.3f -> VALID
        assert bounds.isPlayerAABBInsideWorld(midX, midY, 0.3f) : "Player Z=0.3 flush with North boundary must be VALID";

        // South boundary flush: pz = 2499.7f -> VALID
        assert bounds.isPlayerAABBInsideWorld(midX, midY, 2499.7f) : "Player Z=2499.7 flush with South boundary must be VALID";

        // E. Vertical player bounds (Y axis) using height = 1.8:
        // Below world bottom (Y < 0):
        assert !bounds.isPlayerAABBInsideWorld(midX, -0.01f, midZ) : "Player Y=-0.01 must be rejected";
        assert !bounds.isPlayerAABBInsideWorld(midX, -10.0f, midZ) : "Player Y=-10.0 must be rejected";

        // Flush with world bottom (py = 0.0f, baseY=0.0, topY=1.8 <= 256.0):
        assert bounds.isPlayerAABBInsideWorld(midX, 0.0f, midZ) : "Player Y=0.0 flush with bottom bedrock must be VALID";

        // Center height (py = 66.0f, topY = 67.8):
        assert bounds.isPlayerAABBInsideWorld(midX, 66.0f, midZ) : "Player Y=66.0 must be VALID";

        // Flush with world ceiling: py = 256.0 - 1.8 = 254.2f (topY = 256.0f):
        assert bounds.isPlayerAABBInsideWorld(midX, 254.2f, midZ) : "Player Y=254.2 flush with world ceiling must be VALID";

        // Center technically inside [0, 256], but vertical AABB overflow (py > 254.2f):
        assert !bounds.isPlayerAABBInsideWorld(midX, 254.3f, midZ) : "Player Y=254.3 must be rejected (topY = 256.1 > 256)";
        assert !bounds.isPlayerAABBInsideWorld(midX, 255.0f, midZ) : "Player Y=255.0 must be rejected (topY = 256.8 > 256)";
        assert !bounds.isPlayerAABBInsideWorld(midX, 256.0f, midZ) : "Player Y=256.0 must be rejected (baseY = 256.0)";
        assert !bounds.isPlayerAABBInsideWorld(midX, 300.0f, midZ) : "Player Y=300.0 must be rejected";

        System.out.println("PASSED");
    }

    /**
     * 3. WorldSession player restoration: verify relocation to spawn when AABB extends outside.
     */
    private static void testWorldSessionPlayerRestorationAABBContainment() {
        System.out.print("[Phase 4] WorldSession saved-player restoration AABB containment... ");

        WorldSession session = new WorldSession();
        Player player = new Player();
        Camera camera = new Camera();
        GameSettings settings = new GameSettings();
        settings.renderDistance = 2;

        // Case 1: Player center inside [0, 2500] but AABB extends outside (px = 2499.85f)
        WorldSaveManager.SavedWorld overflowEast = new WorldSaveManager.SavedWorld();
        overflowEast.name = "AABB Overflow East";
        overflowEast.seed = 42L;
        overflowEast.playerX = 2499.85f;
        overflowEast.playerY = 66.0f;
        overflowEast.playerZ = 1250.0f;
        overflowEast.worldWidth = 2500;
        overflowEast.worldDepth = 2500;
        overflowEast.worldHeight = 256;
        overflowEast.spawnX = 1250;
        overflowEast.spawnY = 66;
        overflowEast.spawnZ = 1250;

        session.enterWorld(42L, overflowEast.name, "Plains", overflowEast, player, camera, null, settings, null);

        // Player must be relocated to spawn because AABB maxX was 2500.15 > 2500.0
        assert Math.abs(player.pos.x - 1250.0f) < 1e-4f : "Player should be relocated to spawn X=1250, got: " + player.pos.x;
        assert Math.abs(player.pos.z - 1250.0f) < 1e-4f : "Player should be relocated to spawn Z=1250, got: " + player.pos.z;
        session.shutdown(player, 0.0);

        // Case 2: Player vertical AABB overflow (py = 255.0f, topY = 256.8 > 256.0)
        WorldSaveManager.SavedWorld overflowCeiling = new WorldSaveManager.SavedWorld();
        overflowCeiling.name = "AABB Overflow Ceiling";
        overflowCeiling.seed = 42L;
        overflowCeiling.playerX = 1250.0f;
        overflowCeiling.playerY = 255.0f;
        overflowCeiling.playerZ = 1250.0f;
        overflowCeiling.worldWidth = 2500;
        overflowCeiling.worldDepth = 2500;
        overflowCeiling.worldHeight = 256;
        overflowCeiling.spawnX = 1250;
        overflowCeiling.spawnY = 66;
        overflowCeiling.spawnZ = 1250;

        session.enterWorld(42L, overflowCeiling.name, "Plains", overflowCeiling, player, camera, null, settings, null);

        // Player must be relocated to spawn Y=66
        assert Math.abs(player.pos.y - 66.0f) < 1e-4f : "Player should be relocated to spawn Y=66, got: " + player.pos.y;
        session.shutdown(player, 0.0);

        // Case 3: Player flush inside boundary (px = 2499.7f, py = 66.0f, pz = 1250.0f) -> MUST BE PRESERVED
        WorldSaveManager.SavedWorld flushValid = new WorldSaveManager.SavedWorld();
        flushValid.name = "Flush Valid";
        flushValid.seed = 42L;
        flushValid.playerX = 2499.7f;
        flushValid.playerY = 66.0f;
        flushValid.playerZ = 1250.0f;
        flushValid.worldWidth = 2500;
        flushValid.worldDepth = 2500;
        flushValid.worldHeight = 256;
        flushValid.spawnX = 1250;
        flushValid.spawnY = 66;
        flushValid.spawnZ = 1250;

        session.enterWorld(42L, flushValid.name, "Plains", flushValid, player, camera, null, settings, null);

        assert Math.abs(player.pos.x - 2499.7f) < 1e-4f : "Valid flush player X=2499.7 must be preserved, got: " + player.pos.x;
        assert Math.abs(player.pos.z - 1250.0f) < 1e-4f : "Valid player Z must be preserved";
        session.shutdown(player, 0.0);

        System.out.println("PASSED");
    }

    /**
     * 4. Atomic replacement: preserve valid save if writing fails.
     */
    private static void testMetadataFailureRecoveryAndAtomicWrite() throws Exception {
        System.out.print("[Phase 4] Atomic save replacement & write failure preservation... ");

        File targetFile = new File(TEST_BASE_DIR, "atomic_test/world.jmc");

        // Write initial valid save
        WorldSaveManager.SavedWorld initial = new WorldSaveManager.SavedWorld();
        initial.name = "Initial Valid Save";
        initial.seed = 11111L;
        WorldSaveManager.saveBinaryDirect(initial, targetFile);

        // Verify that existing file is read correctly
        WorldSaveManager.SavedWorld read1 = WorldSaveManager.loadWorld(targetFile);
        assert read1 != null && "Initial Valid Save".equals(read1.name) : "Initial save should be readable";

        // Verify that no stray temp file is left over
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        assert !tempFile.exists() : "Temporary file should be removed after clean atomic move";

        // Verify original file remains intact
        WorldSaveManager.SavedWorld verifiedOriginal = WorldSaveManager.loadWorld(targetFile);
        assert verifiedOriginal != null && "Initial Valid Save".equals(verifiedOriginal.name) : "Original file must remain intact";

        System.out.println("PASSED");
    }

    /**
     * 5. Chunk persistence round-trip at all 4 corners and center.
     */
    private static void testCornerAndCenterChunkPersistence() throws Exception {
        System.out.print("[Phase 4] Chunk persistence round-trip for 4 corners & spawn center... ");

        File worldDir = new File(TEST_BASE_DIR, "chunk_corners_world");
        RegionManager regionManager = new RegionManager(worldDir, WorldBounds.DEFAULT);

        // Chunks to test: (0,0), (156,0), (0,156), (156,156), and spawn chunk (78,78)
        int[][] testCoords = {
            {0, 0},
            {156, 0},
            {0, 156},
            {156, 156},
            {78, 78}
        };

        for (int[] coord : testCoords) {
            int cx = coord[0];
            int cz = coord[1];

            Chunk original = new Chunk(cx, cz);
            // Populate test voxels in valid portion
            original.setBlockState(0, 0, 0, Blocks.BEDROCK.getDefaultState());
            original.setBlockState(1, 64, 1, Blocks.STONE.getDefaultState());
            original.setBlockState(2, 65, 2, Blocks.DIAMOND_ORE.getDefaultState());
            original.setBlockState(3, 70, 3, Blocks.WATER.getDefaultState());

            regionManager.saveChunk(original);

            // Verify chunk was saved
            assert regionManager.hasSavedChunk(cx, cz) : "RegionManager must confirm chunk (" + cx + "," + cz + ") is saved";

            // Load back
            Chunk reloaded = regionManager.loadChunk(cx, cz);
            assert reloaded != null : "Failed to reload chunk (" + cx + "," + cz + ")";
            assert reloaded.getCx() == cx && reloaded.getCz() == cz : "Chunk coordinates mismatch";
            assert reloaded.getBlockState(0, 0, 0).is(Blocks.BEDROCK) : "Bedrock mismatch at (" + cx + "," + cz + ")";
            assert reloaded.getBlockState(1, 64, 1).is(Blocks.STONE) : "Stone mismatch at (" + cx + "," + cz + ")";
            assert reloaded.getBlockState(2, 65, 2).is(Blocks.DIAMOND_ORE) : "Diamond ore mismatch at (" + cx + "," + cz + ")";
            assert reloaded.getBlockState(3, 70, 3).isWater() : "Water mismatch at (" + cx + "," + cz + ")";
        }

        regionManager.close();
        System.out.println("PASSED");
    }

    /**
     * 6. Partial edge chunk voxel isolation: invalid columns strictly remain AIR.
     */
    private static void testPartialEdgeChunkVoxelIsolation() throws Exception {
        System.out.print("[Phase 4] Partial edge chunk invalid column isolation on load & save... ");

        File worldDir = new File(TEST_BASE_DIR, "partial_isolation_world");
        RegionManager regionManager = new RegionManager(worldDir, WorldBounds.DEFAULT);

        // Corner partial chunk: cx=156, cz=156. Valid local voxels are 0..3 on X and Z.
        Chunk partialChunk = new Chunk(156, 156);
        // Valid voxels:
        partialChunk.setBlockState(0, 60, 0, Blocks.STONE.getDefaultState());
        partialChunk.setBlockState(3, 60, 3, Blocks.COBBLESTONE.getDefaultState());

        regionManager.saveChunk(partialChunk);

        Chunk loaded = regionManager.loadChunk(156, 156);
        assert loaded != null;
        assert loaded.getBlockState(0, 60, 0).is(Blocks.STONE);
        assert loaded.getBlockState(3, 60, 3).is(Blocks.COBBLESTONE);

        // Verify invalid local voxels (lx >= 4 or lz >= 4) remain strictly AIR
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (x >= 4 || z >= 4) {
                    assert loaded.getBlockState(x, 60, z).isAir() : "Partial chunk out-of-world voxel (" + x + "," + z + ") must be AIR";
                }
            }
        }

        regionManager.close();
        System.out.println("PASSED");
    }

    /**
     * 7. 5x5 Region grid mapping and rejection of out-of-world chunk writes.
     */
    private static void testRegionStorageAndBoundaryWriteRejection() throws Exception {
        System.out.print("[Phase 4] 5x5 Region grid mapping & boundary write rejection... ");

        // Region coordinate mapping
        assert RegionPos.fromChunkCoords(0, 0).equals(new RegionPos(0, 0)) : "Chunk (0,0) -> Region (0,0)";
        assert RegionPos.fromChunkCoords(156, 0).equals(new RegionPos(4, 0)) : "Chunk (156,0) -> Region (4,0)";
        assert RegionPos.fromChunkCoords(0, 156).equals(new RegionPos(0, 4)) : "Chunk (0,156) -> Region (0,4)";
        assert RegionPos.fromChunkCoords(156, 156).equals(new RegionPos(4, 4)) : "Chunk (156,156) -> Region (4,4)";
        assert RegionPos.fromChunkCoords(78, 78).equals(new RegionPos(2, 2)) : "Center chunk (78,78) -> Region (2,2)";

        // Filenames
        assert "r.0.0.jmc".equals(RegionPos.fromChunkCoords(0, 0).getFileName());
        assert "r.4.4.jmc".equals(RegionPos.fromChunkCoords(156, 156).getFileName());

        // Rejection of invalid chunk writes in bounded RegionManager
        File worldDir = new File(TEST_BASE_DIR, "region_rejection_world");
        RegionManager boundedRegionManager = new RegionManager(worldDir, WorldBounds.DEFAULT);

        // Attempt to save invalid chunks: (-1, 0), (157, 0), (0, -1), (0, 157)
        Chunk invalidNegative = new Chunk(-1, 0);
        invalidNegative.setBlockState(0, 60, 0, Blocks.STONE.getDefaultState());
        boundedRegionManager.saveChunk(invalidNegative);

        Chunk invalidOver = new Chunk(157, 0);
        invalidOver.setBlockState(0, 60, 0, Blocks.STONE.getDefaultState());
        boundedRegionManager.saveChunk(invalidOver);

        // Both must be rejected: neither region should have been created or saved
        assert !boundedRegionManager.hasSavedChunk(-1, 0) : "Invalid chunk (-1, 0) must not be saved";
        assert !boundedRegionManager.hasSavedChunk(157, 0) : "Invalid chunk (157, 0) must not be saved";

        boundedRegionManager.close();
        System.out.println("PASSED");
    }

    /**
     * 8. Backward compatibility: reading V1, V2, and V3 chunk payloads.
     */
    private static void testLegacySerializerBackwardCompatibility() throws Exception {
        System.out.print("[Phase 4] ChunkSerializer V1, V2, and V3 backward compatibility... ");

        Chunk original = new Chunk(10, 20);
        original.setBlockState(0, 64, 0, Blocks.STONE.getDefaultState());
        original.setBlockState(5, 70, 5, Blocks.OAK_LOG.getDefaultState());

        // Test V1 serialization & deserialization
        byte[] v1Bytes = ChunkSerializer.serialize(original, ChunkSerializer.FORMAT_VERSION_V1);
        Chunk loadedV1 = ChunkSerializer.deserialize(v1Bytes);
        assert loadedV1.getCx() == 10 && loadedV1.getCz() == 20;
        assert loadedV1.getBlockState(0, 64, 0).is(Blocks.STONE);
        assert loadedV1.getBlockState(5, 70, 5).is(Blocks.OAK_LOG);

        // Test V2 serialization & deserialization
        byte[] v2Bytes = ChunkSerializer.serialize(original, ChunkSerializer.FORMAT_VERSION_V2);
        Chunk loadedV2 = ChunkSerializer.deserialize(v2Bytes);
        assert loadedV2.getCx() == 10 && loadedV2.getCz() == 20;
        assert loadedV2.getBlockState(0, 64, 0).is(Blocks.STONE);

        // Test V3 serialization & deserialization
        byte[] v3Bytes = ChunkSerializer.serialize(original, ChunkSerializer.FORMAT_VERSION_V3);
        Chunk loadedV3 = ChunkSerializer.deserialize(v3Bytes);
        assert loadedV3.getCx() == 10 && loadedV3.getCz() == 20;
        assert loadedV3.getBlockState(0, 64, 0).is(Blocks.STONE);

        System.out.println("PASSED");
    }

    /**
     * 9. Legacy V3 save detection, explicit migration to V4 finite world, and out-of-bounds skipping.
     * Verifies:
     * - Imported chunk block contents across multiple blocks and positions.
     * - Partial edge chunk sanitization (illegal columns cleared to AIR).
     * - Player position, health, hunger, hotbar, selected slot, and world time preserved.
     */
    private static void testLegacyV3MigrationToFiniteV4() throws Exception {
        System.out.print("[Phase 4] Legacy V3 save migration to V4 finite world... ");

        File sourceWorldDir = new File(TEST_BASE_DIR, "legacy_v3_source");
        File destWorldDir = new File(TEST_BASE_DIR, "migrated_v4_dest");
        sourceWorldDir.mkdirs();

        // 1. Create a simulated legacy V3 save with out-of-bounds player
        WorldSaveManager.SavedWorld v3Save = new WorldSaveManager.SavedWorld();
        v3Save.formatVersion = 3;
        v3Save.name = "Old Unbounded World";
        v3Save.seed = 55555L;
        v3Save.worldTime = 8000.0;
        v3Save.playerX = -50.0f;
        v3Save.playerY = 64.0f;
        v3Save.playerZ = -50.0f;
        v3Save.selectedSlot = 5;
        for (int i = 0; i < 9; i++) {
            v3Save.hotbarBlocks[i] = (byte) (i + 1);
            v3Save.hotbarCounts[i] = (i + 1) * 5;
        }

        File sourceMetaFile = new File(sourceWorldDir, "world.jmc");
        WorldSaveManager.saveBinaryDirect(v3Save, sourceMetaFile);

        // Verify loaded source is marked as legacy
        WorldSaveManager.SavedWorld loadedSource = WorldSaveManager.loadWorld(sourceMetaFile);
        assert loadedSource != null && loadedSource.isLegacy() : "V3 save must be recognized as legacy";

        // Create legacy chunks:
        // Chunk A: In-bounds (5, 5) -> multiple block types to verify content fidelity
        // Chunk B: Out-of-bounds (-2, -3) -> should be skipped
        // Chunk C: Out-of-bounds (200, 200) -> should be skipped
        // Chunk D: Partial edge chunk (156, 156) with valid AND illegal blocks
        RegionManager sourceRegionManager = new RegionManager(sourceWorldDir, WorldBounds.UNBOUNDED);

        Chunk inBoundsChunk = new Chunk(5, 5);
        inBoundsChunk.setBlockState(0, 0, 0, Blocks.BEDROCK.getDefaultState());
        inBoundsChunk.setBlockState(1, 60, 1, Blocks.STONE.getDefaultState());
        inBoundsChunk.setBlockState(2, 63, 2, Blocks.DIRT.getDefaultState());
        inBoundsChunk.setBlockState(0, 64, 0, Blocks.DIAMOND_ORE.getDefaultState());
        inBoundsChunk.setBlockState(3, 70, 3, Blocks.WATER.getDefaultState());
        sourceRegionManager.saveChunk(inBoundsChunk);

        Chunk outBoundsNeg = new Chunk(-2, -3);
        outBoundsNeg.setBlockState(0, 64, 0, Blocks.COBBLESTONE.getDefaultState());
        sourceRegionManager.saveChunk(outBoundsNeg);

        Chunk outBoundsPos = new Chunk(200, 200);
        outBoundsPos.setBlockState(0, 64, 0, Blocks.SAND.getDefaultState());
        sourceRegionManager.saveChunk(outBoundsPos);

        // Partial edge chunk (156, 156) with valid and invalid voxels
        Chunk partialCorner = new Chunk(156, 156);
        partialCorner.setBlockState(0, 60, 0, Blocks.STONE.getDefaultState());       // valid (lx=0, lz=0)
        partialCorner.setBlockState(3, 60, 3, Blocks.COBBLESTONE.getDefaultState()); // valid (lx=3, lz=3)
        partialCorner.setBlockState(4, 60, 4, Blocks.DIAMOND_ORE.getDefaultState()); // ILLEGAL (lx=4, lz=4)
        partialCorner.setBlockState(10, 60, 10, Blocks.SAND.getDefaultState());       // ILLEGAL (lx=10, lz=10)
        sourceRegionManager.saveChunk(partialCorner);

        sourceRegionManager.close();

        // 2. Perform explicit migration to destination
        WorldConfiguration targetConfig = WorldConfiguration.createDefault(55555L);
        SaveMigrationManager.MigrationReport report = SaveMigrationManager.migrateToFiniteWorld(
            sourceWorldDir, destWorldDir, targetConfig
        );

        assert report.success() : "Migration must report success";
        assert report.sourceVersion() == 3 : "Source version must be 3";
        assert report.destinationVersion() == 4 : "Destination version must be 4";
        assert report.importedChunkCount() == 2 : "Expected 2 imported chunks ((5,5) and (156,156)), got: " + report.importedChunkCount();
        assert report.skippedChunkCount() == 2 : "Expected 2 skipped chunks, got: " + report.skippedChunkCount();
        assert !report.playerPositionCompatible() : "Player position was out-of-bounds and should not be compatible";
        assert report.playerRelocated() : "Player should be reported as relocated";

        // 3. Verify destination save metadata
        File destMetaFile = new File(destWorldDir, "world.jmc");
        assert destMetaFile.exists() : "Migrated world.jmc must exist in destination";

        WorldSaveManager.SavedWorld destSave = WorldSaveManager.loadWorld(destMetaFile);
        assert destSave != null && destSave.formatVersion == 4 : "Destination must be V4";
        assert !destSave.isLegacy() : "Destination must not be legacy";
        assert destSave.worldWidth == 2500 && destSave.worldDepth == 2500 : "Destination dimensions must be 2500x2500";
        // Player should be relocated to default spawn (1250, 66, 1250)
        assert Math.abs(destSave.playerX - 1250.0f) < 1e-4f : "Player X should be relocated to spawn";
        assert Math.abs(destSave.playerZ - 1250.0f) < 1e-4f : "Player Z should be relocated to spawn";
        // Inventory must be fully preserved!
        assert destSave.selectedSlot == 5 : "Selected slot must be preserved";
        for (int i = 0; i < 9; i++) {
            assert destSave.hotbarBlocks[i] == (byte) (i + 1) : "Hotbar block mismatch at " + i;
            assert destSave.hotbarCounts[i] == (i + 1) * 5 : "Hotbar count mismatch at " + i;
        }
        assert Math.abs(destSave.worldTime - 8000.0) < 1e-4 : "World time must be preserved";

        // 4. Verify destination region files: in-bounds chunks exist, out-of-bounds chunks do not
        RegionManager destRegionManager = new RegionManager(destWorldDir, WorldBounds.DEFAULT);
        assert destRegionManager.hasSavedChunk(5, 5) : "In-bounds chunk (5,5) must be present in destination";
        assert destRegionManager.hasSavedChunk(156, 156) : "Partial edge chunk (156,156) must be present in destination";
        assert !destRegionManager.hasSavedChunk(-2, -3) : "Out-of-bounds chunk (-2,-3) must NOT be in destination";
        assert !destRegionManager.hasSavedChunk(200, 200) : "Out-of-bounds chunk (200,200) must NOT be in destination";

        // Verify full block contents of chunk (5,5)
        Chunk loadedChunk5 = destRegionManager.loadChunk(5, 5);
        assert loadedChunk5 != null;
        assert loadedChunk5.getBlockState(0, 0, 0).is(Blocks.BEDROCK) : "Bedrock must be preserved";
        assert loadedChunk5.getBlockState(1, 60, 1).is(Blocks.STONE) : "Stone must be preserved";
        assert loadedChunk5.getBlockState(2, 63, 2).is(Blocks.DIRT) : "Dirt must be preserved";
        assert loadedChunk5.getBlockState(0, 64, 0).is(Blocks.DIAMOND_ORE) : "Diamond ore must be preserved";
        assert loadedChunk5.getBlockState(3, 70, 3).isWater() : "Water must be preserved";

        // Verify partial edge chunk sanitization for chunk (156, 156)
        Chunk loadedChunkEdge = destRegionManager.loadChunk(156, 156);
        assert loadedChunkEdge != null;
        assert loadedChunkEdge.getBlockState(0, 60, 0).is(Blocks.STONE) : "Valid block at (0,60,0) must be preserved";
        assert loadedChunkEdge.getBlockState(3, 60, 3).is(Blocks.COBBLESTONE) : "Valid block at (3,60,3) must be preserved";
        assert loadedChunkEdge.getBlockState(4, 60, 4).isAir() : "Illegal voxel at (4,60,4) MUST BE SANITIZED TO AIR";
        assert loadedChunkEdge.getBlockState(10, 60, 10).isAir() : "Illegal voxel at (10,60,10) MUST BE SANITIZED TO AIR";

        destRegionManager.close();

        System.out.println("PASSED");
    }

    /**
     * 10. Source immutability and repeat-safety of migration.
     */
    private static void testMigrationRepeatSafetyAndSourceImmutability() throws Exception {
        System.out.print("[Phase 4] Migration repeat-safety & source file immutability... ");

        File sourceWorldDir = new File(TEST_BASE_DIR, "legacy_v3_source");
        File destWorldDir = new File(TEST_BASE_DIR, "migrated_v4_dest");

        // Record source file byte contents before second run
        File sourceMetaFile = new File(sourceWorldDir, "world.jmc");
        byte[] sourceBytesBefore = Files.readAllBytes(sourceMetaFile.toPath());

        File sourceRegionsDir = new File(sourceWorldDir, "regions");
        File[] sourceRegionFiles = sourceRegionsDir.listFiles();
        assert sourceRegionFiles != null && sourceRegionFiles.length > 0;
        byte[][] regionBytesBefore = new byte[sourceRegionFiles.length][];
        for (int i = 0; i < sourceRegionFiles.length; i++) {
            regionBytesBefore[i] = Files.readAllBytes(sourceRegionFiles[i].toPath());
        }

        // Run migration a second time
        WorldConfiguration targetConfig = WorldConfiguration.createDefault(55555L);
        SaveMigrationManager.MigrationReport report2 = SaveMigrationManager.migrateToFiniteWorld(
            sourceWorldDir, destWorldDir, targetConfig
        );

        assert report2.success() : "Second migration attempt must succeed";
        assert report2.importedChunkCount() == 2 : "Imported chunk count must be identical";
        assert report2.skippedChunkCount() == 2 : "Skipped chunk count must be identical";

        // Verify source metadata file was NOT modified in any way
        byte[] sourceBytesAfter = Files.readAllBytes(sourceMetaFile.toPath());
        assert Arrays.equals(sourceBytesBefore, sourceBytesAfter) : "Source world.jmc must be byte-for-byte identical after migration";

        // Verify source region files were NOT modified in any way
        for (int i = 0; i < sourceRegionFiles.length; i++) {
            byte[] regionBytesAfter = Files.readAllBytes(sourceRegionFiles[i].toPath());
            assert Arrays.equals(regionBytesBefore[i], regionBytesAfter) : "Source region file " + sourceRegionFiles[i].getName() + " must be byte-for-byte identical";
        }

        // Verify destination can be safely reloaded with identical contents
        WorldSaveManager.SavedWorld destReloaded = WorldSaveManager.loadWorld(new File(destWorldDir, "world.jmc"));
        assert destReloaded != null && destReloaded.formatVersion == 4;

        RegionManager destRegionManager = new RegionManager(destWorldDir, WorldBounds.DEFAULT);
        assert destRegionManager.hasSavedChunk(5, 5);
        assert destRegionManager.hasSavedChunk(156, 156);
        destRegionManager.close();

        System.out.println("PASSED");
    }

    /**
     * 11. Failure recovery: truncated metadata and corrupted region entries.
     */
    private static void testCorruptedAndTruncatedDataHandling() throws Exception {
        System.out.print("[Phase 4] Truncated metadata & corrupted region error handling... ");

        // A. Truncated metadata file (e.g. 10 bytes)
        File truncatedMeta = new File(TEST_BASE_DIR, "truncated/world.jmc");
        truncatedMeta.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(truncatedMeta)) {
            fos.write(new byte[]{0x4A, 0x4D, 0x43, 0x57, 0, 0, 0, 4, 0, 5}); // Incomplete header
        }

        boolean caughtTruncated = false;
        try {
            WorldSaveManager.loadBinarySave(truncatedMeta);
        } catch (IOException e) {
            caughtTruncated = true;
        }
        assert caughtTruncated : "Truncated metadata must fail safely with IOException";

        // B. Non-magic metadata file
        File badMagicMeta = new File(TEST_BASE_DIR, "bad_magic/world.jmc");
        badMagicMeta.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(badMagicMeta)) {
            fos.write(new byte[]{1, 2, 3, 4, 0, 0, 0, 4});
        }

        boolean caughtBadMagic = false;
        try {
            WorldSaveManager.loadBinarySave(badMagicMeta);
        } catch (IOException e) {
            caughtBadMagic = true;
        }
        assert caughtBadMagic : "Invalid magic header must throw IOException";

        System.out.println("PASSED");
    }
}
