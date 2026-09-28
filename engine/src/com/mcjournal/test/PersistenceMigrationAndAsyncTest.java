package com.mcjournal.test;

import com.mcjournal.*;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Player;
import com.mcjournal.client.WorldSaveManager;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive automated test suite for P9 (Persistence Architecture):
 * - Unified binary .jmc format (P9.1 & P9.2)
 * - Versioned save files with formatVersion = 3 (P9.3)
 * - Automatic migration from legacy JSON to binary .jmc (P9.3)
 * - Asynchronous background chunk and world saving (P9.4)
 *
 * Run with: java -ea -cp "engine/bin;engine/lib/*" com.mcjournal.test.PersistenceMigrationAndAsyncTest
 */
public class PersistenceMigrationAndAsyncTest {

    private static final File TEST_WORLD_DIR = new File("saves/test_persistence_world");

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("       RUNNING P9 (PERSISTENCE) TEST SUITE       ");
        System.out.println("=================================================");

        cleanTestDirectory();

        testP9_1_and_P9_2_BinarySaveFormat();
        testP9_3_VersionedSaveAndMigration();
        testP9_4_AsynchronousSaving();

        cleanTestDirectory();

        System.out.println("\n>>> ALL P9 PERSISTENCE TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void cleanTestDirectory() {
        WorldSaveManager.deleteWorld();
        deleteDir(TEST_WORLD_DIR);
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                f.delete();
            }
        }
        dir.delete();
    }

    private static void testP9_1_and_P9_2_BinarySaveFormat() {
        System.out.print("[P9.1 & P9.2] Binary .jmc world save & paletted chunk persistence... ");

        ChunkManager manager = new ChunkManager(4, 99999L, TEST_WORLD_DIR);
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockState(5, 10, 5, Blocks.DIAMOND_ORE.getDefaultState());
        chunk.setBlockState(5, 11, 5, Blocks.COBBLESTONE.getDefaultState());
        manager.setChunk(new ChunkPos(0, 0), chunk);

        Player player = new Player();
        player.pos.set(12.5f, 68.0f, -4.2f);
        player.yaw = 45.0f;
        player.pitch = -15.0f;
        player.health = 18;
        player.hunger = 15;
        player.selectedSlot = 2;
        player.hotbarBlocks[0] = Blocks.STONE.getLegacyId();
        player.hotbarCounts[0] = 64;

        // Perform save
        WorldSaveManager.saveWorld("Test World", "Desert", 99999L, player, 12000.0, manager);

        // Verify saves/world.jmc exists
        File jmcFile = new File("saves/world.jmc");
        assert jmcFile.exists() && jmcFile.length() > 0 : "world.jmc binary file must exist";

        // Verify saves/hardcore_world.json is NOT created (no legacy block string maps!)
        File jsonFile = new File("saves/hardcore_world.json");
        assert !jsonFile.exists() : "Legacy JSON save must not be written during modern binary save";

        // Load world back
        WorldSaveManager.SavedWorld loaded = WorldSaveManager.loadWorld();
        assert loaded != null : "WorldSaveManager.loadWorld() must successfully load binary world.jmc";
        assert loaded.formatVersion == WorldSaveManager.FORMAT_VERSION : "Format version must be " + WorldSaveManager.FORMAT_VERSION;
        assert "Test World".equals(loaded.name) : "World name mismatch";
        assert "Desert".equals(loaded.biome) : "Biome mismatch";
        assert loaded.seed == 99999L : "Seed mismatch";
        assert Math.abs(loaded.worldTime - 12000.0) < 0.001 : "World time mismatch";
        assert Math.abs(loaded.playerX - 12.5f) < 0.001f : "Player X mismatch";
        assert Math.abs(loaded.playerY - 68.0f) < 0.001f : "Player Y mismatch";
        assert Math.abs(loaded.playerZ - (-4.2f)) < 0.001f : "Player Z mismatch";
        assert loaded.health == 18 && loaded.hunger == 15 && loaded.selectedSlot == 2 : "Player stats mismatch";
        assert loaded.hotbarBlocks[0] == Blocks.STONE.getLegacyId() && loaded.hotbarCounts[0] == 64 : "Hotbar mismatch";

        // Verify chunk was persisted to .jmc region file and can be read back
        Chunk reloadedChunk = manager.getPersistence().loadFromDisk(0, 0);
        assert reloadedChunk != null : "Chunk (0, 0) must be persisted in .jmc region file";
        assert reloadedChunk.getBlockState(5, 10, 5).is(Blocks.DIAMOND_ORE) : "Diamond ore must persist in .jmc chunk";
        assert reloadedChunk.getBlockState(5, 11, 5).is(Blocks.COBBLESTONE) : "Cobblestone must persist in .jmc chunk";

        System.out.println("PASSED");
    }

    private static void testP9_3_VersionedSaveAndMigration() throws Exception {
        System.out.print("[P9.3] Versioned save file & legacy JSON migration... ");

        // Clean current binary save
        File jmcFile = new File("saves/world.jmc");
        if (jmcFile.exists()) jmcFile.delete();

        // Write simulated legacy JSON save
        File legacyFile = new File("saves/hardcore_world.json");
        legacyFile.getParentFile().mkdirs();

        String legacyJson = """
            {
              "name": "Migrated Hardcore",
              "biome": "Forest",
              "seed": 44444,
              "createdAt": "2026-09-01 10:00",
              "playerX": 10.0,
              "playerY": 65.0,
              "playerZ": 20.0,
              "playerYaw": 90.0,
              "playerPitch": 0.0,
              "health": 20,
              "hunger": 20,
              "selectedSlot": 0,
              "hotbarBlocks": [1, 2, 3, 0, 0, 0, 0, 0, 0],
              "hotbarCounts": [10, 20, 30, 0, 0, 0, 0, 0, 0],
              "worldTime": 6000.0,
              "modifiedBlockStates": {}
            }
            """;

        try (FileWriter fw = new FileWriter(legacyFile)) {
            fw.write(legacyJson);
        }

        assert legacyFile.exists() : "Legacy JSON save must exist before migration";

        // Load world: this should trigger automatic migration from JSON to binary v3 world.jmc
        WorldSaveManager.SavedWorld migrated = WorldSaveManager.loadWorld();
        assert migrated != null : "loadWorld() must successfully read and migrate legacy save";
        assert "Migrated Hardcore".equals(migrated.name);
        assert migrated.seed == 44444L;
        assert migrated.formatVersion == 3 : "Migrated save must have formatVersion = 3";

        // Verify binary world.jmc was generated
        assert jmcFile.exists() && jmcFile.length() > 0 : "world.jmc must be created during migration";

        // Verify legacy JSON was archived
        File migratedArchive = new File("saves/hardcore_world.json.migrated");
        assert migratedArchive.exists() : "hardcore_world.json.migrated archive must exist";
        assert !legacyFile.exists() : "Original hardcore_world.json must be renamed after migration";

        // Subsequent loadWorld() should read directly from binary world.jmc
        WorldSaveManager.SavedWorld reloaded = WorldSaveManager.loadWorld();
        assert reloaded != null && reloaded.formatVersion == 3;
        assert "Migrated Hardcore".equals(reloaded.name);

        System.out.println("PASSED");
    }

    private static void testP9_4_AsynchronousSaving() throws Exception {
        System.out.print("[P9.4] Asynchronous dirty chunk & metadata saving... ");

        ChunkPersistence persistence = new ChunkPersistence(TEST_WORLD_DIR);

        // Create 5 dirty chunks
        java.util.List<Chunk> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Chunk c = new Chunk(i, 0);
            c.setBlockState(0, 10, 0, Blocks.BEDROCK.getDefaultState());
            assert c.isDirty() : "Chunk must be marked dirty after modification";
            chunks.add(c);
        }

        long startNs = System.nanoTime();
        // Trigger non-blocking async save
        CompletableFuture<Void> future = persistence.saveAllDirtyAsync(chunks);
        long callDurationNs = System.nanoTime() - startNs;

        // Caller thread must return almost instantaneously (fast O(1) array copy, < 50ms)
        double callMs = callDurationNs / 1_000_000.0;
        assert callMs < 100.0 : "saveAllDirtyAsync must return immediately without blocking caller, took " + callMs + " ms";

        // Dirty flags on the active chunks must have been cleared immediately on caller thread
        for (Chunk c : chunks) {
            assert !c.isDirty() : "Active chunk dirty flag must be reset immediately during snapshotting";
        }

        // Wait for background persistence thread to finish writing to .jmc disk
        future.get(5, TimeUnit.SECONDS);

        // Verify all 5 chunks are safely readable from disk
        for (int i = 0; i < 5; i++) {
            Chunk loaded = persistence.loadFromDisk(i, 0);
            assert loaded != null : "Chunk (" + i + ", 0) must be written to disk by background worker";
            assert loaded.getBlockState(0, 10, 0).is(Blocks.BEDROCK) : "Bedrock block must be preserved on disk";
        }

        persistence.close();
        System.out.println("PASSED (non-blocking call: " + String.format("%.2f", callMs) + " ms)");
    }
}
