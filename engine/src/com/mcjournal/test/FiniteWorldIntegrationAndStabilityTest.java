package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.*;
import com.mcjournal.physics.AABB;
import com.mcjournal.physics.CollisionDetector;
import com.mcjournal.physics.PhysicsSystem;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;
import com.mcjournal.world.biome.BiomeRegistry;
import com.mcjournal.world.structure.*;
import com.mcjournal.world.terrain.PlateauModifier;
import com.mcjournal.world.terrain.SpawnLocator;
import com.mcjournal.world.terrain.TerrainModifierPipeline;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phase 7 Master Integration & Stability Test Suite.
 * Final verification of the complete finite world implementation across all systems:
 * 1. Comprehensive Boundary & Corner Matrix (physical containment, raycast, block edit, fluid, mesh vertices).
 * 2. Coordinate & Indexing Invariant Audit (floorDiv/floorMod, region grid, partial chunks).
 * 3. Complete World Lifecycle & Persistence Round-Trip (create, spawn, edit, save, shutdown, reopen, restore).
 * 4. Concurrency, Race Condition & Streaming Stress Audit.
 * 5. Sustained Streaming & Resource Stability Benchmark.
 */
public class FiniteWorldIntegrationAndStabilityTest {

    private static final File TEST_WORLD_DIR = new File("saves/test_phase7_integration");

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("    RUNNING PHASE 7: FINITE WORLD FINAL INTEGRATION & STABILITY AUDIT           ");
        System.out.println("===============================================================================");

        cleanupDir(TEST_WORLD_DIR);

        try {
            testBoundaryAndCornerMatrix();
            testCoordinateAndIndexingInvariants();
            testWorldLifecycleAndPersistenceRoundTrip();
            testConcurrencyAndStreamingStress();
            testSustainedStreamingAndResourceStability();
            testPhase71RiskVerification();

            System.out.println("\n>>> ALL PHASE 7 INTEGRATION & STABILITY AUDIT TESTS PASSED! <<<\n");
        } catch (Throwable t) {
            System.err.println("❌ PHASE 7 AUDIT FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            cleanupDir(TEST_WORLD_DIR);
        }
    }

    // =========================================================================
    // 1. BOUNDARY & CORNER MATRIX
    // =========================================================================

    private static void testBoundaryAndCornerMatrix() {
        System.out.print("[Phase 7] Comprehensive boundary & corner matrix (containment, mesh, fluids, raycast)... ");

        WorldBounds bounds = WorldBounds.DEFAULT;
        CollisionDetector collisionDetector = new CollisionDetector();
        ChunkManager world = new ChunkManager(123456L);

        // A. Player AABB Physical Containment at all 4 edges and 4 corners
        float halfW = Player.WIDTH / 2.0f; // 0.3f
        float pHeight = Player.HEIGHT;     // 1.8f

        // West edge (X = 0)
        Vector3f posWest = new Vector3f(1.0f, 65.0f, 1250.0f);
        Vector3f velWest = new Vector3f(-50.0f, 0.0f, 0.0f); // High velocity towards West
        CollisionDetector.MovementResult resWest = collisionDetector.resolveMovement(world, posWest, velWest, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false);
        assert Math.abs(resWest.resolvedPos().x - halfW) < 1e-4 : "West containment failed: x=" + resWest.resolvedPos().x;
        assert resWest.collidedX() : "West collision must be flagged";

        // East edge (X = 2500)
        Vector3f posEast = new Vector3f(2499.0f, 65.0f, 1250.0f);
        Vector3f velEast = new Vector3f(50.0f, 0.0f, 0.0f);
        CollisionDetector.MovementResult resEast = collisionDetector.resolveMovement(world, posEast, velEast, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false);
        float expectedMaxX = 2500.0f - halfW;
        assert Math.abs(resEast.resolvedPos().x - expectedMaxX) < 1e-4 : "East containment failed: x=" + resEast.resolvedPos().x;
        assert resEast.collidedX();

        // North edge (Z = 0)
        Vector3f posNorth = new Vector3f(1250.0f, 65.0f, 1.0f);
        Vector3f velNorth = new Vector3f(0.0f, 0.0f, -50.0f);
        CollisionDetector.MovementResult resNorth = collisionDetector.resolveMovement(world, posNorth, velNorth, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false);
        assert Math.abs(resNorth.resolvedPos().z - halfW) < 1e-4;
        assert resNorth.collidedZ();

        // South edge (Z = 2500)
        Vector3f posSouth = new Vector3f(1250.0f, 65.0f, 2499.0f);
        Vector3f velSouth = new Vector3f(0.0f, 0.0f, 50.0f);
        CollisionDetector.MovementResult resSouth = collisionDetector.resolveMovement(world, posSouth, velSouth, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false);
        float expectedMaxZ = 2500.0f - halfW;
        assert Math.abs(resSouth.resolvedPos().z - expectedMaxZ) < 1e-4;
        assert resSouth.collidedZ();

        // All 4 Corners (simultaneous X and Z boundary impact)
        int[][] cornerCoords = {{0, 0}, {2499, 0}, {0, 2499}, {2499, 2499}};
        float[][] cornerVels = {{-20f, -20f}, {20f, -20f}, {-20f, 20f}, {20f, 20f}};

        for (int i = 0; i < 4; i++) {
            Vector3f p = new Vector3f(cornerCoords[i][0], 65.0f, cornerCoords[i][1]);
            Vector3f v = new Vector3f(cornerVels[i][0], 0.0f, cornerVels[i][1]);
            CollisionDetector.MovementResult res = collisionDetector.resolveMovement(world, p, v, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false);
            assert bounds.isPlayerAABBInsideWorld(res.resolvedPos().x, res.resolvedPos().y, res.resolvedPos().z) :
                    "Player AABB breached bounds at corner " + i + ": " + res.resolvedPos();
            assert res.collidedX() && res.collidedZ() : "Corner collision must flag both X and Z";
        }

        // Vertical Ceiling & Floor Containment
        Vector3f posCeil = new Vector3f(1250.0f, 253.0f, 1250.0f);
        Vector3f velCeil = new Vector3f(0.0f, 50.0f, 0.0f); // High upward impulse
        CollisionDetector.MovementResult resCeil = collisionDetector.resolveMovement(world, posCeil, velCeil, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, false, false);
        float expectedMaxY = 256.0f - pHeight;
        assert Math.abs(resCeil.resolvedPos().y - expectedMaxY) < 1e-4 : "Ceiling breach: y=" + resCeil.resolvedPos().y;
        assert resCeil.collidedY() : "Ceiling collision must be flagged";

        // B. ItemEntity Containment at Edges & Ceiling
        ItemEntity item = new ItemEntity((byte) 1, 1, 2499.5f, 254.5f, 2499.5f, 20f, 20f, 20f, 0f);
        item.update(0.05f, world, null);
        assert item.pos.x <= 2500.0f - ItemEntity.SIZE / 2.0f;
        assert item.pos.z <= 2500.0f - ItemEntity.SIZE / 2.0f;
        assert item.pos.y <= 256.0f - ItemEntity.SIZE;

        // C. Raycast Boundary Termination
        // Ray starting outside world returns null
        Vector3f rayOriginOut = new Vector3f(2505.0f, 65.0f, 1250.0f);
        Vector3f rayDirIn = new Vector3f(-1.0f, 0.0f, 0.0f);
        assert Raycast.cast(world, rayOriginOut, rayDirIn, 10.0f) == null;

        // Ray shooting from inside towards boundary without obstacles terminates at boundary
        Vector3f rayOriginIn = new Vector3f(2498.0f, 150.0f, 1250.0f);
        Vector3f rayDirOut = new Vector3f(1.0f, 0.0f, 0.0f);
        assert Raycast.cast(world, rayOriginIn, rayDirOut, 10.0f) == null;

        // D. Block Placement / Breaking at Last Valid Coordinate
        world.setChunk(new ChunkPos(0, 0), world.getChunkGenerator().generateChunk(new ChunkPos(0, 0)));
        world.setChunk(new ChunkPos(156, 156), world.getChunkGenerator().generateChunk(new ChunkPos(156, 156)));

        assert world.setBlockStateAt(0, 65, 0, Blocks.STONE.getDefaultState());
        assert world.setBlockStateAt(2499, 65, 2499, Blocks.STONE.getDefaultState());
        assert !world.setBlockStateAt(2500, 65, 1250, Blocks.STONE.getDefaultState()) : "Must reject X=2500";
        assert !world.setBlockStateAt(-1, 65, 1250, Blocks.STONE.getDefaultState()) : "Must reject X=-1";
        assert !world.setBlockStateAt(1250, 256, 1250, Blocks.STONE.getDefaultState()) : "Must reject Y=256";
        assert !world.setBlockStateAt(1250, -1, 1250, Blocks.STONE.getDefaultState()) : "Must reject Y=-1";

        // Partial chunk (156, 156): lx=3 (wx=2499) valid, lx=4 (wx=2500) invalid
        assert world.setBlockStateAt(2499, 65, 2499, Blocks.COBBLESTONE.getDefaultState());
        assert world.getBlockStateAt(2499, 65, 2499).is(Blocks.COBBLESTONE);
        assert !world.setBlockStateAt(2500, 65, 2500, Blocks.COBBLESTONE.getDefaultState());

        // E. Fluid Containment at Boundaries
        FluidPhysicsManager fluids = new FluidPhysicsManager();
        fluids.onBlockChanged(world, null, null, 2499, 65, 1250);
        // Ensure queue does not accept out-of-world coordinates
        fluids.queueFluidUpdate(2500, 65, 1250, 1);
        fluids.tick(world);
        assert world.getBlockStateAt(2500, 65, 1250).isAir();

        // F. Meshing at All 4 Corners & Vertex Coordinate Inspection
        ChunkPos[] cornerChunks = {
            new ChunkPos(0, 0),
            new ChunkPos(156, 0),
            new ChunkPos(0, 156),
            new ChunkPos(156, 156)
        };

        for (ChunkPos cpos : cornerChunks) {
            Chunk c = world.getGenerator().generateChunk(cpos.x(), cpos.z());
            ChunkNeighborhood neighborhood = ChunkNeighborhood.of(c, world);
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborhood);

            // Inspect solid vertices (stride 13: pos(0..2))
            float[] verts = mesh.solidVertices;
            for (int v = 0; v < verts.length; v += 13) {
                float vx = verts[v];
                float vy = verts[v + 1];
                float vz = verts[v + 2];

                assert vx >= 0.0f && vx <= 2500.0f : "Vertex X outside world bounds in chunk " + cpos + ": " + vx;
                assert vy >= 0.0f && vy <= 256.0f : "Vertex Y outside world bounds in chunk " + cpos + ": " + vy;
                assert vz >= 0.0f && vz <= 2500.0f : "Vertex Z outside world bounds in chunk " + cpos + ": " + vz;
            }

            // In partial chunk (156, 156), assert valid bounds
            if (cpos.x() == 156 && cpos.z() == 156) {
                for (int v = 0; v < verts.length; v += 13) {
                    float vx = verts[v];
                    float vz = verts[v + 2];
                    assert vx <= 2500.0f && vz <= 2500.0f;
                }
            }
        }

        world.shutdown();
        System.out.println("PASSED");
    }

    // =========================================================================
    // 2. COORDINATE & INDEXING INVARIANT AUDIT
    // =========================================================================

    private static void testCoordinateAndIndexingInvariants() {
        System.out.print("[Phase 7] Coordinate & indexing invariants (floorDiv, floorMod, regions, partial chunks)... ");

        WorldBounds bounds = WorldBounds.DEFAULT;

        // 1. Horizontal Block Coordinate to Chunk Coordinate round-trips
        for (int wx : new int[]{0, 1, 15, 16, 17, 2495, 2496, 2499}) {
            int cx = Math.floorDiv(wx, 16);
            int lx = Math.floorMod(wx, 16);
            assert cx * 16 + lx == wx;
            assert bounds.isValidHorizontal(wx, 0);
            assert bounds.isValidChunk(cx, 0);
        }

        // Negative block coordinates: floorDiv(-1, 16) = -1, floorMod(-1, 16) = 15
        assert Math.floorDiv(-1, 16) == -1;
        assert Math.floorMod(-1, 16) == 15;
        assert !bounds.isValidHorizontal(-1, 0);
        assert !bounds.isValidChunk(-1, 0);

        // World border overflow
        assert Math.floorDiv(2500, 16) == 156;
        assert Math.floorMod(2500, 16) == 4;
        assert !bounds.isValidHorizontal(2500, 0);
        assert bounds.isValidChunk(156, 0); // Chunk 156 exists, but local block 4 is invalid
        assert !bounds.isLocalBlockValid(156, 0, 4, 64, 0);

        // 2. Region coordinate mapping across the entire 157x157 chunk grid
        int totalChunks = 0;
        for (int cz = 0; cz < 157; cz++) {
            for (int cx = 0; cx < 157; cx++) {
                RegionPos rpos = RegionPos.fromChunkCoords(cx, cz);
                assert rpos.rx() >= 0 && rpos.rx() <= 4 : "Region rx out of 5x5 grid: " + rpos.rx();
                assert rpos.rz() >= 0 && rpos.rz() <= 4 : "Region rz out of 5x5 grid: " + rpos.rz();
                int lcx = rpos.getLocalChunkX(cx);
                int lcz = rpos.getLocalChunkZ(cz);
                assert lcx >= 0 && lcx < 32;
                assert lcz >= 0 && lcz < 32;
                totalChunks++;
            }
        }
        assert totalChunks == 157 * 157 : "Total chunk count mismatch";

        // Region boundary chunks:
        // Chunk (0, 0) -> Region (0, 0), local (0, 0)
        RegionPos r0 = RegionPos.fromChunkCoords(0, 0);
        assert r0.rx() == 0 && r0.rz() == 0 && r0.getLocalChunkX(0) == 0 && r0.getLocalChunkZ(0) == 0;

        // Chunk (31, 31) -> Region (0, 0), local (31, 31)
        RegionPos r31 = RegionPos.fromChunkCoords(31, 31);
        assert r31.rx() == 0 && r31.rz() == 0 && r31.getLocalChunkX(31) == 31 && r31.getLocalChunkZ(31) == 31;

        // Chunk (32, 32) -> Region (1, 1), local (0, 0)
        RegionPos r32 = RegionPos.fromChunkCoords(32, 32);
        assert r32.rx() == 1 && r32.rz() == 1 && r32.getLocalChunkX(32) == 0 && r32.getLocalChunkZ(32) == 0;

        // Chunk (156, 156) -> Region (4, 4), local (28, 28)
        RegionPos r156 = RegionPos.fromChunkCoords(156, 156);
        assert r156.rx() == 4 && r156.rz() == 4 && r156.getLocalChunkX(156) == 28 && r156.getLocalChunkZ(156) == 28;

        System.out.println("PASSED");
    }

    // =========================================================================
    // 3. COMPLETE WORLD LIFECYCLE & PERSISTENCE ROUND-TRIP
    // =========================================================================

    private static void testWorldLifecycleAndPersistenceRoundTrip() throws Exception {
        System.out.print("[Phase 7] Complete world lifecycle & persistence round-trip... ");

        File saveDir = new File(TEST_WORLD_DIR, "lifecycle_world");
        cleanupDir(saveDir);
        saveDir.mkdirs();

        long seed = 87654321L;
        String worldName = "LifecycleTest";

        // Step 1: Create world session and locate safe spawn
        WorldSession session1 = new WorldSession();
        Player player1 = new Player();
        Camera camera1 = new Camera();
        GameSettings settings = new GameSettings();

        session1.enterWorld(seed, worldName, "Plains", null, player1, camera1, null, settings, null);

        // Verify world dimensions and configuration
        WorldBounds bounds = session1.getChunkManager().getWorldBounds();
        assert bounds.getWidth() == 2500 && bounds.getDepth() == 2500 && bounds.getHeight() == 256;

        // Verify spawn position has clearance and solid ground
        float spawnX = player1.pos.x;
        float spawnY = player1.pos.y;
        float spawnZ = player1.pos.z;
        assert bounds.isPlayerAABBInsideWorld(spawnX, spawnY, spawnZ);

        // Step 2: Modify blocks in spawn chunk
        int modX = (int) Math.floor(spawnX);
        int modY = (int) Math.floor(spawnY) - 1; // ground block
        int modZ = (int) Math.floor(spawnZ);
        session1.getChunkManager().setBlockStateAt(modX, modY, modZ, Blocks.DIAMOND_ORE.getDefaultState());

        // Step 3: Modify blocks in partial edge chunk (156, 156)
        session1.getChunkManager().setBlockStateAt(2499, 70, 2499, Blocks.SAND.getDefaultState());
        // Verify out-of-world coordinate is strictly rejected
        assert !session1.getChunkManager().setBlockStateAt(2500, 70, 2500, Blocks.SAND.getDefaultState());

        // Step 4: Save and shut down world
        session1.shutdown(player1, 7500.0);

        // Step 5: Verify metadata V4 exists on disk
        WorldSaveManager.SavedWorld loadedSave = WorldSaveManager.loadWorld();
        assert loadedSave != null : "World save metadata must load successfully";
        assert loadedSave.formatVersion == 4;
        assert loadedSave.worldWidth == 2500;
        assert loadedSave.worldDepth == 2500;
        assert loadedSave.seed == seed;

        // Step 6: Reopen world in new session
        WorldSession session2 = new WorldSession();
        Player player2 = new Player();
        Camera camera2 = new Camera();

        session2.enterWorld(seed, worldName, "Plains", loadedSave, player2, camera2, null, settings, null);

        // Step 7: Verify player position and stats restored
        assert Math.abs(player2.pos.x - spawnX) < 1e-3;
        assert Math.abs(player2.pos.y - spawnY) < 1e-3;
        assert Math.abs(player2.pos.z - spawnZ) < 1e-3;

        // Step 8: Verify modified blocks were restored and not regenerated
        BlockState restoredGround = session2.getChunkManager().getBlockStateAt(modX, modY, modZ);
        assert restoredGround.is(Blocks.DIAMOND_ORE) : "Mutated spawn block must persist across restart";

        // Partial edge chunk (156, 156) is outside render distance of spawn, load from disk persistence to verify
        Chunk cornerChunk = session2.getChunkManager().getChunkGenerator().generateChunk(new ChunkPos(156, 156));
        assert cornerChunk != null;
        assert cornerChunk.getBlockState(3, 70, 3).is(Blocks.SAND) : "Mutated partial edge chunk block must persist across restart";

        // Step 9: Continue playing and save again
        session2.getChunkManager().setBlockStateAt(modX, modY, modZ, Blocks.OAK_LOG.getDefaultState());
        session2.shutdown(player2, 8000.0);

        WorldSaveManager.SavedWorld reloadedSave = WorldSaveManager.loadWorld();
        assert reloadedSave != null;
        assert reloadedSave.formatVersion == 4;

        cleanupDir(saveDir);
        System.out.println("PASSED");
    }

    // =========================================================================
    // 4. CONCURRENCY & STREAMING STRESS AUDIT
    // =========================================================================

    private static void testConcurrencyAndStreamingStress() throws Exception {
        System.out.print("[Phase 7] Concurrency, race condition & streaming stress audit... ");

        long seed = 998877L;
        ChunkManager manager = new ChunkManager(12, seed);

        // 1. Concurrent requests for the same chunk across 8 threads
        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Future<Chunk>> sameChunkFutures = new ArrayList<>();
        ChunkPos targetPos = new ChunkPos(45, 45);

        for (int i = 0; i < 20; i++) {
            sameChunkFutures.add(exec.submit(() -> manager.getChunkGenerator().generateChunk(targetPos)));
        }

        Chunk baseline = sameChunkFutures.get(0).get();
        for (Future<Chunk> f : sameChunkFutures) {
            Chunk c = f.get();
            assert c != null;
            short[] s1 = baseline.getBlockStates();
            short[] s2 = c.getBlockStates();
            for (int j = 0; j < s1.length; j++) {
                assert s1[j] == s2[j] : "Race condition in identical chunk generation";
            }
        }

        // 2. Concurrent requests for adjacent chunks in 3x3 grid
        List<Future<Chunk>> adjacentFutures = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = 45 + dx;
                int cz = 45 + dz;
                adjacentFutures.add(exec.submit(() -> manager.getChunkGenerator().generateChunk(new ChunkPos(cx, cz))));
            }
        }
        for (Future<Chunk> f : adjacentFutures) {
            assert f.get() != null;
        }

        // 3. Repeated Load / Unload Cycles (50 iterations)
        for (int i = 0; i < 50; i++) {
            int cx = 10 + (i % 5);
            int cz = 10 + (i / 5);
            Chunk c = manager.getChunkGenerator().generateChunk(new ChunkPos(cx, cz));
            manager.setChunk(new ChunkPos(cx, cz), c);
            assert manager.isChunkLoaded(cx, cz);
            manager.setChunk(new ChunkPos(cx, cz), null);
            assert !manager.isChunkLoaded(cx, cz);
        }

        exec.shutdown();
        manager.shutdown();
        System.out.println("PASSED");
    }

    // =========================================================================
    // 5. SUSTAINED STREAMING & RESOURCE STABILITY BENCHMARK
    // =========================================================================

    private static void testSustainedStreamingAndResourceStability() {
        System.out.print("[Phase 7] Sustained world streaming & memory stability benchmark... ");

        long seed = 55443322L;
        ChunkManager manager = new ChunkManager(6, seed); // Render distance 6 (13x13 grid)

        // Force GC before measuring memory baseline
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Simulate sustained player movement traversing across the world diagonally from (10, 10) to (40, 40)
        int stepCount = 30;
        long totalGenNanos = 0;

        for (int step = 0; step < stepCount; step++) {
            int playerCx = 10 + step;
            int playerCz = 10 + step;

            long t0 = System.nanoTime();
            manager.updatePlayerPosition(playerCx, playerCz, 1.0f, 1.0f);

            // Drain GPU unloads and uploads on each game frame
            manager.processGpuUnloads(null);
            manager.processGpuUploads(null);
            totalGenNanos += (System.nanoTime() - t0);
        }

        // Allow any background worker threads to finish and flush queues
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        manager.processGpuUnloads(null);
        manager.processGpuUploads(null);

        // Memory stability check
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        System.gc();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double memDeltaMB = (memAfter - memBefore) / (1024.0 * 1024.0);

        // Assert memory growth remains stable and does not leak unbounded
        assert memDeltaMB < 250.0 : "Excessive memory growth detected during streaming: " + memDeltaMB + " MB";

        // Assert loaded chunk count at any given time does not exceed maximum allowable bounds
        int activeChunks = manager.getLoadedChunkCount();
        int maxAllowedChunks = (int) Math.pow(manager.getStreamer().getUnloadDistance() * 2 + 1, 2);
        assert activeChunks <= maxAllowedChunks : "Loaded chunk count exceeded bounds: " + activeChunks + " > " + maxAllowedChunks;

        double avgMsPerStep = (totalGenNanos / 1_000_000.0) / stepCount;
        manager.shutdown();

        System.out.println(String.format("PASSED (Active Chunks: %d | Mem Delta: %+.1f MB | Avg Step: %.2f ms)",
                activeChunks, memDeltaMB, avgMsPerStep));
    }

    // =========================================================================
    // 6. PHASE 7.1 RISK VERIFICATION (SHUTDOWN, UNLOADED MUTATIONS, ITEM DIMS, HEADLESS)
    // =========================================================================

    private static void testPhase71RiskVerification() {
        System.out.println("\n--- RUNNING PHASE 7.1 FINAL RISK VERIFICATION ---");
        testChunkWorkerShutdownWithDelayedTask();
        testUnloadedChunkMutationsAndMemoryBounds();
        testItemEntityDimensionsAndContainmentPrecision();
        testHeadlessGpuUnloadResourceRelease();
    }

    private static void testChunkWorkerShutdownWithDelayedTask() {
        System.out.print("[Phase 7.1] Chunk worker shutdown with delayed task & interrupt safety... ");
        File worldDir = new File(TEST_WORLD_DIR, "shutdown_risk_test");
        cleanupDir(worldDir);
        worldDir.mkdirs();

        ChunkManager manager = new ChunkManager(4, 99999L, worldDir, WorldBounds.DEFAULT);
        AtomicBoolean taskStarted = new AtomicBoolean(false);
        AtomicBoolean taskInterruptedOrCompleted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Submit deliberately delayed task
        manager.submitWorkerTask(() -> {
            taskStarted.set(true);
            latch.countDown();
            try {
                Thread.sleep(10000); // 10 seconds
            } catch (InterruptedException e) {
                taskInterruptedOrCompleted.set(true);
                Thread.currentThread().interrupt();
                return;
            }
            taskInterruptedOrCompleted.set(true);
        });

        try {
            boolean started = latch.await(2, TimeUnit.SECONDS);
            assert started : "Worker task failed to start within timeout";
        } catch (InterruptedException e) {
            throw new AssertionError("Latch wait interrupted", e);
        }

        // Now initiate shutdown
        long t0 = System.currentTimeMillis();
        manager.shutdown();
        long elapsed = System.currentTimeMillis() - t0;

        // Verify workers pool is fully terminated
        assert manager.getWorkerExecutorForTest().isTerminated() : "Worker executor must be terminated";
        assert taskInterruptedOrCompleted.get() : "Delayed worker task must have been interrupted or completed";
        // Verify persistence closed safely without hanging indefinitely
        assert elapsed < 5000 : "Shutdown took too long: " + elapsed + " ms";

        System.out.println("PASSED (Terminated in " + elapsed + " ms)");
    }

    private static void testUnloadedChunkMutationsAndMemoryBounds() {
        System.out.print("[Phase 7.1] Unloaded chunk mutations, delta integrity & streaming memory bounds... ");
        File worldDir = new File(TEST_WORLD_DIR, "unloaded_mutations_test");
        cleanupDir(worldDir);
        worldDir.mkdirs();

        ChunkManager manager = new ChunkManager(2, 424242L, worldDir, WorldBounds.DEFAULT);
        int spawnCx = 78; // Center chunk (1250 / 16)
        int spawnCz = 78;
        manager.waitForInitialChunks(spawnCx, spawnCz, 1);
        int initialLoaded = manager.getLoadedChunkCount();

        // 1. Out-of-world mutation rejection
        assert !manager.setBlockStateAt(-1, 64, 50, Blocks.STONE.getDefaultState()) : "Out-of-world negative X must be rejected";
        assert !manager.setBlockStateAt(2500, 64, 50, Blocks.STONE.getDefaultState()) : "Out-of-world X=2500 must be rejected";
        assert !manager.setBlockStateAt(100, 256, 100, Blocks.STONE.getDefaultState()) : "Out-of-world Y=256 must be rejected";
        assert manager.getLoadedChunkCount() == initialLoaded : "Rejected mutations must not instantiate chunks";

        // 2. Mutate distant unloaded chunk (10, 10)
        int distantWx = 10 * 16 + 5;
        int distantWz = 10 * 16 + 5;
        boolean mutated = manager.setBlockStateAt(distantWx, 75, distantWz, Blocks.DIAMOND_ORE.getDefaultState());
        assert mutated : "Valid unloaded chunk mutation must succeed";
        Chunk distantChunk = manager.getChunk(10, 10);
        assert distantChunk != null : "Mutated chunk must be instantiated";
        assert distantChunk.getBlockState(5, 75, 5).is(Blocks.DIAMOND_ORE) : "Voxel state must match mutated block";
        assert distantChunk.isDirty() : "Chunk must be marked dirty";

        // 3. Repeated mutation must reuse existing instance without duplicate objects
        int countBeforeSecondEdit = manager.getLoadedChunkCount();
        manager.setBlockStateAt(distantWx, 76, distantWz, Blocks.COBBLESTONE.getDefaultState());
        assert manager.getLoadedChunkCount() == countBeforeSecondEdit : "Repeated edit must not duplicate chunk instance";
        assert manager.getChunk(10, 10) == distantChunk : "Must reuse identical chunk reference";

        // 4. Repeated edits across 20 distant chunks must not bypass streaming memory limits
        for (int i = 0; i < 20; i++) {
            manager.setBlockStateAt(i * 16 + 2, 70, 2 * 16 + 2, Blocks.SAND.getDefaultState());
        }
        assert manager.getLoadedChunkCount() > initialLoaded : "Distant chunks instantiated in memory";

        // When streaming updates around spawn, excess distant chunks must be queued for unload
        manager.updatePlayerPosition(spawnCx, spawnCz);
        int unloadsProcessed = manager.processGpuUnloads(null);
        assert unloadsProcessed >= 20 : "Distant chunks must be unloaded by streaming";
        assert manager.getLoadedChunkCount() <= initialLoaded : "Active chunks in memory must return to bounded streaming footprint";

        // Verify the unloaded mutations were persisted to disk
        manager.shutdown();

        // Reload world and verify distant block states persisted on disk
        ChunkManager reloaded = new ChunkManager(2, 424242L, worldDir, WorldBounds.DEFAULT);
        Chunk loadedDistant = reloaded.getChunkGenerator().generateChunk(new ChunkPos(10, 10));
        assert loadedDistant.getBlockState(5, 75, 5).is(Blocks.DIAMOND_ORE) : "Mutated block 1 must persist across save/reload";
        assert loadedDistant.getBlockState(5, 76, 5).is(Blocks.COBBLESTONE) : "Mutated block 2 must persist across save/reload";
        for (int i = 0; i < 20; i++) {
            Chunk c = reloaded.getChunkGenerator().generateChunk(new ChunkPos(i, 2));
            assert c.getBlockState(2, 70, 2).is(Blocks.SAND) : "Batch mutated chunk " + i + " must persist on disk";
        }
        reloaded.shutdown();

        System.out.println("PASSED");
    }

    private static void testItemEntityDimensionsAndContainmentPrecision() {
        System.out.print("[Phase 7.1] ItemEntity dimensions (0.28), AABB containment & velocity zeroing... ");
        assert ItemEntity.SIZE == 0.28f : "ItemEntity.SIZE must be strictly 0.28f";
        float halfSize = ItemEntity.SIZE / 2.0f;
        assert Math.abs(halfSize - 0.14f) < 1e-6f : "Half size must be strictly 0.14f";

        WorldBounds bounds = WorldBounds.DEFAULT;
        ChunkManager world = new ChunkManager(123456L);

        // Ceiling test: item propelled upward into ceiling
        ItemEntity ceilingItem = new ItemEntity((byte) 1, 1, 100.0f, 255.5f, 100.0f, 0.0f, 15.0f, 0.0f, 0.0f);
        ceilingItem.update(0.05f, world, null);
        assert ceilingItem.pos.y <= 256.0f - ItemEntity.SIZE : "Item top must not exceed 256.0f";
        assert ceilingItem.velocity.y == 0.0f : "Ceiling collision must zero vertical velocity";

        // Floor test: item falling below Y=0
        ItemEntity floorItem = new ItemEntity((byte) 1, 1, 100.0f, 0.1f, 100.0f, 0.0f, -10.0f, 0.0f, 0.0f);
        floorItem.update(0.05f, world, null);
        assert floorItem.pos.y >= 0.0f : "Item bottom must not penetrate below 0.0f";
        assert floorItem.velocity.y == 0.0f : "Floor collision must zero vertical velocity";
        assert floorItem.onGround : "Item on floor must have onGround = true";

        // Horizontal boundary tests
        ItemEntity westItem = new ItemEntity((byte) 1, 1, 0.2f, 64.0f, 100.0f, -10.0f, 0.0f, 0.0f, 0.0f);
        westItem.update(0.05f, world, null);
        assert westItem.pos.x >= halfSize : "West item must be clamped to halfSize";
        assert westItem.velocity.x == 0.0f : "West boundary collision must zero velocity";

        ItemEntity eastItem = new ItemEntity((byte) 1, 1, 2499.8f, 64.0f, 100.0f, 10.0f, 0.0f, 0.0f, 0.0f);
        eastItem.update(0.05f, world, null);
        assert eastItem.pos.x <= 2500.0f - halfSize : "East item must be clamped to width - halfSize";
        assert eastItem.velocity.x == 0.0f : "East boundary collision must zero velocity";

        System.out.println("PASSED");
    }

    private static void testHeadlessGpuUnloadResourceRelease() {
        System.out.print("[Phase 7.1] Headless GPU unload draining & CPU resource release... ");
        ChunkManager manager = new ChunkManager(2, 55555L, null, WorldBounds.DEFAULT);
        manager.waitForInitialChunks(10, 10, 1);
        int initialCount = manager.getLoadedChunkCount();
        assert initialCount > 0 : "Initial chunks must be generated";

        // Queue all initial chunks for unload
        for (ChunkPos pos : new ArrayList<>(manager.getAllChunks().stream().map(c -> new ChunkPos(c.getCx(), c.getCz())).toList())) {
            manager.getUploadQueue().queueUnload(pos);
        }

        assert manager.getUploadQueue().hasPendingUnloads() : "Unload queue must have pending items";

        // In headless mode (renderer == null), processGpuUnloads must drain safely and release CPU references
        int unloads = manager.processGpuUnloads(null);
        assert unloads == initialCount : "All pending unloads must be processed";
        assert !manager.getUploadQueue().hasPendingUnloads() : "Upload queue must be empty";
        assert manager.getLoadedChunkCount() == 0 : "All CPU chunk references must be released";
        assert manager.getAllMeshes().isEmpty() : "All CPU mesh references must be released";

        manager.shutdown();
        System.out.println("PASSED");
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
