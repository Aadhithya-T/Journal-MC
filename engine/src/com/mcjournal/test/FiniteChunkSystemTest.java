package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Phase 2 Test Suite: Finite Chunk System, Streaming, and Partial Edge Chunks.
 * Validates:
 * - Finite chunk coordinates (0..156) and strict rejection of out-of-world coordinates.
 * - Partial chunk local limits (cx=156, cz=156 has valid local coords 0..3).
 * - Terrain generation and tree planting strictly within valid partial local bounds.
 * - Chunk streamer filtering at edges and all four corners.
 * - ChunkNeighborhood bounds awareness and perimeter face transparency.
 * - Meshing of partial chunks emitting outer boundary walls and no quads > 2500.
 * - Block mutation and delta overlay validation.
 */
public class FiniteChunkSystemTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING PHASE 2: FINITE CHUNK SYSTEM TESTS    ");
        System.out.println("=================================================");

        testChunkCoordinateValidation();
        testPartialChunkLocalLimits();
        testPartialChunkTerrainGeneration();
        testStreamerEdgeAndCornerFiltering();
        testChunkNeighborhoodBoundsAwareness();
        testPartialChunkMeshingAndPerimeterWalls();
        testBlockMutationAndDeltaValidation();

        System.out.println(">>> ALL PHASE 2 FINITE CHUNK SYSTEM TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    /**
     * 1. Valid and invalid chunk coordinates in 2500x2500 finite world.
     */
    private static void testChunkCoordinateValidation() {
        System.out.print("[Phase 2] Valid chunk ranges [0..156] and invalid chunk rejection... ");
        ChunkManager manager = new ChunkManager(4, 12345L);
        WorldBounds bounds = manager.getWorldBounds();

        // Valid boundary chunks
        assert bounds.isValidChunk(0, 0) : "Chunk (0,0) must be valid";
        assert bounds.isValidChunk(156, 0) : "Chunk (156,0) must be valid";
        assert bounds.isValidChunk(0, 156) : "Chunk (0,156) must be valid";
        assert bounds.isValidChunk(156, 156) : "Chunk (156,156) must be valid";
        assert bounds.isValidChunk(78, 78) : "Center chunk (78,78) must be valid";

        // Invalid chunks
        assert !bounds.isValidChunk(-1, 0) : "Chunk (-1,0) must be invalid";
        assert !bounds.isValidChunk(0, -1) : "Chunk (0,-1) must be invalid";
        assert !bounds.isValidChunk(-1, -1) : "Chunk (-1,-1) must be invalid";
        assert !bounds.isValidChunk(157, 0) : "Chunk (157,0) must be invalid";
        assert !bounds.isValidChunk(0, 157) : "Chunk (0,157) must be invalid";
        assert !bounds.isValidChunk(157, 157) : "Chunk (157,157) must be invalid";

        // ChunkManager access for invalid chunks
        Chunk invalidChunk = new Chunk(-1, 0);
        manager.setChunk(new ChunkPos(-1, 0), invalidChunk);
        assert manager.getChunk(-1, 0) == null : "setChunk on out-of-world pos must be rejected";
        assert !manager.isChunkLoaded(-1, 0) : "isChunkLoaded on out-of-world pos must return false";
        assert manager.getChunkStatus(-1, 0) == ChunkStatus.UNLOADED : "Status of out-of-world chunk must be UNLOADED";
        assert manager.getChunkCoordinateStatus(-1, 0) == WorldBounds.CoordinateStatus.OUT_OF_WORLD : "CoordinateStatus must be OUT_OF_WORLD";

        System.out.println("PASSED");
    }

    /**
     * 2. Partial chunk local ranges: (156, 0), (0, 156), (156, 156).
     */
    private static void testPartialChunkLocalLimits() {
        System.out.print("[Phase 2] Partial edge chunk local coordinate bounds... ");
        WorldBounds bounds = WorldBounds.DEFAULT;

        // East edge chunk (156, 0)
        assert bounds.isPartialChunkX(156) : "Chunk 156 must be partial along X";
        assert !bounds.isPartialChunkZ(0) : "Chunk 0 must NOT be partial along Z";
        assert bounds.getValidLocalBlockCountX(156) == 4 : "cx=156 must have 4 local X blocks";
        assert bounds.getValidLocalBlockCountZ(0) == 16 : "cz=0 must have 16 local Z blocks";
        assert bounds.getValidLocalMaxX(156) == 3 : "cx=156 max local X must be 3";

        // South edge chunk (0, 156)
        assert !bounds.isPartialChunkX(0) : "Chunk 0 must NOT be partial along X";
        assert bounds.isPartialChunkZ(156) : "Chunk 156 must be partial along Z";
        assert bounds.getValidLocalBlockCountX(0) == 16 : "cx=0 must have 16 local X blocks";
        assert bounds.getValidLocalBlockCountZ(156) == 4 : "cz=156 must have 4 local Z blocks";
        assert bounds.getValidLocalMaxZ(156) == 3 : "cz=156 max local Z must be 3";

        // South-East corner chunk (156, 156)
        assert bounds.isPartialChunk(156, 156) : "Chunk (156,156) must be partial";
        assert bounds.getValidLocalBlockCountX(156) == 4;
        assert bounds.getValidLocalBlockCountZ(156) == 4;

        // Local coordinate validation helper
        assert bounds.isLocalBlockValid(156, 156, 3, 64, 3) : "Local (3,64,3) in (156,156) must be valid";
        assert !bounds.isLocalBlockValid(156, 156, 4, 64, 3) : "Local (4,64,3) in (156,156) must be invalid";
        assert !bounds.isLocalBlockValid(156, 156, 3, 64, 4) : "Local (3,64,4) in (156,156) must be invalid";
        assert !bounds.isLocalBlockValid(156, 156, 15, 64, 15) : "Local (15,64,15) in (156,156) must be invalid";

        System.out.println("PASSED");
    }

    /**
     * 3. Terrain generation inside partial chunks: only valid voxels populated, invalid voxels remain air.
     */
    private static void testPartialChunkTerrainGeneration() {
        System.out.print("[Phase 2] Terrain generator partial chunk voxel isolation... ");
        TerrainGenerator gen = new TerrainGenerator(4242L, WorldBounds.DEFAULT);

        // Generate corner chunk (156, 156)
        Chunk corner = gen.generateChunk(156, 156);
        assert corner != null;

        // Valid columns (lx: 0..3, lz: 0..3) must have bedrock at Y=0 and solid terrain
        for (int lz = 0; lz < 4; lz++) {
            for (int lx = 0; lx < 4; lx++) {
                assert corner.getBlockState(lx, 0, lz).is(Blocks.BEDROCK) : "Valid partial voxel must have bedrock at Y=0";
            }
        }

        // Invalid columns (lx >= 4 or lz >= 4) must strictly remain Air (stateId = 0)
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                if (lx >= 4 || lz >= 4) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        assert corner.getStateId(lx, y, lz) == 0 :
                            "Voxel at (" + lx + ", " + y + ", " + lz + ") in partial chunk (156,156) must be Air, got: " + corner.getStateId(lx, y, lz);
                    }
                }
            }
        }

        // Out-of-world chunk generation must return empty air chunk
        Chunk oow = gen.generateChunk(-1, 0);
        for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
            assert oow.getStateIds()[i] == 0 : "Out-of-world chunk must be completely empty air";
        }

        System.out.println("PASSED");
    }

    /**
     * 4. Streamer candidate filtering: player near edges and corners never queues invalid chunks.
     */
    private static void testStreamerEdgeAndCornerFiltering() {
        System.out.print("[Phase 2] Chunk streamer edge & corner filtering... ");
        ChunkStreamer streamer = new ChunkStreamer(4, WorldBounds.DEFAULT);

        // Test West edge (cx = 0, cz = 78)
        List<ChunkPos> westChunks = streamer.computeStreamingChunks(0, 78);
        for (ChunkPos pos : westChunks) {
            assert pos.x() >= 0 && pos.x() <= 156 && pos.z() >= 0 && pos.z() <= 156 :
                "West streaming queued invalid chunk: " + pos;
        }

        // Test East edge (cx = 156, cz = 78)
        List<ChunkPos> eastChunks = streamer.computeStreamingChunks(156, 78);
        for (ChunkPos pos : eastChunks) {
            assert pos.x() >= 0 && pos.x() <= 156 && pos.z() >= 0 && pos.z() <= 156 :
                "East streaming queued invalid chunk: " + pos;
        }

        // Test North edge (cx = 78, cz = 0)
        List<ChunkPos> northChunks = streamer.computeStreamingChunks(78, 0);
        for (ChunkPos pos : northChunks) {
            assert pos.x() >= 0 && pos.x() <= 156 && pos.z() >= 0 && pos.z() <= 156 :
                "North streaming queued invalid chunk: " + pos;
        }

        // Test South edge (cx = 78, cz = 156)
        List<ChunkPos> southChunks = streamer.computeStreamingChunks(78, 156);
        for (ChunkPos pos : southChunks) {
            assert pos.x() >= 0 && pos.x() <= 156 && pos.z() >= 0 && pos.z() <= 156 :
                "South streaming queued invalid chunk: " + pos;
        }

        // Test all four corners
        int[][] corners = {{0, 0}, {156, 0}, {0, 156}, {156, 156}};
        for (int[] corner : corners) {
            List<ChunkPos> cornerChunks = streamer.computeStreamingChunks(corner[0], corner[1]);
            for (ChunkPos pos : cornerChunks) {
                assert pos.x() >= 0 && pos.x() <= 156 && pos.z() >= 0 && pos.z() <= 156 :
                    "Corner " + corner[0] + "," + corner[1] + " queued invalid chunk: " + pos;
            }
        }

        // Verify priority queue from streamer also contains no invalid chunks
        ChunkManager manager = new ChunkManager(4, 9999L);
        PriorityQueue<ChunkPos> pq = streamer.createPriorityQueue(new ChunkPos(0, 0), 1.0f, 0.0f);
        for (ChunkStreamer.ChunkOffset offset : streamer.getSpiralOffsets()) {
            int cx = 0 + offset.dx();
            int cz = 0 + offset.dz();
            if (manager.getWorldBounds().isValidChunk(cx, cz)) {
                pq.add(new ChunkPos(cx, cz));
            }
        }
        while (!pq.isEmpty()) {
            ChunkPos pos = pq.poll();
            assert pos.x() >= 0 && pos.z() >= 0 : "Priority queue contained negative chunk: " + pos;
        }

        manager.shutdown();
        System.out.println("PASSED");
    }

    /**
     * 5. ChunkNeighborhood bounds awareness: out-of-world queries return transparent air, no AO darkening.
     */
    private static void testChunkNeighborhoodBoundsAwareness() {
        System.out.print("[Phase 2] ChunkNeighborhood boundary transparency & AO solid isolation... ");
        ChunkManager manager = new ChunkManager(2, 42L);

        Chunk cornerChunk = new Chunk(0, 0);
        cornerChunk.setBlockState(0, 64, 0, Blocks.STONE.getDefaultState());
        manager.setChunk(cornerChunk.getPos(), cornerChunk);

        ChunkNeighborhood neighborhood = new ChunkNeighborhood(cornerChunk, manager);

        // Query -X neighbor chunk (-1, 0)
        assert neighborhood.isNeighborOutOfWorld(-1, 0) : "(-1, 0) must be out of world";
        assert !neighborhood.isNeighborLoaded(-1, 0) : "(-1, 0) must not be loaded";
        assert neighborhood.getStateId(-1, 64, 0) == 0 : "Out-of-world neighbor must return air";
        assert neighborhood.isTransparent(-1, 64, 0) : "Out-of-world neighbor must be transparent";
        assert !neighborhood.isAOSolid(-1, 64, 0) : "Out-of-world neighbor must not be AO solid";

        // Query -Z neighbor chunk (0, -1)
        assert neighborhood.isNeighborOutOfWorld(0, -1) : "(0, -1) must be out of world";
        assert neighborhood.isTransparent(0, 64, -1) : "(0, -1) must be transparent";
        assert !neighborhood.isAOSolid(0, 64, -1) : "(0, -1) must not be AO solid";

        // Partial chunk (156, 156)
        Chunk partialCorner = new Chunk(156, 156);
        partialCorner.setBlockState(3, 64, 3, Blocks.STONE.getDefaultState());
        manager.setChunk(partialCorner.getPos(), partialCorner);
        ChunkNeighborhood partialNeighborhood = new ChunkNeighborhood(partialCorner, manager);

        // Local lx=4 inside chunk 156 is out of world
        assert partialNeighborhood.getStateId(4, 64, 3) == 0 : "lx=4 in chunk 156 must return air";
        assert partialNeighborhood.isTransparent(4, 64, 3) : "lx=4 in chunk 156 must be transparent";
        assert !partialNeighborhood.isAOSolid(4, 64, 3) : "lx=4 in chunk 156 must not be AO solid";

        manager.shutdown();
        System.out.println("PASSED");
    }

    /**
     * 6. Meshing partial chunks: perimeter faces at X=2499 and Z=2499 generated, no vertices > 2500.
     */
    private static void testPartialChunkMeshingAndPerimeterWalls() {
        System.out.print("[Phase 2] Partial chunk meshing & perimeter wall quad generation... ");
        ChunkManager manager = new ChunkManager(2, 42L);

        Chunk corner = new Chunk(156, 156);
        // Place stone at local (3, 64, 3) which corresponds to world X=2499, Z=2499 (the extreme world corner!)
        corner.setBlockState(3, 64, 3, Blocks.STONE.getDefaultState());
        manager.setChunk(corner.getPos(), corner);

        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(corner, manager);
        assert mesh != null;
        assert !mesh.isEmpty() : "Mesh for corner block must not be empty";

        // Inspect solid vertices (stride 13: pos(3), uv(4), col(3), norm(3))
        float[] verts = mesh.solidVertices;
        int vertexCount = verts.length / 13;
        assert vertexCount > 0 : "Mesh must contain vertices";

        boolean foundEastFace = false;
        boolean foundSouthFace = false;

        for (int i = 0; i < vertexCount; i++) {
            float vx = verts[i * 13];
            float vy = verts[i * 13 + 1];
            float vz = verts[i * 13 + 2];
            float nx = verts[i * 13 + 10];
            float nz = verts[i * 13 + 12];

            // Crucial assertion: no vertex coordinate may exceed the 2500.0f boundary!
            assert vx <= 2500.001f : "Vertex X coordinate exceeded 2500: " + vx;
            assert vz <= 2500.001f : "Vertex Z coordinate exceeded 2500: " + vz;

            // Check if outer +X perimeter wall quad was generated (nx == 1.0f at vx == 2500.0f)
            if (Math.abs(nx - 1.0f) < 0.01f && Math.abs(vx - 2500.0f) < 0.01f) {
                foundEastFace = true;
            }
            // Check if outer +Z perimeter wall quad was generated (nz == 1.0f at vz == 2500.0f)
            if (Math.abs(nz - 1.0f) < 0.01f && Math.abs(vz - 2500.0f) < 0.01f) {
                foundSouthFace = true;
            }
        }

        assert foundEastFace : "East perimeter face (+X face at X=2500) must be generated for block at X=2499";
        assert foundSouthFace : "South perimeter face (+Z face at Z=2500) must be generated for block at Z=2499";

        manager.shutdown();
        System.out.println("PASSED");
    }

    /**
     * 7. Block mutation and delta overlay validation:
     * Attempts to modify out-of-world coordinates or invalid partial chunk voxels are rejected.
     */
    private static void testBlockMutationAndDeltaValidation() {
        System.out.print("[Phase 2] Block mutations and delta overlay rejection... ");
        ChunkManager manager = new ChunkManager(2, 42L);

        // 1. Modifying out-of-bounds coordinates
        assert !manager.setBlockStateAt(-1, 64, 1000, Blocks.STONE.getDefaultState()) : "setBlockStateAt at X=-1 must return false";
        assert !manager.setBlockStateAt(2500, 64, 1000, Blocks.STONE.getDefaultState()) : "setBlockStateAt at X=2500 must return false";
        assert !manager.setBlockStateAt(1000, 64, -1, Blocks.STONE.getDefaultState()) : "setBlockStateAt at Z=-1 must return false";
        assert !manager.setBlockStateAt(1000, 64, 2500, Blocks.STONE.getDefaultState()) : "setBlockStateAt at Z=2500 must return false";
        assert !manager.setBlockStateAt(1000, -1, 1000, Blocks.STONE.getDefaultState()) : "setBlockStateAt at Y=-1 must return false";
        assert !manager.setBlockStateAt(1000, 256, 1000, Blocks.STONE.getDefaultState()) : "setBlockStateAt at Y=256 must return false";

        // 2. Modifying invalid local coordinates inside chunk 156 (world X: 2496..2499 valid, 2500+ invalid)
        // World X = 2500 is local lx = 4 in chunk 156
        assert !manager.setBlockStateAt(2500, 64, 100, Blocks.STONE.getDefaultState()) : "Voxel at local X=4 in chunk 156 must be rejected";
        assert manager.getBlockStateAt(2500, 64, 100).isAir() : "Invalid partial block position must return Air";

        // 3. Valid block mutation at the world boundary (X=2499, Z=2499)
        Chunk corner = new Chunk(156, 156);
        manager.setChunk(corner.getPos(), corner);
        assert manager.setBlockStateAt(2499, 64, 2499, Blocks.DIAMOND_ORE.getDefaultState()) : "Valid border block at (2499,64,2499) must succeed";
        assert manager.getBlockStateAt(2499, 64, 2499).is(Blocks.DIAMOND_ORE) : "Border block must be DIAMOND_ORE";

        // 4. Breaking block outside bounds
        assert manager.breakBlock(-5, 64, 100) == null : "breakBlock outside world must return null";
        assert manager.breakBlock(2505, 64, 100) == null : "breakBlock outside world must return null";

        // 5. Delta batch overlays outside bounds
        Map<WorldBlockPos, BlockState> deltas = new HashMap<>();
        deltas.put(new WorldBlockPos(-10, 64, 50), Blocks.STONE.getDefaultState());
        deltas.put(new WorldBlockPos(2510, 64, 50), Blocks.STONE.getDefaultState());
        deltas.put(new WorldBlockPos(2499, 70, 2499), Blocks.COBBLESTONE.getDefaultState()); // valid

        manager.applyModifiedWorldBlockStates(deltas);
        assert manager.getBlockStateAt(-10, 64, 50).isAir() : "Out of bounds delta must not be applied";
        assert manager.getBlockStateAt(2510, 64, 50).isAir() : "Out of bounds delta must not be applied";
        assert manager.getBlockStateAt(2499, 70, 2499).is(Blocks.COBBLESTONE) : "Valid border delta must be applied";

        manager.shutdown();
        System.out.println("PASSED");
    }
}
