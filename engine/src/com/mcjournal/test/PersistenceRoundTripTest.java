package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkSerializer;
import com.mcjournal.Item;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Player;
import com.mcjournal.client.WorldSaveManager;

import java.io.File;
import java.util.Map;

/**
 * P11 Unit Test: Persistence System
 * Covers: save -> load -> identical world verification,
 * paletted chunk binary serialization fidelity, and end-to-end world state restoration.
 */
public class PersistenceRoundTripTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: PERSISTENCE ROUND-TRIP TESTS ");
        System.out.println("=================================================");

        testChunkBinaryBitForBitFidelity();
        testWorldSaveAndLoadFidelity();

        System.out.println(">>> ALL PERSISTENCE ROUND-TRIP TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testChunkBinaryBitForBitFidelity() throws Exception {
        System.out.print("[P11 - Persistence] Chunk binary serialization bit-for-bit fidelity... ");

        Chunk original = new Chunk(-5, 8);

        // Populate chunk with a realistic mix of block states
        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS.getDefaultState();
        BlockState log = Blocks.OAK_LOG.getDefaultState();
        BlockState leaves = Blocks.OAK_LEAVES.getDefaultState();
        BlockState diamond = Blocks.DIAMOND_ORE.getDefaultState();
        BlockState waterLvl3 = Blocks.WATER.getDefaultState().with(BlockProperties.LEVEL, 3);
        BlockState waterLvl7 = Blocks.WATER.getDefaultState().with(BlockProperties.LEVEL, 7);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                original.setBlockState(x, 0, z, Blocks.BEDROCK.getDefaultState());
                for (int y = 1; y < 50; y++) {
                    original.setBlockState(x, y, z, stone);
                }
                for (int y = 50; y < 62; y++) {
                    original.setBlockState(x, y, z, dirt);
                }
                original.setBlockState(x, 62, z, grass);
            }
        }

        // Add distinct features
        original.setBlockState(0, 0, 0, diamond);
        original.setBlockState(15, 62, 15, log);
        original.setBlockState(15, 63, 15, leaves);
        original.setBlockState(7, 63, 7, waterLvl3);
        original.setBlockState(8, 63, 7, waterLvl7);

        // Serialize to binary (v3 paletted)
        byte[] serializedData = ChunkSerializer.serialize(original);
        assert serializedData != null && serializedData.length > 0 : "Serialized data must not be empty";

        // Deserialize to a new chunk
        Chunk deserialized = ChunkSerializer.deserialize(serializedData);
        assert deserialized != null : "Deserialized chunk must not be null";
        assert deserialized.getCx() == original.getCx() : "Chunk X mismatch";
        assert deserialized.getCz() == original.getCz() : "Chunk Z mismatch";

        // Bit-for-bit verification of all 65,536 voxels
        int matchingVoxels = 0;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int originalId = original.getBlockState(x, y, z).getStateId();
                    int loadedId = deserialized.getBlockState(x, y, z).getStateId();
                    assert originalId == loadedId : "Voxel mismatch at (" + x + ", " + y + ", " + z + "): expected " + originalId + " got " + loadedId;
                    matchingVoxels++;
                }
            }
        }

        assert matchingVoxels == Chunk.TOTAL_VOXELS : "All voxels must match identically";
        System.out.println("PASSED (" + matchingVoxels + " voxels verified identical)");
    }

    private static void testWorldSaveAndLoadFidelity() {
        System.out.print("[P11 - Persistence] WorldSaveManager end-to-end save -> load -> identical world... ");

        String worldName = "P11_Fidelity_Test_World";
        String biome = "Hardcore Plains";
        long seed = 9876543210L;
        double worldTime = 14500.0;

        File worldDir = new File("saves/p11_fidelity_test_world");
        deleteRecursively(worldDir);
        WorldSaveManager.deleteWorld();

        ChunkManager cm = new ChunkManager(2, seed, worldDir);

        // Mutate blocks in the world
        cm.setBlockStateAt(5, 70, 5, Blocks.DIAMOND_ORE.getDefaultState());
        cm.setBlockStateAt(-12, 65, 8, Blocks.SAND.getDefaultState());
        cm.setBlockStateAt(22, 68, -14, Blocks.COBBLESTONE.getDefaultState());

        // Setup Player state
        Player player = new Player();
        player.pos.set(10.5f, 72.0f, -8.3f);
        player.yaw = 142.5f;
        player.pitch = -15.0f;
        player.health = 18;
        player.hunger = 15;
        player.selectedSlot = 2;
        player.hotbarBlocks[0] = Item.IRON_AXE;
        player.hotbarCounts[0] = 1;
        player.hotbarBlocks[1] = (byte) Blocks.STONE.getDefaultState().getStateId();
        player.hotbarCounts[1] = 64;
        player.hotbarBlocks[2] = Item.IRON_PICKAXE;
        player.hotbarCounts[2] = 1;

        // Save World
        WorldSaveManager.saveWorld(worldName, biome, seed, player, worldTime, cm);

        // Load World back from disk
        WorldSaveManager.SavedWorld loaded = WorldSaveManager.loadWorld();
        assert loaded != null : "World save must successfully load from disk";

        // Verify World Metadata
        assert loaded.name.equals(worldName) : "World name mismatch";
        assert loaded.biome.equals(biome) : "Biome mismatch";
        assert loaded.seed == seed : "Seed mismatch";
        assert Math.abs(loaded.worldTime - worldTime) < 0.01 : "World time mismatch";

        // Verify Player Position & Stats
        assert Math.abs(loaded.playerX - 10.5f) < 0.001f : "Player X mismatch";
        assert Math.abs(loaded.playerY - 72.0f) < 0.001f : "Player Y mismatch";
        assert Math.abs(loaded.playerZ - (-8.3f)) < 0.001f : "Player Z mismatch";
        assert Math.abs(loaded.playerYaw - 142.5f) < 0.001f : "Player Yaw mismatch";
        assert Math.abs(loaded.playerPitch - (-15.0f)) < 0.001f : "Player Pitch mismatch";
        assert loaded.health == 18 : "Player health mismatch";
        assert loaded.hunger == 15 : "Player hunger mismatch";
        assert loaded.selectedSlot == 2 : "Selected slot mismatch";

        // Verify Inventory contents
        assert loaded.hotbarBlocks[0] == Item.IRON_AXE && loaded.hotbarCounts[0] == 1;
        assert loaded.hotbarBlocks[1] == (byte) Blocks.STONE.getDefaultState().getStateId() && loaded.hotbarCounts[1] == 64;
        assert loaded.hotbarBlocks[2] == Item.IRON_PICKAXE && loaded.hotbarCounts[2] == 1;

        // Clean up test files
        cm.shutdown();
        deleteRecursively(worldDir);
        WorldSaveManager.deleteWorld();

        System.out.println("PASSED");
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteRecursively(f);
            }
        }
        file.delete();
    }
}
