package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Axis;

import java.util.HashMap;
import java.util.Map;

public class ChunkStreamingTest {
    public static void main(String[] args) {
        System.out.println("=== Running Chunk Streaming Architecture Tests ===");

        // 1. ChunkPos Tests
        testChunkPos();

        // 2. Frustum Culler Tests
        testFrustumCuller();

        // 3. Chunk Serializer Tests
        testChunkSerializer();

        // 4. Region File & Region Manager Persistence Tests
        testRegionPersistence();

        // 5. ChunkManager Streaming & Life Cycle Tests
        testChunkStreaming();

        // 6. Block State Modification & Unloaded Chunk Delta Retention
        testDeltasOnUnloadedChunks();

        System.out.println("\n🎉 ALL CHUNK STREAMING & PERSISTENCE TESTS PASSED SUCCESSFULLY!");
    }

    private static void testChunkSerializer() {
        System.out.print("Testing ChunkSerializer binary compression...");
        try {
            Chunk chunk = new Chunk(5, -7);
            chunk.setBlockState(0, 64, 0, Blocks.GRASS.getDefaultState());
            chunk.setBlockState(15, 70, 15, Blocks.DIAMOND_ORE.getDefaultState());
            chunk.setBlockState(4, 12, 4, Blocks.WATER.getDefaultState());

            byte[] serialized = ChunkSerializer.serialize(chunk);
            assert serialized != null && serialized.length > 0 : "Serialized payload should not be empty";

            Chunk deserialized = ChunkSerializer.deserialize(serialized);
            assert deserialized.getCx() == 5 && deserialized.getCz() == -7 : "Chunk coordinates mismatch";
            assert deserialized.getBlockState(0, 64, 0).is(Blocks.GRASS) : "Expected GRASS at (0,64,0)";
            assert deserialized.getBlockState(15, 70, 15).is(Blocks.DIAMOND_ORE) : "Expected DIAMOND_ORE at (15,70,15)";
            assert deserialized.getBlockState(4, 12, 4).is(Blocks.WATER) : "Expected WATER at (4,12,4)";

            System.out.println(" PASSED! (128 KB chunk compressed to " + serialized.length + " bytes)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testRegionPersistence() {
        System.out.print("Testing RegionFile & RegionManager disk storage...");
        java.io.File tempDir = new java.io.File("temp_test_world_" + System.currentTimeMillis());
        try {
            RegionManager regionManager = new RegionManager(tempDir);

            Chunk c1 = new Chunk(0, 0);
            c1.setBlockState(8, 64, 8, Blocks.OAK_LOG.getDefaultState());
            regionManager.saveChunk(c1);

            Chunk c2 = new Chunk(35, -40); // Falls into another region file r.1.-2.jmc
            c2.setBlockState(2, 50, 2, Blocks.COBBLESTONE.getDefaultState());
            regionManager.saveChunk(c2);

            regionManager.close();

            // Re-open with fresh RegionManager to test disk persistence
            RegionManager reloadManager = new RegionManager(tempDir);
            Chunk loadedC1 = reloadManager.loadChunk(0, 0);
            assert loadedC1 != null : "Failed to load chunk (0,0) from disk";
            assert loadedC1.getBlockState(8, 64, 8).is(Blocks.OAK_LOG) : "Expected OAK_LOG in chunk (0,0)";

            Chunk loadedC2 = reloadManager.loadChunk(35, -40);
            assert loadedC2 != null : "Failed to load chunk (35,-40) from disk";
            assert loadedC2.getBlockState(2, 50, 2).is(Blocks.COBBLESTONE) : "Expected COBBLESTONE in chunk (35,-40)";

            reloadManager.close();
            System.out.println(" PASSED!");
        } finally {
            deleteRecursive(tempDir);
        }
    }

    private static void deleteRecursive(java.io.File file) {
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }

    private static void testFrustumCuller() {
        System.out.print("Testing FrustumCuller...");
        com.mcjournal.client.Camera camera = new com.mcjournal.client.Camera();
        camera.setPosition(0, 64, 0);
        camera.setYaw(0); // Looking towards -Z
        camera.setPitch(0);
        camera.updateProjection(16.0f / 9.0f);
        camera.updateView();

        com.mcjournal.client.FrustumCuller culler = new com.mcjournal.client.FrustumCuller();
        culler.update(camera.getProjectionMatrix(), camera.getViewMatrix());

        // Chunk in front of camera (cx=0, cz=-3 -> Z: -48 to -32) should be in frustum
        assert culler.isChunkInFrustum(0, -3) : "Chunk (0, -3) in front of camera should be visible";

        // Chunk behind camera (cx=0, cz=5 -> Z: 80 to 96) should NOT be in frustum
        assert !culler.isChunkInFrustum(0, 5) : "Chunk (0, 5) behind camera should be culled";

        System.out.println(" PASSED!");
    }

    private static void testChunkPos() {
        System.out.print("Testing ChunkPos...");
        ChunkPos p1 = ChunkPos.fromWorldCoords(15, 31);
        assert p1.x() == 0 && p1.z() == 1 : "Expected (0, 1) got " + p1;

        ChunkPos p2 = ChunkPos.fromWorldCoords(-1, -17);
        assert p2.x() == -1 && p2.z() == -2 : "Expected (-1, -2) got " + p2;

        ChunkPos p3 = new ChunkPos(0, 0);
        ChunkPos p4 = new ChunkPos(3, 4);
        assert p3.distanceChebyshev(p4) == 4 : "Expected Chebyshev distance 4";
        assert p3.distanceSquared(p4) == 25.0 : "Expected distSq 25";

        assert p1.equals(new ChunkPos(0, 1));
        assert p1.hashCode() == new ChunkPos(0, 1).hashCode();
        System.out.println(" PASSED!");
    }

    private static void testChunkStreaming() {
        System.out.print("Testing ChunkManager streaming...");
        ChunkManager manager = new ChunkManager(4, 4242L); // Render distance 4
        try {
            // Initial spawn wait (radius 2 -> 5x5 = 25 chunks)
            manager.waitForInitialChunks(0, 0, 2);
            assert manager.getLoadedChunkCount() == 25 : "Expected 25 chunks, got " + manager.getLoadedChunkCount();

            // Verify spawn block retrieval
            BlockState surfaceBlock = manager.getBlockStateAt(8, 64, 8);
            assert surfaceBlock != null : "Surface block should not be null";

            // Drain uploads
            int uploads = 0;
            while (manager.pollPendingMeshUpload() != null) {
                uploads++;
            }
            assert uploads == 25 : "Expected 25 mesh uploads, got " + uploads;

            // Move player far away (e.g. chunk (30, 30))
            manager.updatePlayerPosition(30, 30);

            // Wait a moment for worker threads to process
            Thread.sleep(300);

            // Verify old chunks are queued for unload (beyond 4 + 3 = 7 distance)
            assert manager.hasPendingUnloads() : "Should have pending chunk unloads";
            int unloads = 0;
            while (manager.pollPendingMeshUnload() != null) {
                unloads++;
            }
            assert unloads > 0 : "Expected unloads > 0, got " + unloads;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            manager.shutdown();
        }
        System.out.println(" PASSED!");
    }

    private static void testDeltasOnUnloadedChunks() {
        System.out.print("Testing voxel deltas on unloaded chunks...");
        ChunkManager manager = new ChunkManager(4, 9999L);
        try {
            // Player starts at (0,0)
            manager.waitForInitialChunks(0, 0, 1);

            // Apply a modified block state in a distant chunk that is NOT loaded (chunk 50, 50 -> world coords 800, 70, 800)
            int wx = 800;
            int wy = 70;
            int wz = 800;
            BlockState diamondState = Blocks.DIAMOND_ORE.getDefaultState();
            Map<String, BlockState> deltas = new HashMap<>();
            deltas.put(wx + "," + wy + "," + wz, diamondState);

            manager.applyModifiedBlockStates(deltas);

            // Verify chunk (50, 50) is not loaded yet
            assert !manager.isChunkLoaded(50, 50) : "Chunk (50,50) should not be loaded yet";

            // Move player to chunk (50, 50) and stream it in
            manager.waitForInitialChunks(50, 50, 1);

            // Check that the modified block state was applied when chunk was generated!
            BlockState block = manager.getBlockStateAt(wx, wy, wz);
            assert block.is(Blocks.DIAMOND_ORE) : "Expected DIAMOND_ORE at (800, 70, 800), got " + block;

        } finally {
            manager.shutdown();
        }
        System.out.println(" PASSED!");
    }
}
