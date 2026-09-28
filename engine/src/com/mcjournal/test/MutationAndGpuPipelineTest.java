package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.Blocks;

import java.util.Set;

public class MutationAndGpuPipelineTest {

    public static void main(String[] args) {
        System.out.println("=== Running Mutation System (P5) & GPU Pipeline (P4) Tests ===");

        testDirtyChunkSetDeduplication();
        testDirtyChunkBoundaryTracking();
        testChunkManagerBatchedMutation();
        testInterleavedIndexedMeshGeometry();
        testGpuUploadBudgeting();

        System.out.println("\n🎉 ALL MUTATION AND GPU PIPELINE TESTS PASSED SUCCESSFULLY!");
    }

    private static void testDirtyChunkSetDeduplication() {
        System.out.print("Testing DirtyChunkSet deduplication... ");

        DirtyChunkSet dirtySet = new DirtyChunkSet();

        // 50 block changes inside chunk (2, 3)
        for (int i = 0; i < 50; i++) {
            dirtySet.markBlockModified(2 * 16 + 5, 64 + (i % 10), 3 * 16 + 5);
        }

        assert dirtySet.size() == 1 : "50 changes in the same chunk must result in exactly 1 dirty chunk entry, got: " + dirtySet.size();

        Set<ChunkPos> drained = dirtySet.drainDirtyChunks();
        assert drained.size() == 1 : "Drained set must contain 1 entry";
        assert drained.contains(new ChunkPos(2, 3)) : "Drained set must contain chunk (2, 3)";
        assert dirtySet.isEmpty() : "DirtyChunkSet must be empty after drain";

        System.out.println("PASSED!");
    }

    private static void testDirtyChunkBoundaryTracking() {
        System.out.print("Testing chunk boundary and corner dirtiness tracking... ");

        DirtyChunkSet dirtySet = new DirtyChunkSet();

        // 1. Interior voxel at (5, 64, 5) in chunk (0, 0) -> only (0, 0) dirty
        dirtySet.markBlockModified(5, 64, 5);
        Set<ChunkPos> interior = dirtySet.drainDirtyChunks();
        assert interior.size() == 1 && interior.contains(new ChunkPos(0, 0)) : "Interior voxel must only mark center chunk dirty";

        // 2. West boundary voxel at (0, 64, 5) -> marks (0, 0) and (-1, 0)
        dirtySet.markBlockModified(0, 64, 5);
        Set<ChunkPos> west = dirtySet.drainDirtyChunks();
        assert west.size() == 2 : "West boundary must mark 2 chunks dirty, got: " + west.size();
        assert west.contains(new ChunkPos(0, 0)) && west.contains(new ChunkPos(-1, 0)) : "West boundary must mark (0,0) and (-1,0)";

        // 3. South-West corner voxel at (0, 64, 0) -> marks (0, 0), (-1, 0), (0, -1), and (-1, -1)
        dirtySet.markBlockModified(0, 64, 0);
        Set<ChunkPos> corner = dirtySet.drainDirtyChunks();
        assert corner.size() == 4 : "Corner voxel must mark 4 chunks dirty, got: " + corner.size();
        assert corner.contains(new ChunkPos(0, 0)) && corner.contains(new ChunkPos(-1, 0))
            && corner.contains(new ChunkPos(0, -1)) && corner.contains(new ChunkPos(-1, -1)) : "Corner must mark all 4 adjacent chunks";

        System.out.println("PASSED!");
    }

    private static void testChunkManagerBatchedMutation() {
        System.out.print("Testing ChunkManager asynchronous dirty batching... ");

        ChunkManager manager = new ChunkManager(2, 42L);
        Chunk chunk = new Chunk(0, 0);
        manager.setChunk(new ChunkPos(0, 0), chunk);

        // Perform 20 block mutations in chunk (0, 0)
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 5; z++) {
                manager.setBlockStateAt(x, 64, z, Blocks.COBBLESTONE.getDefaultState());
            }
        }

        // Verify dirty set accumulated chunk (0, 0) without immediately meshing
        assert manager.getDirtyChunks().size() == 1 : "Dirty chunk count must be 1";

        // Synchronously flush dirty chunks
        manager.flushDirtyChunks();

        assert manager.getDirtyChunks().isEmpty() : "Dirty chunks must be empty after flush";
        assert manager.getChunkMesh(new ChunkPos(0, 0)) != null : "Mesh must exist after dirty flush";
        assert manager.pollPendingMeshUpload() != null : "Upload queue must contain pending mesh for GPU";

        System.out.println("PASSED!");
    }

    private static void testInterleavedIndexedMeshGeometry() {
        System.out.print("Testing interleaved VBO layout & indexed quad geometry (P4.1 & P4.2)... ");

        Chunk chunk = new Chunk(0, 0);
        // Create 16x16 stone layer
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                chunk.setBlockState(x, 64, z, Blocks.STONE.getDefaultState());
            }
        }

        ChunkNeighborhood neighborhood = ChunkNeighborhood.of(chunk, null);
        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborhood);

        // 1. Verify interleaved stride (13 floats = 52 bytes)
        assert mesh.solidVertices.length > 0 : "Interleaved solid vertices must not be empty";
        assert mesh.solidVertices.length % 13 == 0 : "Vertex buffer length must be a multiple of 13 floats (stride 52 bytes)";

        // 2. Verify indexed triangles (6 indices per quad)
        assert mesh.solidIndices.length > 0 : "Solid indices must not be empty";
        assert mesh.solidIndices.length % 6 == 0 : "Index buffer length must be a multiple of 6 indices";

        int uniqueVertexCount = mesh.solidVertices.length / 13;
        int indexCount = mesh.solidIndices.length;
        int quadCount = indexCount / 6;

        // In indexed geometry, each quad contributes exactly 4 unique vertices and 6 indices
        assert uniqueVertexCount == quadCount * 4 : "Unique vertices must equal 4 * quadCount";

        // 3. Verify index bounds (all indices must be in [0, uniqueVertexCount - 1])
        for (int idx : mesh.solidIndices) {
            assert idx >= 0 && idx < uniqueVertexCount : "Index out of vertex bounds: " + idx + " vs max " + uniqueVertexCount;
        }

        // 4. Verify memory savings vs non-indexed (4 vertices vs 6 vertices = 33.3% vertex reduction)
        int nonIndexedVertexCount = quadCount * 6;
        float vertexReduction = 100.0f * (1.0f - (float) uniqueVertexCount / (float) nonIndexedVertexCount);
        assert Math.abs(vertexReduction - 33.33f) < 0.5f : "Vertex count reduction must be ~33.3%";

        // 5. Verify byte footprint calculation
        assert mesh.getTotalBytes() > 0 : "Total bytes must be > 0";
        int expectedBytes = (mesh.solidVertices.length * 4) + (mesh.solidIndices.length * 4);
        assert mesh.getTotalBytes() == expectedBytes : "Total bytes calculation must match vertex + index size";

        System.out.printf("PASSED! (%d quads, %d unique vertices, %.1f%% vertex reduction)\n",
            quadCount, uniqueVertexCount, vertexReduction);
    }

    private static void testGpuUploadBudgeting() {
        System.out.print("Testing GPU upload queue budgeting (P4.3)... ");

        // Budget: max 2 uploads per frame, max 1000 bytes
        ChunkUploadQueue queue = new ChunkUploadQueue(2, 1000);

        queue.queueUpload(new ChunkPos(0, 0), 400);
        queue.queueUpload(new ChunkPos(1, 0), 400);
        queue.queueUpload(new ChunkPos(2, 0), 400);
        queue.queueUpload(new ChunkPos(3, 0), 400);

        assert queue.getPendingUploadCount() == 4 : "Queue must contain 4 tasks";

        // Simulate 1st frame draining: takes 2 uploads (800 bytes < 1000)
        int frame1Uploads = 0;
        int frame1Bytes = 0;
        while (queue.hasPendingUploads() && frame1Uploads < queue.getMaxUploadsPerFrame()) {
            ChunkUploadQueue.UploadTask task = queue.pollUploadTask();
            frame1Bytes += task.byteSize();
            frame1Uploads++;
        }

        assert frame1Uploads == 2 : "Frame 1 must upload exactly 2 meshes (hit maxUploadsPerFrame budget)";
        assert frame1Bytes == 800 : "Frame 1 uploaded 800 bytes";
        assert queue.getPendingUploadCount() == 2 : "2 meshes must remain for subsequent frames";

        System.out.println("PASSED!");
    }
}
