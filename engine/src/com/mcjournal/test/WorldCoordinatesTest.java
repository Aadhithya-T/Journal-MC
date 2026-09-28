package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkNeighborhood;
import com.mcjournal.ChunkPos;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;

/**
 * P11 Unit Test: World System
 * Covers: chunk coordinates, negative coordinates, block access, and chunk boundaries.
 */
public class WorldCoordinatesTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: WORLD COORDINATES TESTS      ");
        System.out.println("=================================================");

        testChunkPosPackingAndMath();
        testNegativeCoordinateConversions();
        testChunkBlockAccess();
        testChunkBoundaryNeighborhoodAccess();

        System.out.println(">>> ALL WORLD COORDINATES TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testChunkPosPackingAndMath() {
        System.out.print("[P11 - World] ChunkPos packing, unpacking, equality & distances... ");

        int[][] testCoords = {
            {0, 0}, {1, 1}, {-1, -1}, {100, -250}, {-500, 300},
            {16384, -16384}, {-32768, 32767}
        };

        for (int[] coord : testCoords) {
            int cx = coord[0];
            int cz = coord[1];
            ChunkPos pos = new ChunkPos(cx, cz);

            assert pos.x() == cx : "ChunkPos.x() mismatch";
            assert pos.z() == cz : "ChunkPos.z() mismatch";

            long packed = pos.asLong();
            ChunkPos fromPacked = ChunkPos.fromLong(packed);
            assert fromPacked.equals(pos) : "Unpacked ChunkPos must equal original";
            assert fromPacked.hashCode() == pos.hashCode() : "Hash codes must match";
            assert fromPacked.x() == cx : "Unpacked X mismatch";
            assert fromPacked.z() == cz : "Unpacked Z mismatch";
        }

        // Distance squared
        ChunkPos a = new ChunkPos(0, 0);
        ChunkPos b = new ChunkPos(3, 4);
        assert Math.abs(a.distanceSquared(b) - 25.0) < 0.001 : "3-4-5 triangle distance squared must be 25";

        // fromWorldCoords
        ChunkPos fromWorld = ChunkPos.fromWorldCoords(35, -17);
        assert fromWorld.x() == 2 && fromWorld.z() == -2 : "fromWorldCoords mismatch";

        System.out.println("PASSED");
    }

    private static void testNegativeCoordinateConversions() {
        System.out.print("[P11 - World] Negative coordinates floor division & local offset masking... ");

        // Comprehensive verification that cx * 16 + (wx & 15) == wx
        for (int wx = -10000; wx <= 10000; wx++) {
            int cx = Math.floorDiv(wx, 16);
            int localX = wx & 15;
            int reconstructed = cx * 16 + localX;
            assert reconstructed == wx : "Reconstructed world coordinate must match original: wx=" + wx + " cx=" + cx + " lx=" + localX;
            assert localX >= 0 && localX < 16 : "Local chunk coordinate must be strictly [0, 15]";
        }

        // Specific edge case validations
        assert Math.floorDiv(0, 16) == 0 && (0 & 15) == 0;
        assert Math.floorDiv(15, 16) == 0 && (15 & 15) == 15;
        assert Math.floorDiv(16, 16) == 1 && (16 & 15) == 0;

        assert Math.floorDiv(-1, 16) == -1 && (-1 & 15) == 15;
        assert Math.floorDiv(-16, 16) == -1 && (-16 & 15) == 0;
        assert Math.floorDiv(-17, 16) == -2 && (-17 & 15) == 15;
        assert Math.floorDiv(-32, 16) == -2 && (-32 & 15) == 0;
        assert Math.floorDiv(-33, 16) == -3 && (-33 & 15) == 15;

        System.out.println("PASSED (20,001 points verified)");
    }

    private static void testChunkBlockAccess() {
        System.out.print("[P11 - World] Chunk local block state access & bounds... ");

        Chunk chunk = new Chunk(-3, 7);
        assert chunk.getCx() == -3 && chunk.getCz() == 7;

        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS.getDefaultState();

        // Corners and height boundaries
        chunk.setBlockState(0, 0, 0, stone);
        chunk.setBlockState(15, 0, 15, stone);
        chunk.setBlockState(0, Chunk.HEIGHT - 1, 0, grass);
        chunk.setBlockState(15, Chunk.HEIGHT - 1, 15, dirt);

        assert chunk.getBlockState(0, 0, 0).equals(stone) : "Origin corner block state mismatch";
        assert chunk.getBlockState(15, 0, 15).equals(stone) : "Max XZ corner block state mismatch";
        assert chunk.getBlockState(0, Chunk.HEIGHT - 1, 0).equals(grass) : "Top corner block state mismatch";
        assert chunk.getBlockState(15, Chunk.HEIGHT - 1, 15).equals(dirt) : "Top corner block state mismatch";

        // Interior block
        chunk.setBlockState(8, 64, 8, stone);
        assert chunk.getBlockState(8, 64, 8).equals(stone);

        // Verify out of bounds handling
        assert chunk.getBlockState(0, -1, 0).isAir() : "Below Y=0 must return air";
        assert chunk.getBlockState(0, Chunk.HEIGHT, 0).isAir() : "Above HEIGHT must return air";

        System.out.println("PASSED");
    }

    private static void testChunkBoundaryNeighborhoodAccess() {
        System.out.print("[P11 - World] Chunk boundary traversal & 3x3 neighborhood lookups... ");

        ChunkManager manager = new ChunkManager(12345L);

        int centerCx = 0;
        int centerCz = 0;
        Chunk center = new Chunk(centerCx, centerCz);

        Chunk posX = new Chunk(1, 0);
        Chunk negX = new Chunk(-1, 0);
        Chunk posZ = new Chunk(0, 1);
        Chunk negZ = new Chunk(0, -1);

        BlockState diamond = Blocks.DIAMOND_ORE.getDefaultState();
        BlockState cobble = Blocks.COBBLESTONE.getDefaultState();
        BlockState sand = Blocks.SAND.getDefaultState();
        BlockState log = Blocks.OAK_LOG.getDefaultState();

        // Place marker blocks along neighbor borders
        posX.setBlockState(0, 64, 8, diamond);   // +X border: x=16 in center local frame
        negX.setBlockState(15, 64, 8, cobble);   // -X border: x=-1 in center local frame
        posZ.setBlockState(8, 64, 0, sand);      // +Z border: z=16 in center local frame
        negZ.setBlockState(8, 64, 15, log);      // -Z border: z=-1 in center local frame

        manager.setChunk(center.getPos(), center);
        manager.setChunk(posX.getPos(), posX);
        manager.setChunk(negX.getPos(), negX);
        manager.setChunk(posZ.getPos(), posZ);
        manager.setChunk(negZ.getPos(), negZ);

        ChunkNeighborhood neighborhood = new ChunkNeighborhood(center, manager);

        // Query neighbor blocks through center neighborhood
        assert neighborhood.getBlockState(16, 64, 8).equals(diamond) : "+X boundary neighbor lookup failed";
        assert neighborhood.getBlockState(-1, 64, 8).equals(cobble) : "-X boundary neighbor lookup failed";
        assert neighborhood.getBlockState(8, 64, 16).equals(sand) : "+Z boundary neighbor lookup failed";
        assert neighborhood.getBlockState(8, 64, -1).equals(log) : "-Z boundary neighbor lookup failed";

        System.out.println("PASSED");
    }
}
