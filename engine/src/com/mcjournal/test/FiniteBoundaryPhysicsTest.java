package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.EngineConstants;
import com.mcjournal.FluidPhysicsManager;
import com.mcjournal.WorldBlockPos;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.ItemEntity;
import com.mcjournal.client.Raycast;
import com.mcjournal.client.RaycastHit;
import com.mcjournal.physics.CollisionDetector;
import com.mcjournal.world.WorldBounds;
import org.joml.Vector3f;

/**
 * Phase 3 Test Suite: Physical World Containment, Raycasting, and Boundary Systems.
 * Validates:
 * - Player physical AABB containment on all 4 boundaries (X=0, X=2500, Z=0, Z=2500) and corners.
 * - Anti-tunneling / high velocity containment.
 * - Smooth wall sliding along boundary planes and unobstructed departure.
 * - Jump and fall containment against boundary edges.
 * - Dropped item entity containment at perimeter.
 * - Raycast boundary termination (origins outside, rays traversing outside, edge stepping).
 * - Block interaction rejection and inventory preservation.
 * - Fluid simulation containment at world edges and partial chunk limits.
 * - Natural boundary hooks and EdgeZone classification.
 */
public class FiniteBoundaryPhysicsTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING PHASE 3: BOUNDARY & CONTAINMENT TESTS ");
        System.out.println("=================================================");

        testPlayerBoundaryContainment();
        testPlayerWallSlidingAndDeparture();
        testPlayerCornerContainment();
        testAntiTunnelingHighVelocity();
        testItemEntityContainment();
        testRaycastBoundaryTermination();
        testBlockInteractionAndInventoryProtection();
        testFluidSimulationBoundaryContainment();
        testNaturalBoundaryPresentationHooks();

        System.out.println(">>> ALL PHASE 3 BOUNDARY & CONTAINMENT TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    /**
     * 1. Player physical AABB containment on all 4 cardinal boundaries.
     */
    private static void testPlayerBoundaryContainment() {
        System.out.print("[Phase 3] Player AABB physical containment on all 4 boundaries... ");
        ChunkManager world = new ChunkManager(4, 12345L);
        CollisionDetector cd = new CollisionDetector();
        float w = 0.6f;
        float h = 1.8f;
        float halfW = w / 2.0f; // 0.3f
        float stepHeight = 0.6f;

        // West Boundary (X = 0): Attempt to move past X = 0
        Vector3f posWest = new Vector3f(1.0f, 64.0f, 100.0f);
        Vector3f velWest = new Vector3f(-2.0f, 0.0f, 0.0f); // Wants to reach -1.0f
        CollisionDetector.MovementResult resWest = cd.resolveMovement(world, posWest, velWest, w, h, stepHeight, false, false);
        assert Math.abs(resWest.resolvedPos().x - halfW) < 1e-5f : "Player center X must clamp to halfW at West boundary, got: " + resWest.resolvedPos().x;
        assert resWest.resolvedPos().x - halfW >= 0.0f : "Player min X must not be negative";
        assert resWest.collidedX() : "collidedX must be true when hitting West boundary";
        assert resWest.resolvedVel().x == 0.0f : "Velocity X must be zeroed when hitting West boundary";

        // East Boundary (X = 2500): Attempt to move past X = 2500
        Vector3f posEast = new Vector3f(2499.0f, 64.0f, 100.0f);
        Vector3f velEast = new Vector3f(3.0f, 0.0f, 0.0f); // Wants to reach 2502.0f
        CollisionDetector.MovementResult resEast = cd.resolveMovement(world, posEast, velEast, w, h, stepHeight, false, false);
        float maxAllowedX = 2500.0f - halfW;
        assert Math.abs(resEast.resolvedPos().x - maxAllowedX) < 1e-5f : "Player center X must clamp to maxAllowedX at East boundary, got: " + resEast.resolvedPos().x;
        assert resEast.resolvedPos().x + halfW <= 2500.0f : "Player max X must not exceed 2500.0";
        assert resEast.collidedX() : "collidedX must be true when hitting East boundary";
        assert resEast.resolvedVel().x == 0.0f : "Velocity X must be zeroed when hitting East boundary";

        // North Boundary (Z = 0): Attempt to move past Z = 0
        Vector3f posNorth = new Vector3f(100.0f, 64.0f, 1.0f);
        Vector3f velNorth = new Vector3f(0.0f, 0.0f, -2.0f);
        CollisionDetector.MovementResult resNorth = cd.resolveMovement(world, posNorth, velNorth, w, h, stepHeight, false, false);
        assert Math.abs(resNorth.resolvedPos().z - halfW) < 1e-5f : "Player center Z must clamp to halfW at North boundary, got: " + resNorth.resolvedPos().z;
        assert resNorth.collidedZ() : "collidedZ must be true when hitting North boundary";
        assert resNorth.resolvedVel().z == 0.0f : "Velocity Z must be zeroed";

        // South Boundary (Z = 2500): Attempt to move past Z = 2500
        Vector3f posSouth = new Vector3f(100.0f, 64.0f, 2499.0f);
        Vector3f velSouth = new Vector3f(0.0f, 0.0f, 2.5f);
        CollisionDetector.MovementResult resSouth = cd.resolveMovement(world, posSouth, velSouth, w, h, stepHeight, false, false);
        float maxAllowedZ = 2500.0f - halfW;
        assert Math.abs(resSouth.resolvedPos().z - maxAllowedZ) < 1e-5f : "Player center Z must clamp to maxAllowedZ at South boundary, got: " + resSouth.resolvedPos().z;
        assert resSouth.collidedZ() : "collidedZ must be true when hitting South boundary";
        assert resSouth.resolvedVel().z == 0.0f : "Velocity Z must be zeroed";

        System.out.println("PASSED");
    }

    /**
     * 2. Smooth wall sliding parallel to boundary planes and unconstrained departure.
     */
    private static void testPlayerWallSlidingAndDeparture() {
        System.out.print("[Phase 3] Wall sliding along boundary planes and departure... ");
        ChunkManager world = new ChunkManager(4, 12345L);
        CollisionDetector cd = new CollisionDetector();
        float w = 0.6f;
        float h = 1.8f;
        float halfW = w / 2.0f;
        float stepHeight = 0.6f;

        // At West wall (X = 0.3f), player presses into wall (vel.x = -1.0) while running South (vel.z = +2.0)
        Vector3f posAtWall = new Vector3f(halfW, 64.0f, 100.0f);
        Vector3f velSliding = new Vector3f(-1.0f, 0.0f, 2.0f);
        CollisionDetector.MovementResult resSlide = cd.resolveMovement(world, posAtWall, velSliding, w, h, stepHeight, false, false);
        assert Math.abs(resSlide.resolvedPos().x - halfW) < 1e-5f : "X must remain flush with West wall";
        assert Math.abs(resSlide.resolvedPos().z - 102.0f) < 1e-5f : "Z must freely slide forward to 102.0f, got: " + resSlide.resolvedPos().z;
        assert resSlide.collidedX() : "collidedX should be flagged";
        assert !resSlide.collidedZ() : "collidedZ should not be flagged during parallel sliding";

        // Player standing at West wall moves East (away from wall)
        Vector3f velDepart = new Vector3f(1.5f, 0.0f, 0.0f);
        CollisionDetector.MovementResult resDepart = cd.resolveMovement(world, posAtWall, velDepart, w, h, stepHeight, false, false);
        assert Math.abs(resDepart.resolvedPos().x - (halfW + 1.5f)) < 1e-5f : "Player should freely depart from wall to the East";
        assert !resDepart.collidedX() : "collidedX must be false when moving away from wall";

        System.out.println("PASSED");
    }

    /**
     * 3. Corner containment (simultaneous collision on both axes).
     */
    private static void testPlayerCornerContainment() {
        System.out.print("[Phase 3] Player corner containment at all 4 corners... ");
        ChunkManager world = new ChunkManager(4, 12345L);
        CollisionDetector cd = new CollisionDetector();
        float w = 0.6f;
        float h = 1.8f;
        float halfW = w / 2.0f;
        float stepHeight = 0.6f;
        float maxBound = 2500.0f - halfW;

        // Corner 1: (0, 0)
        Vector3f posNW = new Vector3f(1.0f, 64.0f, 1.0f);
        Vector3f velNW = new Vector3f(-5.0f, 0.0f, -5.0f);
        CollisionDetector.MovementResult resNW = cd.resolveMovement(world, posNW, velNW, w, h, stepHeight, false, false);
        assert Math.abs(resNW.resolvedPos().x - halfW) < 1e-5f : "Corner (0,0) X clamp failed";
        assert Math.abs(resNW.resolvedPos().z - halfW) < 1e-5f : "Corner (0,0) Z clamp failed";
        assert resNW.collidedX() && resNW.collidedZ() : "Both X and Z must report collision at corner";

        // Corner 2: (2500, 0)
        Vector3f posNE = new Vector3f(2499.0f, 64.0f, 1.0f);
        Vector3f velNE = new Vector3f(5.0f, 0.0f, -5.0f);
        CollisionDetector.MovementResult resNE = cd.resolveMovement(world, posNE, velNE, w, h, stepHeight, false, false);
        assert Math.abs(resNE.resolvedPos().x - maxBound) < 1e-5f : "Corner (2500,0) X clamp failed";
        assert Math.abs(resNE.resolvedPos().z - halfW) < 1e-5f : "Corner (2500,0) Z clamp failed";

        // Corner 3: (0, 2500)
        Vector3f posSW = new Vector3f(1.0f, 64.0f, 2499.0f);
        Vector3f velSW = new Vector3f(-5.0f, 0.0f, 5.0f);
        CollisionDetector.MovementResult resSW = cd.resolveMovement(world, posSW, velSW, w, h, stepHeight, false, false);
        assert Math.abs(resSW.resolvedPos().x - halfW) < 1e-5f : "Corner (0,2500) X clamp failed";
        assert Math.abs(resSW.resolvedPos().z - maxBound) < 1e-5f : "Corner (0,2500) Z clamp failed";

        // Corner 4: (2500, 2500)
        Vector3f posSE = new Vector3f(2499.0f, 64.0f, 2499.0f);
        Vector3f velSE = new Vector3f(5.0f, 0.0f, 5.0f);
        CollisionDetector.MovementResult resSE = cd.resolveMovement(world, posSE, velSE, w, h, stepHeight, false, false);
        assert Math.abs(resSE.resolvedPos().x - maxBound) < 1e-5f : "Corner (2500,2500) X clamp failed";
        assert Math.abs(resSE.resolvedPos().z - maxBound) < 1e-5f : "Corner (2500,2500) Z clamp failed";

        System.out.println("PASSED");
    }

    /**
     * 4. Anti-tunneling at high velocities (e.g., explosions or speed boosts).
     */
    private static void testAntiTunnelingHighVelocity() {
        System.out.print("[Phase 3] Anti-tunneling at extreme velocities (v = 500.0f)... ");
        ChunkManager world = new ChunkManager(4, 12345L);
        CollisionDetector cd = new CollisionDetector();
        float w = 0.6f;
        float h = 1.8f;
        float halfW = w / 2.0f;
        float stepHeight = 0.6f;

        Vector3f startPos = new Vector3f(100.0f, 64.0f, 100.0f);
        Vector3f extremeVel = new Vector3f(-500.0f, 0.0f, 0.0f); // Extreme speed toward X=0
        CollisionDetector.MovementResult res = cd.resolveMovement(world, startPos, extremeVel, w, h, stepHeight, false, false);

        assert Math.abs(res.resolvedPos().x - halfW) < 1e-5f : "Extreme velocity must not tunnel beyond West wall, got X=" + res.resolvedPos().x;
        assert res.collidedX() : "Extreme velocity must register collidedX";
        assert res.resolvedVel().x == 0.0f : "Extreme velocity must be zeroed";

        // Toward East boundary
        Vector3f extremeEast = new Vector3f(5000.0f, 0.0f, 0.0f);
        CollisionDetector.MovementResult resEast = cd.resolveMovement(world, startPos, extremeEast, w, h, stepHeight, false, false);
        float maxAllowedX = 2500.0f - halfW;
        assert Math.abs(resEast.resolvedPos().x - maxAllowedX) < 1e-5f : "Extreme velocity must not tunnel beyond East wall, got: " + resEast.resolvedPos().x;
        assert resEast.collidedX() : "Extreme velocity East must register collidedX";
        assert resEast.resolvedVel().x == 0.0f : "Extreme velocity East must be zeroed";

        System.out.println("PASSED");
    }

    /**
     * 5. Dropped item entity physical containment.
     */
    private static void testItemEntityContainment() {
        System.out.print("[Phase 3] Dropped item entity physical containment... ");
        ChunkManager world = new ChunkManager(4, 12345L);

        // Throw item toward West border
        ItemEntity itemWest = new ItemEntity((byte) 4, 1, 1.0f, 64.0f, 50.0f, -5.0f, 0.0f, 0.0f, 0.0f);
        itemWest.update(1.0f, world, null);

        float halfSize = ItemEntity.SIZE / 2.0f; // 0.14f
        assert itemWest.pos.x >= halfSize : "Item center X must not penetrate West boundary";
        assert Math.abs(itemWest.pos.x - halfSize) < 1e-4f : "Item should be clamped to border, got: " + itemWest.pos.x;
        assert itemWest.velocity.x == 0.0f : "Item velocity X should be zeroed upon border collision";

        // Throw item toward East border
        ItemEntity itemEast = new ItemEntity((byte) 4, 1, 2499.0f, 64.0f, 50.0f, 10.0f, 0.0f, 0.0f, 0.0f);
        itemEast.update(1.0f, world, null);

        float maxItemX = 2500.0f - halfSize;
        assert itemEast.pos.x <= maxItemX : "Item center X must not penetrate East boundary";
        assert Math.abs(itemEast.pos.x - maxItemX) < 1e-4f : "Item should clamp to East border";
        assert itemEast.velocity.x == 0.0f : "Item velocity X should be zeroed";

        System.out.println("PASSED");
    }

    /**
     * 6. Raycast boundary termination.
     */
    private static void testRaycastBoundaryTermination() {
        System.out.print("[Phase 3] Raycast DDA boundary termination... ");
        ChunkManager world = new ChunkManager(4, 12345L);

        // A. Ray starting completely outside world bounds returns null immediately
        Vector3f outsideOrigin = new Vector3f(-10.0f, 64.0f, 50.0f);
        Vector3f dirInward = new Vector3f(1.0f, 0.0f, 0.0f);
        RaycastHit resOut = Raycast.cast(world, outsideOrigin, dirInward, 20.0f);
        assert resOut == null : "Ray starting outside world bounds must return null immediately";

        // B. Ray starting inside world shooting outward terminates at boundary
        Vector3f insideNearEdge = new Vector3f(2.0f, 64.5f, 100.5f);
        Vector3f dirWest = new Vector3f(-1.0f, 0.0f, 0.0f);
        // Ensure no blocks in path
        RaycastHit resWest = Raycast.cast(world, insideNearEdge, dirWest, 10.0f);
        assert resWest == null : "Ray shooting out of world into air must terminate at boundary without error";

        // C. Outermost valid block hit vs out-of-bounds ray
        // Populate block at X=0, Y=64, Z=100
        Chunk chunk0 = new Chunk(0, 6);
        world.setChunk(new ChunkPos(0, 6), chunk0);
        world.setBlockStateAt(0, 64, 100, Blocks.STONE.getDefaultState());

        Vector3f insideLookingAtBorderBlock = new Vector3f(2.5f, 64.5f, 100.5f);
        RaycastHit hitBorderBlock = Raycast.cast(world, insideLookingAtBorderBlock, dirWest, 5.0f);
        assert hitBorderBlock != null : "Raycast should hit stone block at X=0";
        assert hitBorderBlock.getBlock().x() == 0 && hitBorderBlock.getBlock().y() == 64 && hitBorderBlock.getBlock().z() == 100 : "Hit pos must be (0, 64, 100)";

        // D. Ray starting from outermost valid air block looking further outward terminates safely
        Vector3f onOuterBlock = new Vector3f(0.5f, 65.5f, 100.5f);
        RaycastHit hitBeyond = Raycast.cast(world, onOuterBlock, dirWest, 5.0f);
        assert hitBeyond == null : "Ray stepping out to X=-1 must terminate immediately";

        System.out.println("PASSED");
    }

    /**
     * 7. Block interaction and inventory preservation.
     */
    private static void testBlockInteractionAndInventoryProtection() {
        System.out.print("[Phase 3] Block placement/break boundary rejection... ");
        ChunkManager world = new ChunkManager(4, 12345L);

        // Attempting to set block out of bounds
        boolean placedNegative = world.setBlockStateAt(-1, 64, 100, Blocks.DIRT.getDefaultState());
        assert !placedNegative : "setBlockStateAt at X=-1 must return false";

        boolean placedOverMax = world.setBlockStateAt(2500, 64, 100, Blocks.DIRT.getDefaultState());
        assert !placedOverMax : "setBlockStateAt at X=2500 must return false";

        boolean placedOverMaxZ = world.setBlockStateAt(100, 64, 2500, Blocks.DIRT.getDefaultState());
        assert !placedOverMaxZ : "setBlockStateAt at Z=2500 must return false";

        // Attempting to get block out of bounds returns AIR
        assert world.getBlockStateAt(-1, 64, 100).isAir() : "getBlockStateAt out of bounds must return AIR";
        assert world.getBlockStateAt(2500, 64, 100).isAir() : "getBlockStateAt out of bounds must return AIR";

        // Partial edge chunk invalid local voxel placement
        // Chunk cx=156, cz=156 only has valid lx=0..3, lz=0..3
        // Local x=4 corresponds to wx = 156*16 + 4 = 2500
        boolean placedPartialInvalid = world.setBlockStateAt(2500, 64, 2499, Blocks.COBBLESTONE.getDefaultState());
        assert !placedPartialInvalid : "setBlockStateAt at partial chunk invalid local voxel must return false";

        System.out.println("PASSED");
    }

    /**
     * 8. Fluid simulation containment at world boundaries and partial chunk boundaries.
     */
    private static void testFluidSimulationBoundaryContainment() {
        System.out.print("[Phase 3] Fluid simulation containment at world edges... ");
        ChunkManager world = new ChunkManager(4, 12345L);
        FluidPhysicsManager fluid = new FluidPhysicsManager();

        // Load chunk (0,0) and fill with air
        Chunk chunk00 = new Chunk(0, 0);
        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    chunk00.setBlockState(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        world.setChunk(new ChunkPos(0, 0), chunk00);

        // Put a solid floor at Y=60 so water spreads horizontally
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                chunk00.setBlockState(x, 60, z, Blocks.STONE.getDefaultState());
            }
        }

        // Place water source at (0, 61, 5) right on the West edge (wx=0)
        chunk00.setBlockState(0, 61, 5, Blocks.WATER.getDefaultState());
        fluid.scheduleUpdate(0, 61, 5, 0, 1);

        // Run simulation ticks
        fluid.tick(world);
        fluid.tick(world);

        // Verify water spread inside valid world (e.g. X=1, Z=5)
        assert world.getBlockStateAt(1, 61, 5).isWater() : "Water should spread east into valid world at (1, 61, 5)";

        // Verify water never queried or modified out of bounds (X=-1)
        assert world.getBlockStateAt(-1, 61, 5).isAir() : "Water must never spread out of bounds to X=-1";

        // Verify partial edge chunk fluid containment
        // Chunk (156, 156), valid wx=2496..2499, valid wz=2496..2499
        Chunk edgeChunk = new Chunk(156, 156);
        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    edgeChunk.setBlockState(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        for (int z = 0; z < 4; z++) {
            for (int x = 0; x < 4; x++) {
                edgeChunk.setBlockState(x, 60, z, Blocks.STONE.getDefaultState());
            }
        }
        world.setChunk(new ChunkPos(156, 156), edgeChunk);

        // Place water at valid edge voxel (wx=2499, wy=61, wz=2497) -> lx=3, lz=1
        edgeChunk.setBlockState(3, 61, 1, Blocks.WATER.getDefaultState());
        fluid.scheduleUpdate(2499, 61, 2497, 0, 1);

        fluid.tick(world);
        fluid.tick(world);

        // Local x=4 (wx=2500) must remain AIR and not have water
        assert edgeChunk.getBlockState(4, 61, 1).isAir() : "Fluid must not spread into invalid partial chunk local voxels";

        // Reset singleton frame metrics after fluid tests
        com.mcjournal.client.EngineMetrics.getInstance().endFrame();

        System.out.println("PASSED");
    }

    /**
     * 9. Natural boundary presentation hooks and EdgeZone classification.
     */
    private static void testNaturalBoundaryPresentationHooks() {
        System.out.print("[Phase 3] EdgeZone classification and boundary hooks... ");
        WorldBounds bounds = WorldBounds.DEFAULT;

        // Core zone: center of world (1250, 1250) -> distance is 1250 blocks (> 200)
        assert bounds.getEdgeZone(1250, 1250) == WorldBounds.EdgeZone.CORE : "Center must be CORE";

        // Approach zone: distance 150 blocks from West edge (150, 1250)
        assert bounds.getEdgeZone(150, 1250) == WorldBounds.EdgeZone.APPROACH : "Distance 150 must be APPROACH";

        // Perimeter zone: distance 50 blocks from East edge (2450, 1250) -> distance is 2499 - 2450 = 49
        assert bounds.getEdgeZone(2450, 1250) == WorldBounds.EdgeZone.PERIMETER : "Distance 49 must be PERIMETER";

        // Border zone: distance 10 blocks from North edge (1250, 10)
        assert bounds.getEdgeZone(1250, 10) == WorldBounds.EdgeZone.BORDER : "Distance 10 must be BORDER";

        // Exactly on edge (0, 0)
        assert bounds.getEdgeZone(0, 0) == WorldBounds.EdgeZone.BORDER : "Corner (0,0) must be BORDER";

        // Unbounded world always returns CORE
        assert WorldBounds.UNBOUNDED.getEdgeZone(0, 0) == WorldBounds.EdgeZone.CORE : "UNBOUNDED must always return CORE";

        System.out.println("PASSED");
    }
}
