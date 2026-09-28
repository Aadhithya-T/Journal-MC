package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.Blocks;

public class ChunkMeshingTest {

    public static void main(String[] args) {
        System.out.println("=== Running Chunk Meshing Architecture Tests (P3) ===");

        testChunkNeighborhoodBoundaries();
        testGreedyMeshingVertexReduction();
        testAmbientOcclusionFidelity();
        testChannelSeparation();

        System.out.println("\n🎉 ALL CHUNK MESHING TESTS PASSED SUCCESSFULLY!");
    }

    private static void testChunkNeighborhoodBoundaries() {
        System.out.print("Testing ChunkNeighborhood boundary access across 3x3 grid... ");

        Chunk center = new Chunk(0, 0);
        Chunk east = new Chunk(1, 0);
        Chunk west = new Chunk(-1, 0);
        Chunk north = new Chunk(0, 1);
        Chunk south = new Chunk(0, -1);
        Chunk northEast = new Chunk(1, 1);

        // Place distinctive blocks at boundary voxels
        center.setBlockState(0, 64, 0, Blocks.STONE.getDefaultState());
        center.setBlockState(15, 64, 15, Blocks.DIRT.getDefaultState());

        east.setBlockState(0, 64, 5, Blocks.COBBLESTONE.getDefaultState());
        west.setBlockState(15, 64, 5, Blocks.SAND.getDefaultState());
        north.setBlockState(5, 64, 0, Blocks.OAK_LOG.getDefaultState());
        south.setBlockState(5, 64, 15, Blocks.BEDROCK.getDefaultState());
        northEast.setBlockState(0, 64, 0, Blocks.DIAMOND_ORE.getDefaultState());

        ChunkManager mockManager = new ChunkManager(2, 42L);
        mockManager.setChunk(new ChunkPos(0, 0), center);
        mockManager.setChunk(new ChunkPos(1, 0), east);
        mockManager.setChunk(new ChunkPos(-1, 0), west);
        mockManager.setChunk(new ChunkPos(0, 1), north);
        mockManager.setChunk(new ChunkPos(0, -1), south);
        mockManager.setChunk(new ChunkPos(1, 1), northEast);

        ChunkNeighborhood neighborhood = ChunkNeighborhood.of(center, mockManager);

        // 1. Center chunk internal queries
        assert neighborhood.getBlockState(0, 64, 0).is(Blocks.STONE) : "Center (0,0) failed";
        assert neighborhood.getBlockState(15, 64, 15).is(Blocks.DIRT) : "Center (15,15) failed";

        // 2. Cardinal neighbor boundary queries (lx = 16 or -1, lz = 16 or -1)
        assert neighborhood.getBlockState(16, 64, 5).is(Blocks.COBBLESTONE) : "East neighbor (+X boundary) failed";
        assert neighborhood.getBlockState(-1, 64, 5).is(Blocks.SAND) : "West neighbor (-X boundary) failed";
        assert neighborhood.getBlockState(5, 64, 16).is(Blocks.OAK_LOG) : "North neighbor (+Z boundary) failed";
        assert neighborhood.getBlockState(5, 64, -1).is(Blocks.BEDROCK) : "South neighbor (-Z boundary) failed";

        // 3. Diagonal corner neighbor query
        assert neighborhood.getBlockState(16, 64, 16).is(Blocks.DIAMOND_ORE) : "NorthEast diagonal neighbor failed";

        // 4. Out of world bounds queries (y < 0 or y >= 256)
        assert neighborhood.getStateId(0, -1, 0) == 0 : "Negative Y must return 0 (air)";
        assert neighborhood.getStateId(0, 256, 0) == 0 : "Y >= 256 must return 0 (air)";

        // 5. Unloaded neighbor corner query
        assert neighborhood.getStateId(-1, 64, -1) == 0 : "Unloaded SouthWest corner must return air safely";

        System.out.println("PASSED!");
    }

    private static void testGreedyMeshingVertexReduction() {
        System.out.print("Testing 2D Greedy Meshing vertex count reduction... ");

        Chunk chunk = new Chunk(0, 0);
        // Create flat 16x16 stone layer at y = 64
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                chunk.setBlockState(x, 64, z, Blocks.STONE.getDefaultState());
            }
        }

        ChunkManager mockManager = new ChunkManager(2, 42L);
        mockManager.setChunk(new ChunkPos(0, 0), chunk);

        ChunkNeighborhood neighborhood = ChunkNeighborhood.of(chunk, mockManager);
        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborhood);

        int solidVertexCount = mesh.solidPositions.length / 3;
        int solidTriangleCount = solidVertexCount / 3;
        int solidQuadCount = solidTriangleCount / 2;

        // In naive meshing, a 16x16 layer generates:
        // Top: 256 quads, Bottom: 256 quads, 4 Sides: 16 quads each = 64 quads.
        // Total naive quads = 576 quads (1,152 triangles, 3,456 vertices).
        // With greedy meshing, the interior merges into huge combined quads.
        // Even with top-face de-tiling dividing the top face into a few rotation clusters,
        // the total quad count must be drastically reduced!
        assert solidQuadCount < 100 : "Expected greedy quad count < 100, but got " + solidQuadCount;

        float reductionPercent = 100.0f * (1.0f - (float) solidQuadCount / 576.0f);
        System.out.printf("PASSED! (%d quads, %.1f%% reduction vs naive 576 quads)\n", solidQuadCount, reductionPercent);
    }

    private static void testAmbientOcclusionFidelity() {
        System.out.print("Testing Ambient Occlusion corner compatibility... ");

        Chunk chunk = new Chunk(0, 0);
        // Create 16x16 stone floor at y = 64
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                chunk.setBlockState(x, 64, z, Blocks.STONE.getDefaultState());
            }
        }
        // Place an obstacle block at (8, 65, 8) sitting on top of the floor
        chunk.setBlockState(8, 65, 8, Blocks.COBBLESTONE.getDefaultState());

        ChunkManager mockManager = new ChunkManager(2, 42L);
        mockManager.setChunk(new ChunkPos(0, 0), chunk);

        ChunkNeighborhood neighborhood = ChunkNeighborhood.of(chunk, mockManager);
        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborhood);

        // Verify that darkened AO exists in the mesh colors
        boolean hasDarkenedAO = false;
        boolean hasBrightAO = false;

        for (int i = 0; i < mesh.solidColors.length; i += 3) {
            float r = mesh.solidColors[i];
            if (r < 0.9f) {
                hasDarkenedAO = true;
            } else if (r >= 0.99f) {
                hasBrightAO = true;
            }
        }

        assert hasDarkenedAO : "Mesh must contain darkened vertex AO values near obstacle";
        assert hasBrightAO : "Mesh must contain fully un-occluded vertex AO values (1.0)";

        System.out.println("PASSED!");
    }

    private static void testChannelSeparation() {
        System.out.print("Testing separate mesh channel generation (Solid vs Cutout vs Water)... ");

        Chunk chunk = new Chunk(0, 0);
        // 1. Solid block: Stone at (2, 64, 2)
        chunk.setBlockState(2, 64, 2, Blocks.STONE.getDefaultState());

        // 2. Cutout plant: Tall grass at (5, 64, 5)
        chunk.setBlockState(5, 64, 5, Blocks.TALL_GRASS.getDefaultState());

        // 3. Cutout leaves: Oak leaves at (8, 64, 8)
        chunk.setBlockState(8, 64, 8, Blocks.OAK_LEAVES.getDefaultState());

        // 4. Fluid: Water at (11, 64, 11)
        chunk.setBlockState(11, 64, 11, Blocks.WATER.getDefaultState());

        ChunkManager mockManager = new ChunkManager(2, 42L);
        mockManager.setChunk(new ChunkPos(0, 0), chunk);

        ChunkNeighborhood neighborhood = ChunkNeighborhood.of(chunk, mockManager);
        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborhood);

        // Assert Solid channel has stone vertices
        assert mesh.solidPositions.length > 0 : "Solid channel must contain stone vertices";

        // Assert Cutout channel has tall grass and leaves vertices
        assert mesh.cutoutPositions.length > 0 : "Cutout channel must contain foliage/leaves vertices";

        // Assert Water channel has water vertices
        assert mesh.waterPositions.length > 0 : "Water channel must contain fluid vertices";

        // Verify vertex count math: 3 coordinates per position, 4 per UV, 3 per color, 3 per normal
        assert mesh.solidPositions.length % 3 == 0 : "Solid positions must be multiples of 3";
        assert mesh.solidUvs.length % 4 == 0 : "Solid UVs must be multiples of 4 (vec4)";
        assert (mesh.solidPositions.length / 3) == (mesh.solidUvs.length / 4) : "Position vertex count must equal UV vertex count";

        assert mesh.cutoutPositions.length % 3 == 0 : "Cutout positions must be multiples of 3";
        assert mesh.cutoutUvs.length % 4 == 0 : "Cutout UVs must be multiples of 4 (vec4)";
        assert (mesh.cutoutPositions.length / 3) == (mesh.cutoutUvs.length / 4) : "Cutout position count must equal UV count";

        assert mesh.waterPositions.length % 3 == 0 : "Water positions must be multiples of 3";
        assert mesh.waterUvs.length % 4 == 0 : "Water UVs must be multiples of 4 (vec4)";
        assert (mesh.waterPositions.length / 3) == (mesh.waterUvs.length / 4) : "Water position count must equal UV count";

        System.out.println("PASSED!");
    }
}
