package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.WorldBlockPos;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Direction;
import com.mcjournal.client.Raycast;
import com.mcjournal.client.RaycastHit;
import org.joml.Vector3f;

/**
 * Comprehensive DDA Voxel Raycast Test Suite (P8.1 & P8.2).
 * Run with: java -ea -cp "engine/bin;engine/lib/*" com.mcjournal.test.RaycastDDATest
 */
public class RaycastDDATest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("         RUNNING P8 (RAYCAST DDA) TESTS          ");
        System.out.println("=================================================");

        testAxisAlignedPositiveX();
        testAxisAlignedNegativeX();
        testAxisAlignedPositiveZ();
        testAxisAlignedNegativeZ();
        testVerticalY();
        testNegativeWorldCoordinates();
        testBoundaryOrigins();
        testExactVoxelCorners();
        testZeroDirectionVector();
        testRaycastHitObjectIntegrity();

        System.out.println("\n>>> ALL P8 RAYCAST DDA TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static ChunkManager createTestWorld() {
        return new ChunkManager(12345L);
    }

    private static void testAxisAlignedPositiveX() {
        System.out.print("[P8.1] Axis-aligned +X raycast... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        // Place stone obstacle at (5, 10, 5)
        chunk.setBlockState(5, 10, 5, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        // Ray origin at (1.5, 10.5, 5.5) pointing in pure +X direction (1, 0, 0)
        Vector3f origin = new Vector3f(1.5f, 10.5f, 5.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Ray along +X should hit stone block at (5, 10, 5)";
        assert hit.bx == 5 && hit.by == 10 && hit.bz == 5 : "Hit voxel coordinate mismatch";
        assert hit.getFace() == Direction.WEST : "Hit face must be WEST (normal (-1, 0, 0))";
        assert hit.normalX == -1 && hit.normalY == 0 && hit.normalZ == 0;
        assert Math.abs(hit.getHitPosition().x - 5.0f) < 0.001f : "Hit surface X must be exactly 5.0";
        assert Math.abs(hit.getDistance() - 3.5f) < 0.001f : "Distance should be 5.0 - 1.5 = 3.5";

        System.out.println("PASSED");
    }

    private static void testAxisAlignedNegativeX() {
        System.out.print("[P8.1] Axis-aligned -X raycast... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(2, 10, 5, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        // Ray origin at (6.5, 10.5, 5.5) pointing in pure -X direction (-1, 0, 0)
        Vector3f origin = new Vector3f(6.5f, 10.5f, 5.5f);
        Vector3f dir = new Vector3f(-1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Ray along -X should hit stone block at (2, 10, 5)";
        assert hit.bx == 2 && hit.by == 10 && hit.bz == 5;
        assert hit.getFace() == Direction.EAST : "Hit face must be EAST (normal (+1, 0, 0))";
        assert hit.normalX == 1 && hit.normalY == 0 && hit.normalZ == 0;
        assert Math.abs(hit.getHitPosition().x - 3.0f) < 0.001f : "Hit surface X should be 3.0 (east face of block 2)";
        assert Math.abs(hit.getDistance() - 3.5f) < 0.001f : "Distance should be 6.5 - 3.0 = 3.5";

        System.out.println("PASSED");
    }

    private static void testAxisAlignedPositiveZ() {
        System.out.print("[P8.1] Axis-aligned +Z raycast... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(5, 10, 8, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        Vector3f origin = new Vector3f(5.5f, 10.5f, 2.0f);
        Vector3f dir = new Vector3f(0.0f, 0.0f, 1.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Ray along +Z should hit block at (5, 10, 8)";
        assert hit.bx == 5 && hit.by == 10 && hit.bz == 8;
        assert hit.getFace() == Direction.NORTH : "Hit face must be NORTH (normal (0, 0, -1))";
        assert hit.normalZ == -1;
        assert Math.abs(hit.getHitPosition().z - 8.0f) < 0.001f;

        System.out.println("PASSED");
    }

    private static void testAxisAlignedNegativeZ() {
        System.out.print("[P8.1] Axis-aligned -Z raycast... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(5, 10, 2, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        Vector3f origin = new Vector3f(5.5f, 10.5f, 7.0f);
        Vector3f dir = new Vector3f(0.0f, 0.0f, -1.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Ray along -Z should hit block at (5, 10, 2)";
        assert hit.bx == 5 && hit.by == 10 && hit.bz == 2;
        assert hit.getFace() == Direction.SOUTH : "Hit face must be SOUTH (normal (0, 0, +1))";
        assert hit.normalZ == 1;
        assert Math.abs(hit.getHitPosition().z - 3.0f) < 0.001f; // South face of block 2 is at z=3.0

        System.out.println("PASSED");
    }

    private static void testVerticalY() {
        System.out.print("[P8.1] Vertical +Y and -Y raycasts... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(5, 20, 5, Blocks.STONE.getDefaultState());
        chunk.setBlockState(5, 10, 5, Blocks.DIRT.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        // Ray shooting UPwards (+Y) towards (5, 20, 5)
        Vector3f upOrigin = new Vector3f(5.5f, 15.0f, 5.5f);
        Vector3f upDir = new Vector3f(0.0f, 1.0f, 0.0f);
        RaycastHit upHit = Raycast.cast(world, upOrigin, upDir, 10.0f);
        assert upHit != null : "Shooting up should hit stone block at (5, 20, 5)";
        assert upHit.by == 20 && upHit.getFace() == Direction.DOWN : "Hit face must be DOWN";
        assert upHit.normalY == -1;
        assert Math.abs(upHit.getHitPosition().y - 20.0f) < 0.001f;

        // Ray shooting DOWNwards (-Y) towards (5, 10, 5)
        Vector3f downOrigin = new Vector3f(5.5f, 15.0f, 5.5f);
        Vector3f downDir = new Vector3f(0.0f, -1.0f, 0.0f);
        RaycastHit downHit = Raycast.cast(world, downOrigin, downDir, 10.0f);
        assert downHit != null : "Shooting down should hit dirt block at (5, 10, 5)";
        assert downHit.by == 10 && downHit.getFace() == Direction.UP : "Hit face must be UP";
        assert downHit.normalY == 1;
        assert Math.abs(downHit.getHitPosition().y - 11.0f) < 0.001f; // Top of dirt at y=11.0

        System.out.println("PASSED");
    }

    private static void testNegativeWorldCoordinates() {
        System.out.print("[P8.1] Negative world coordinates... ");

        ChunkManager world = createTestWorld();
        // Negative chunk (-1, -1) covers x: -16..-1, z: -16..-1
        Chunk negChunk = new Chunk(-1, -1);
        // Place stone at world coords (-10, 5, -10): local in chunk is lx = -10 - (-16) = 6, lz = 6
        negChunk.setBlockState(6, 5, 6, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(-1, -1), negChunk);

        assert world.getBlockStateAt(-10, 5, -10).is(Blocks.STONE) : "Stone block must exist at (-10, 5, -10)";

        Vector3f origin = new Vector3f(-15.0f, 5.5f, -9.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Raycast in negative coords must hit block at (-10, 5, -10)";
        assert hit.bx == -10 && hit.by == 5 && hit.bz == -10 : "Expected hit at (-10, 5, -10), got " + hit.bx + "," + hit.by + "," + hit.bz;
        assert hit.getFace() == Direction.WEST;
        assert Math.abs(hit.getHitPosition().x - (-10.0f)) < 0.001f : "Hit surface X must be -10.0";
        assert hit.getChunk().cx() == -1 && hit.getChunk().cz() == -1 : "Chunk position must be (-1, -1)";

        System.out.println("PASSED");
    }

    private static void testBoundaryOrigins() {
        System.out.print("[P8.1] Boundary origins (exact voxel faces)... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(4, 5, 5, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        // Ray origin lying EXACTLY on the boundary x = 1.000000f, shooting towards x=4
        Vector3f origin = new Vector3f(1.0f, 5.5f, 5.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "Ray starting on integer boundary must hit block at x=4";
        assert hit.bx == 4 && hit.by == 5 && hit.bz == 5;
        assert Math.abs(hit.getDistance() - 3.0f) < 0.001f : "Distance from 1.0 to 4.0 must be 3.0";

        System.out.println("PASSED");
    }

    private static void testExactVoxelCorners() {
        System.out.print("[P8.1] Exact voxel corners & diagonals... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(5, 5, 5, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);

        // Diagonal ray shooting from (1.0, 1.0, 1.0) towards (5.5, 5.5, 5.5)
        Vector3f origin = new Vector3f(1.0f, 1.0f, 1.0f);
        Vector3f dir = new Vector3f(1.0f, 1.0f, 1.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 20.0f);
        assert hit != null : "Diagonal ray should successfully intersect block at (5, 5, 5)";
        assert hit.bx == 5 && hit.by == 5 && hit.bz == 5;

        System.out.println("PASSED");
    }

    private static void testZeroDirectionVector() {
        System.out.print("[P8.1] Zero and near-zero direction vectors... ");

        ChunkManager world = createTestWorld();
        Vector3f origin = new Vector3f(2.0f, 2.0f, 2.0f);
        Vector3f zeroDir = new Vector3f(0.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, zeroDir, 5.0f);
        assert hit == null : "Raycast with zero length direction must return null without crashing";

        Vector3f tinyDir = new Vector3f(0.0000001f, 0.0f, 0.0f);
        RaycastHit tinyHit = Raycast.cast(world, origin, tinyDir, 5.0f);
        assert tinyHit == null : "Raycast with sub-threshold direction length must return null";

        System.out.println("PASSED");
    }

    private static void testRaycastHitObjectIntegrity() {
        System.out.print("[P8.2] Proper RaycastHit object structure & getters... ");

        ChunkManager world = createTestWorld();
        Chunk chunk = new Chunk(2, 3);
        chunk.setBlockState(4, 12, 8, Blocks.OAK_LOG.getDefaultState());
        world.setChunk(new ChunkPos(2, 3), chunk);

        int worldX = 2 * 16 + 4; // 36
        int worldY = 12;
        int worldZ = 3 * 16 + 8; // 56

        Vector3f origin = new Vector3f(worldX - 3.5f, worldY + 0.5f, worldZ + 0.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(world, origin, dir, 10.0f);
        assert hit != null : "RaycastHit must not be null";

        // Validate getters
        ChunkPos chunkPos = hit.getChunk();
        assert chunkPos != null && chunkPos.cx() == 2 && chunkPos.cz() == 3 : "getChunk() mismatch";

        WorldBlockPos blockPos = hit.getBlock();
        assert blockPos != null && blockPos.x() == worldX && blockPos.y() == worldY && blockPos.z() == worldZ : "getBlock() mismatch";

        Direction face = hit.getFace();
        assert face == Direction.WEST : "getFace() should be WEST";

        Vector3f hitPos = hit.getHitPosition();
        assert hitPos != null && Math.abs(hitPos.x - worldX) < 0.001f : "getHitPosition() X mismatch";

        float dist = hit.getDistance();
        assert Math.abs(dist - 3.5f) < 0.001f : "getDistance() mismatch";

        BlockState state = hit.getState();
        assert state != null && state.is(Blocks.OAK_LOG) : "getState() mismatch";

        // Validate backward-compatibility fields
        assert hit.bx == worldX && hit.by == worldY && hit.bz == worldZ;
        assert hit.normalX == -1 && hit.normalY == 0 && hit.normalZ == 0;
        assert hit.blockType == Blocks.OAK_LOG.getLegacyId();

        System.out.println("PASSED");
    }
}
