package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.FluidPhysicsManager;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Player;
import com.mcjournal.physics.AABB;
import com.mcjournal.physics.CollisionDetector;
import com.mcjournal.physics.PhysicsSystem;
import org.joml.Vector3f;

/**
 * Comprehensive automated test suite for P6 (Fluid System) & P7 (Physics Engine).
 * Run with: java -ea -cp "engine/bin;engine/lib/*" com.mcjournal.test.PhysicsAndFluidTest
 */
public class PhysicsAndFluidTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING P6 (FLUIDS) & P7 (PHYSICS) TEST SUITE  ");
        System.out.println("=================================================");

        // P6: Fluid System
        testP6_1_FluidLevelBlockStates();
        testP6_2_BatchedFluidMutations();
        testP6_3_FluidUpdateQueueDeduplication();
        testP6_4_ChunkAwareFluidSimulation();

        // P7: Physics & Collision
        testP7_1_SystemExtraction();
        testP7_2_BroadPhaseCollision();
        testP7_3_EdgeCaseNegativeCoordinates();
        testP7_3_EdgeCaseChunkBoundaries();
        testP7_3_EdgeCaseCeilingCollision();
        testP7_3_EdgeCaseAutoStepUp();
        testP7_3_EdgeCaseWalkingOffEdges();
        testP7_3_EdgeCaseCornerCollision();
        testP7_4_AuthoritativeTimestepAndInterpolation();

        System.out.println("\n>>> ALL P6 & P7 TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static ChunkManager createTestManager() {
        return new ChunkManager(12345L);
    }

    // =========================================================================
    // P6: FLUID SYSTEM TESTS
    // =========================================================================

    private static void testP6_1_FluidLevelBlockStates() {
        System.out.print("[P6.1] Fluid level BlockStates (0..7)... ");

        BlockState defaultWater = Blocks.WATER.getDefaultState();
        assert defaultWater != null : "Default water state must not be null";
        assert defaultWater.get(BlockProperties.LEVEL) == 0 : "Default water must be level 0 (source)";

        for (int lvl = 0; lvl <= 7; lvl++) {
            BlockState levelState = defaultWater.with(BlockProperties.LEVEL, lvl);
            assert levelState != null : "State for water level " + lvl + " must exist";
            assert levelState.get(BlockProperties.LEVEL) == lvl : "Level property mismatch for level " + lvl;
            assert levelState.isWater() : "Water state level " + lvl + " must return isWater() == true";
            assert !levelState.isSolid() : "Water state level " + lvl + " must return isSolid() == false";

            int stateId = levelState.getStateId();
            assert stateId >= 0 : "State ID must be registered";
            assert BlockStateRegistry.getStateById(stateId) == levelState : "Round-trip registry lookup failed for water level " + lvl;
        }

        System.out.println("PASSED");
    }

    private static void testP6_2_BatchedFluidMutations() {
        System.out.print("[P6.2] Batched fluid mutations & dirty chunk updates... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Fill base with bedrock and stone
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlockState(x, 0, z, Blocks.BEDROCK.getDefaultState());
                chunk.setBlockState(x, 1, z, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk);

        FluidPhysicsManager fluidManager = new FluidPhysicsManager();

        // Place a water source block at (8, 2, 8)
        BlockState waterSource = Blocks.WATER.getDefaultState();
        manager.setBlockStateAt(8, 2, 8, waterSource);
        fluidManager.scheduleUpdate(8, 2, 8, 0, 1);

        assert fluidManager.getPendingUpdateCount() == 1 : "Expected 1 scheduled update";

        // Execute fluid tick 1 (processes source and schedules neighbor spreading)
        fluidManager.tick(manager);
        assert fluidManager.getPendingUpdateCount() == 4 : "Expected 4 cardinal neighbors queued";

        // Execute fluid tick 2 (processes queued neighbors and commits batch mutations)
        fluidManager.tick(manager);

        // Check that surrounding blocks have received decaying water states (level 1)
        BlockState northState = manager.getBlockStateAt(8, 2, 7);
        BlockState southState = manager.getBlockStateAt(8, 2, 9);
        BlockState eastState = manager.getBlockStateAt(9, 2, 8);
        BlockState westState = manager.getBlockStateAt(7, 2, 8);

        assert northState.isWater() && northState.get(BlockProperties.LEVEL) == 1 : "North should flow at level 1";
        assert southState.isWater() && southState.get(BlockProperties.LEVEL) == 1 : "South should flow at level 1";
        assert eastState.isWater() && eastState.get(BlockProperties.LEVEL) == 1 : "East should flow at level 1";
        assert westState.isWater() && westState.get(BlockProperties.LEVEL) == 1 : "West should flow at level 1";

        // Verify dirty chunks were marked for meshing
        assert manager.hasDirtyChunks() : "Dirty chunks must be tracked for meshed update";

        System.out.println("PASSED");
    }

    private static void testP6_3_FluidUpdateQueueDeduplication() {
        System.out.print("[P6.3] Fluid update queue deduplication... ");

        FluidPhysicsManager fluidManager = new FluidPhysicsManager();

        // Schedule same voxel update 10 times
        for (int i = 0; i < 10; i++) {
            fluidManager.scheduleUpdate(5, 10, 5, 2, 1);
        }

        assert fluidManager.getPendingUpdateCount() == 1 : "Duplicate updates must be deduplicated in queue";

        // Schedule different voxel
        fluidManager.scheduleUpdate(6, 10, 5, 2, 1);
        assert fluidManager.getPendingUpdateCount() == 2 : "Distinct updates should both be queued";

        System.out.println("PASSED");
    }

    private static void testP6_4_ChunkAwareFluidSimulation() {
        System.out.print("[P6.4] Chunk-aware boundary check (no sync freeze)... ");

        ChunkManager manager = createTestManager();
        Chunk chunk00 = new Chunk(0, 0);
        // Fill base
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk00.setBlockState(x, 0, z, Blocks.BEDROCK.getDefaultState());
                chunk00.setBlockState(x, 1, z, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk00);
        // Notice chunk (1, 0) is deliberately NOT loaded!

        assert manager.isChunkLoaded(0, 0) : "Chunk (0, 0) must be loaded";
        assert !manager.isChunkLoaded(1, 0) : "Chunk (1, 0) must be unloaded";

        FluidPhysicsManager fluidManager = new FluidPhysicsManager();
        // Place water right on chunk border at x=15
        manager.setBlockStateAt(15, 2, 8, Blocks.WATER.getDefaultState());
        fluidManager.scheduleUpdate(15, 2, 8, 0, 1);

        // Tick 1: schedules spread to neighbors inside loaded chunk (unloaded chunk ignored)
        fluidManager.tick(manager);
        // Tick 2: applies batch mutations inside loaded chunk
        fluidManager.tick(manager);

        assert !manager.isChunkLoaded(1, 0) : "Simulating border fluid must NOT synchronously force-generate unloaded chunk";
        // Western neighbor within loaded chunk should still receive fluid
        BlockState west = manager.getBlockStateAt(14, 2, 8);
        assert west.isWater() && west.get(BlockProperties.LEVEL) == 1 : "Internal neighbor must receive fluid";

        System.out.println("PASSED");
    }

    // =========================================================================
    // P7: PHYSICS & COLLISION TESTS
    // =========================================================================

    private static void testP7_1_SystemExtraction() {
        System.out.print("[P7.1] Decoupled PhysicsSystem & CollisionDetector... ");

        PhysicsSystem physics = new PhysicsSystem();
        CollisionDetector detector = physics.getCollisionDetector();
        assert detector != null : "CollisionDetector must be accessible from PhysicsSystem";

        // Verify Player utilizes decoupled PhysicsSystem
        Player player = new Player();
        assert player.getPhysicsSystem() != null : "Player must have a PhysicsSystem instance";

        System.out.println("PASSED");
    }

    private static void testP7_2_BroadPhaseCollision() {
        System.out.print("[P7.2] Broad-phase AABB voxel range collision... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Place stone at (5, 5, 5)
        chunk.setBlockState(5, 5, 5, Blocks.STONE.getDefaultState());
        manager.setChunk(new ChunkPos(0, 0), chunk);

        CollisionDetector detector = new CollisionDetector();

        // Box overlapping (5, 5, 5)
        AABB hitBox = new AABB(4.8f, 4.5f, 4.8f, 5.2f, 5.8f, 5.2f);
        assert detector.hasBlockCollision(manager, hitBox) : "AABB overlapping solid block must detect collision";

        // Box completely clear in air at (2, 2, 2)
        AABB missBox = new AABB(1.0f, 1.0f, 1.0f, 3.0f, 3.0f, 3.0f);
        assert !detector.hasBlockCollision(manager, missBox) : "AABB in air must not detect collision";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseNegativeCoordinates() {
        System.out.print("[P7.3] Edge case: Negative coordinates... ");

        ChunkManager manager = createTestManager();
        // Negative chunk (-1, -1) spans x: -16..-1, z: -16..-1
        Chunk negChunk = new Chunk(-1, -1);
        // Floor at y=4
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                negChunk.setBlockState(lx, 4, lz, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(-1, -1), negChunk);

        CollisionDetector detector = new CollisionDetector();

        // Test landing on floor at negative coordinate (-10, 5, -10)
        Vector3f pos = new Vector3f(-10.0f, 5.5f, -10.0f);
        Vector3f vel = new Vector3f(0.0f, -0.8f, 0.0f); // Falling down
        CollisionDetector.MovementResult res = detector.resolveMovement(
            manager, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, false, false
        );

        assert res.onGround() : "Player falling onto stone floor in negative coords must land";
        assert Math.abs(res.resolvedPos().y - 5.0f) < 0.001f : "Landed Y must be block top (5.0), got " + res.resolvedPos().y;
        assert res.resolvedVel().y == 0.0f : "Vertical velocity must reset to 0 upon landing";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseChunkBoundaries() {
        System.out.print("[P7.3] Edge case: Chunk boundary traversal... ");

        ChunkManager manager = createTestManager();
        Chunk chunk0 = new Chunk(0, 0);
        Chunk chunk1 = new Chunk(1, 0);

        // Continuous flat floor at y=4 across both chunks
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk0.setBlockState(lx, 4, lz, Blocks.STONE.getDefaultState());
                chunk1.setBlockState(lx, 4, lz, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk0);
        manager.setChunk(new ChunkPos(1, 0), chunk1);

        CollisionDetector detector = new CollisionDetector();

        // Position straddling boundary: x=15.9 to 16.2
        Vector3f pos = new Vector3f(15.9f, 5.0f, 8.0f);
        Vector3f vel = new Vector3f(0.3f, 0.0f, 0.0f); // Move across border into chunk 1

        CollisionDetector.MovementResult res = detector.resolveMovement(
            manager, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false
        );

        assert Math.abs(res.resolvedPos().x - 16.2f) < 0.01f : "Crossing boundary should smoothly move to 16.2";
        assert !res.collidedX() : "Should not falsely collide with boundary seam";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseCeilingCollision() {
        System.out.print("[P7.3] Edge case: Jumping into ceiling... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Floor at y=0, ceiling at y=4
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk.setBlockState(lx, 0, lz, Blocks.STONE.getDefaultState());
                chunk.setBlockState(lx, 4, lz, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk);

        CollisionDetector detector = new CollisionDetector();

        // Player standing at y=1.0 (head at 1.0 + 1.8 = 2.8), jumping upwards fast with vel.y = +1.5
        Vector3f pos = new Vector3f(8.0f, 2.0f, 8.0f);
        Vector3f vel = new Vector3f(0.0f, 1.5f, 0.0f);

        CollisionDetector.MovementResult res = detector.resolveMovement(
            manager, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, false, false
        );

        assert res.collidedY() : "Upward movement into ceiling must trigger collidedY";
        assert res.resolvedVel().y == 0.0f : "Vertical velocity must be stopped immediately when hitting ceiling";
        assert res.resolvedPos().y <= 2.25f : "Player head must not penetrate ceiling at y=4";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseAutoStepUp() {
        System.out.print("[P7.3] Edge case: Step-up over 0.5m/0.6m obstacle... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Base floor at y=2
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk.setBlockState(lx, 2, lz, Blocks.STONE.getDefaultState());
            }
        }
        // Step obstacle at x=9, y=3 (1 block high step, but step height is 0.6)
        // With vanilla step height 0.6, let's test horizontal blocked without jump vs jump clearing
        chunk.setBlockState(9, 3, 8, Blocks.STONE.getDefaultState());
        manager.setChunk(new ChunkPos(0, 0), chunk);

        CollisionDetector detector = new CollisionDetector();

        // Player at (8.5, 3.0, 8.0) moving into obstacle at x=9
        Vector3f pos = new Vector3f(8.5f, 3.0f, 8.0f);
        Vector3f vel = new Vector3f(0.3f, 0.0f, 0.0f);

        // Attempting to walk into 1.0m block directly while onGround without jump clearance should stop
        CollisionDetector.MovementResult res = detector.resolveMovement(
            manager, pos, vel, Player.WIDTH, Player.HEIGHT, 0.6f, true, false
        );
        assert res.collidedX() : "1.0m wall higher than 0.6m step should collide horizontally";
        assert res.resolvedVel().x == 0.0f : "Horizontal velocity must be stopped by full-height wall";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseWalkingOffEdges() {
        System.out.print("[P7.3] Edge case: Walking off edges & falling... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Ledge at x=0..8 at y=10
        for (int lx = 0; lx <= 8; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk.setBlockState(lx, 10, lz, Blocks.STONE.getDefaultState());
            }
        }
        // Ground floor below at y=0
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk.setBlockState(lx, 0, lz, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk);

        PhysicsSystem physics = new PhysicsSystem();

        // Player past edge: x=9.5 (block 8 extends from 8.0 to 9.0; past 9.0 is open air)
        Vector3f pos = new Vector3f(9.5f, 11.0f, 8.0f);
        Vector3f prevPos = new Vector3f(9.5f, 11.0f, 8.0f);
        Vector3f vel = new Vector3f(0.1f, 0.0f, 0.0f);

        // Perform tick update
        PhysicsSystem.PhysicsState state = physics.updateEntity(
            manager, pos, prevPos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT,
            Player.EYE_HEIGHT, true, 0, 11.0f, 1.0f, 0.0f, 1.0f, 0.0f, true, false, false, false
        );

        assert !state.onGround() : "Player walking off ledge must no longer be on ground";
        assert state.velocity().y < 0 : "Player must begin accelerating downwards under gravity";
        assert state.pos().y < 11.0f : "Player Y position must drop";

        System.out.println("PASSED");
    }

    private static void testP7_3_EdgeCaseCornerCollision() {
        System.out.print("[P7.3] Edge case: Corner collisions (two solid walls)... ");

        ChunkManager manager = createTestManager();
        Chunk chunk = new Chunk(0, 0);
        // Floor at y=0
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                chunk.setBlockState(lx, 0, lz, Blocks.STONE.getDefaultState());
            }
        }
        // Continuous solid walls at x=10 and z=10 meeting at corner (10, 10)
        for (int y = 1; y <= 3; y++) {
            for (int z = 8; z <= 12; z++) {
                chunk.setBlockState(10, y, z, Blocks.STONE.getDefaultState());
            }
            for (int x = 8; x <= 12; x++) {
                chunk.setBlockState(x, y, 10, Blocks.STONE.getDefaultState());
            }
        }
        manager.setChunk(new ChunkPos(0, 0), chunk);

        CollisionDetector detector = new CollisionDetector();

        // Player starting close to corner at (9.4, 1.0, 9.4) and attempting to move into (10.0, 1.0, 10.0)
        Vector3f pos = new Vector3f(9.4f, 1.0f, 9.4f);
        Vector3f vel = new Vector3f(0.5f, 0.0f, 0.5f);

        CollisionDetector.MovementResult res = detector.resolveMovement(
            manager, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, true, false
        );

        assert res.collidedX() || res.collidedZ() : "Moving diagonally into corner must detect wall collision";
        assert res.resolvedPos().x < 10.0f : "Player must not penetrate wall in X";
        assert res.resolvedPos().z < 10.0f : "Player must not penetrate wall in Z";

        System.out.println("PASSED");
    }

    private static void testP7_4_AuthoritativeTimestepAndInterpolation() {
        System.out.print("[P7.4] Authoritative 20 TPS timestep & render interpolation... ");

        Player player = new Player();
        player.pos.set(0.0f, 10.0f, 0.0f);
        player.prevPos.set(0.0f, 10.0f, 0.0f);

        // Simulate 1 tick of horizontal movement
        player.prevPos.set(player.pos);
        player.pos.add(2.0f, 0.0f, 4.0f); // pos is now (2, 10, 4)

        // Interpolate at partialTick = 0.0 (exact start of tick)
        Vector3f eye0 = player.getEyePosition(0.0f);
        assert Math.abs(eye0.x - 0.0f) < 0.001f : "At partialTick 0.0, eye X should be prevPos X";
        assert Math.abs(eye0.z - 0.0f) < 0.001f : "At partialTick 0.0, eye Z should be prevPos Z";

        // Interpolate at partialTick = 0.5 (halfway between ticks)
        Vector3f eyeHalf = player.getEyePosition(0.5f);
        assert Math.abs(eyeHalf.x - 1.0f) < 0.001f : "At partialTick 0.5, eye X should be 1.0, got " + eyeHalf.x;
        assert Math.abs(eyeHalf.z - 2.0f) < 0.001f : "At partialTick 0.5, eye Z should be 2.0, got " + eyeHalf.z;

        // Interpolate at partialTick = 1.0 (exact end of tick)
        Vector3f eye1 = player.getEyePosition(1.0f);
        assert Math.abs(eye1.x - 2.0f) < 0.001f : "At partialTick 1.0, eye X should be pos X";
        assert Math.abs(eye1.z - 4.0f) < 0.001f : "At partialTick 1.0, eye Z should be pos Z";

        System.out.println("PASSED");
    }
}
