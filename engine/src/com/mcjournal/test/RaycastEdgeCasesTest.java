package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Direction;
import com.mcjournal.client.Raycast;
import com.mcjournal.client.RaycastHit;
import org.joml.Vector3f;

import java.io.File;

/**
 * P11 Unit Test: Raycasting DDA Edge Cases
 * Covers: internal ray origins, boundary origins, grazing parallel rays,
 * chunk boundary transitions, negative coordinates, max distance cutoffs, and empty worlds.
 */
public class RaycastEdgeCasesTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: RAYCAST EDGE CASES TESTS     ");
        System.out.println("=================================================");

        testInternalVoxelOrigin();
        testGrazingParallelRay();
        testExactBoundaryOrigin();
        testChunkBoundaryCrossing();
        testNegativeCoordinateTraversal();
        testMaxDistanceCutoff();
        testEmptyAirMiss();

        System.out.println(">>> ALL RAYCAST EDGE CASES TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testInternalVoxelOrigin() {
        System.out.print("[P11 - Raycast] Origin inside solid block... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk c = new Chunk(0, 0);
        c.setBlockState(5, 64, 5, Blocks.STONE.getDefaultState());
        cm.setChunk(c.getPos(), c);

        // Ray starts inside (5.5, 64.5, 5.5)
        Vector3f origin = new Vector3f(5.5f, 64.5f, 5.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(cm, origin, dir, 5.0f);
        assert hit != null : "Should detect collision when ray starts inside solid block";
        assert hit.bx == 5 && hit.by == 64 && hit.bz == 5 : "Hit coordinates must match container block";
        assert hit.distance <= 0.001f : "Hit distance must be near zero for internal origin";

        System.out.println("PASSED");
    }

    private static void testGrazingParallelRay() {
        System.out.print("[P11 - Raycast] Grazing ray parallel to voxel face... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk c = new Chunk(0, 0);
        c.setBlockState(5, 64, 5, Blocks.STONE.getDefaultState());
        cm.setChunk(c.getPos(), c);

        // Ray starts at X=4.9999 (just outside face at X=5.0) and shoots parallel to face in +Z
        Vector3f origin = new Vector3f(4.9999f, 64.5f, 3.0f);
        Vector3f dir = new Vector3f(0.0f, 0.0f, 1.0f);

        RaycastHit hit = Raycast.cast(cm, origin, dir, 5.0f);
        assert hit == null : "Ray strictly parallel outside face must not falsely intersect the voxel";

        System.out.println("PASSED");
    }

    private static void testExactBoundaryOrigin() {
        System.out.print("[P11 - Raycast] Ray origin exactly on voxel face boundary... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk c = new Chunk(0, 0);
        c.setBlockState(5, 64, 5, Blocks.STONE.getDefaultState());
        cm.setChunk(c.getPos(), c);

        // 1. Origin exactly on boundary face X=5.0f (Math.floor(5.0) = 5, inside block)
        Vector3f originOnFace = new Vector3f(5.0f, 64.5f, 5.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hitOnFace = Raycast.cast(cm, originOnFace, dir, 5.0f);
        assert hitOnFace != null : "Should detect hit when origin is exactly on voxel boundary";
        assert hitOnFace.bx == 5 && hitOnFace.by == 64 && hitOnFace.bz == 5;
        assert hitOnFace.distance <= 0.001f;

        // 2. Origin infinitesimal distance outside face X=4.9999f shooting in
        Vector3f originJustOutside = new Vector3f(5.0f - 1e-4f, 64.5f, 5.5f);
        RaycastHit hitFromOutside = Raycast.cast(cm, originJustOutside, dir, 5.0f);
        assert hitFromOutside != null : "Should detect entry when shooting across boundary face";
        assert hitFromOutside.bx == 5 && hitFromOutside.by == 64 && hitFromOutside.bz == 5;
        assert hitFromOutside.getFace() == Direction.WEST : "Face entered from west must be WEST";

        System.out.println("PASSED");
    }

    private static void testChunkBoundaryCrossing() {
        System.out.print("[P11 - Raycast] Ray crossing chunk boundary (x=15 -> x=16)... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk chunk0 = new Chunk(0, 0);
        Chunk chunk1 = new Chunk(1, 0);

        // Place target block at world (16, 64, 5) -> chunk (1, 0), local (0, 64, 5)
        chunk1.setBlockState(0, 64, 5, Blocks.COBBLESTONE.getDefaultState());
        cm.setChunk(chunk0.getPos(), chunk0);
        cm.setChunk(chunk1.getPos(), chunk1);

        Vector3f origin = new Vector3f(14.5f, 64.5f, 5.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(cm, origin, dir, 5.0f);
        assert hit != null : "Ray must cross chunk boundary and hit block in neighboring chunk";
        assert hit.bx == 16 && hit.by == 64 && hit.bz == 5 : "Hit block coordinates mismatch";
        assert hit.getChunk().x() == 1 && hit.getChunk().z() == 0 : "Hit chunk pos must be (1, 0)";
        assert hit.getFace() == Direction.WEST;
        assert Math.abs(hit.distance - 1.5f) < 0.05f : "Distance should be ~1.5 blocks";

        System.out.println("PASSED");
    }

    private static void testNegativeCoordinateTraversal() {
        System.out.print("[P11 - Raycast] Traversal across negative coordinates... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk negChunk = new Chunk(-1, -1);

        // Place block at world (-5, 70, -8) -> local (-5 & 15 = 11, 70, -8 & 15 = 8)
        negChunk.setBlockState(-5 & 15, 70, -8 & 15, Blocks.DIAMOND_ORE.getDefaultState());
        cm.setChunk(negChunk.getPos(), negChunk);

        Vector3f origin = new Vector3f(-2.5f, 70.5f, -7.5f);
        Vector3f dir = new Vector3f(-1.0f, 0.0f, 0.0f);

        RaycastHit hit = Raycast.cast(cm, origin, dir, 5.0f);
        assert hit != null : "Ray must traverse negative space and hit target";
        assert hit.bx == -5 && hit.by == 70 && hit.bz == -8 : "Coordinates mismatch";
        assert hit.getFace() == Direction.EAST : "Hit face from east should be EAST";

        System.out.println("PASSED");
    }

    private static void testMaxDistanceCutoff() {
        System.out.print("[P11 - Raycast] Max distance reach cutoff... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk c = new Chunk(0, 0);
        c.setBlockState(8, 64, 8, Blocks.STONE.getDefaultState());
        cm.setChunk(c.getPos(), c);

        Vector3f origin = new Vector3f(0.5f, 64.5f, 8.5f);
        Vector3f dir = new Vector3f(1.0f, 0.0f, 0.0f); // Block is at X=8.0 (distance 7.5 blocks away)

        // Reach 5.0 blocks: should not reach
        RaycastHit shortHit = Raycast.cast(cm, origin, dir, 5.0f);
        assert shortHit == null : "Raycast with reach 5.0 should not reach block 7.5 units away";

        // Reach 10.0 blocks: should reach
        RaycastHit longHit = Raycast.cast(cm, origin, dir, 10.0f);
        assert longHit != null : "Raycast with reach 10.0 should reach block 7.5 units away";
        assert longHit.bx == 8;

        System.out.println("PASSED");
    }

    private static void testEmptyAirMiss() {
        System.out.print("[P11 - Raycast] Empty air miss handling... ");

        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_ray_edge"));
        Chunk c = new Chunk(0, 0); // All air
        cm.setChunk(c.getPos(), c);

        Vector3f origin = new Vector3f(8.0f, 100.0f, 8.0f);
        Vector3f dir = new Vector3f(0.0f, 1.0f, 0.0f);

        RaycastHit hit = Raycast.cast(cm, origin, dir, 20.0f);
        assert hit == null : "Raycast into empty air should return null";

        System.out.println("PASSED");
    }
}
