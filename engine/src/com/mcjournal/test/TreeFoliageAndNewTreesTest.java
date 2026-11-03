package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.Item;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Axis;
import com.mcjournal.client.ProceduralTextureGenerator;
import com.mcjournal.client.TextureAtlas;

import java.nio.ByteBuffer;

public class TreeFoliageAndNewTreesTest {

    public static void main(String[] args) {
        testBlockPropertiesAndRegistration();
        testAxisOrientationsAndDrops();
        testToolEffectiveness();
        testTextureAtlasFoliagePalettes();
        testTreeHeightsAndSpruceDominance();
        testWorldGenerationTreeSpecies();
        System.out.println("All TreeFoliageAndNewTreesTest assertions passed successfully!");
    }

    private static void testBlockPropertiesAndRegistration() {
        assert Blocks.SPRUCE_LOG.getLegacyId() == 19 : "Spruce Log ID mismatch: " + Blocks.SPRUCE_LOG.getLegacyId();
        assert Blocks.SPRUCE_LEAVES.getLegacyId() == 20 : "Spruce Leaves ID mismatch: " + Blocks.SPRUCE_LEAVES.getLegacyId();
        assert Blocks.DARK_OAK_LOG.getLegacyId() == 21 : "Dark Oak Log ID mismatch: " + Blocks.DARK_OAK_LOG.getLegacyId();
        assert Blocks.DARK_OAK_LEAVES.getLegacyId() == 22 : "Dark Oak Leaves ID mismatch: " + Blocks.DARK_OAK_LEAVES.getLegacyId();

        assert Blocks.SPRUCE_LOG.isLog() : "Spruce Log must be recognized as log";
        assert Blocks.DARK_OAK_LOG.isLog() : "Dark Oak Log must be recognized as log";
        assert Blocks.OAK_LOG.isLog() : "Oak Log must be recognized as log";
        assert Blocks.BIRCH_LOG.isLog() : "Birch Log must be recognized as log";

        assert Blocks.SPRUCE_LEAVES.isLeaves() : "Spruce Leaves must be recognized as leaves";
        assert Blocks.DARK_OAK_LEAVES.isLeaves() : "Dark Oak Leaves must be recognized as leaves";
        assert Blocks.OAK_LEAVES.isLeaves() : "Oak Leaves must be recognized as leaves";
        assert Blocks.BIRCH_LEAVES.isLeaves() : "Birch Leaves must be recognized as leaves";

        assert Blocks.SPRUCE_LEAVES.isTransparent() : "Spruce leaves must be transparent (cutout)";
        assert Blocks.DARK_OAK_LEAVES.isTransparent() : "Dark oak leaves must be transparent (cutout)";

        // State helpers
        BlockState spruceLeafState = Blocks.SPRUCE_LEAVES.getDefaultState();
        assert spruceLeafState.isLeaves() : "BlockState.isLeaves() failed on spruce";
        assert !spruceLeafState.isLog() : "Spruce leaves should not be log";

        BlockState darkOakLogState = Blocks.DARK_OAK_LOG.getDefaultState();
        assert darkOakLogState.isLog() : "BlockState.isLog() failed on dark oak";
        assert !darkOakLogState.isLeaves() : "Dark oak log should not be leaves";
    }

    private static void testAxisOrientationsAndDrops() {
        // Drops
        assert Blocks.SPRUCE_LOG.getDrop(Blocks.SPRUCE_LOG.getDefaultState()) == Blocks.SPRUCE_LOG;
        assert Blocks.SPRUCE_LEAVES.getDrop(Blocks.SPRUCE_LEAVES.getDefaultState()) == Blocks.SPRUCE_LEAVES;
        assert Blocks.DARK_OAK_LOG.getDrop(Blocks.DARK_OAK_LOG.getDefaultState()) == Blocks.DARK_OAK_LOG;
        assert Blocks.DARK_OAK_LEAVES.getDrop(Blocks.DARK_OAK_LEAVES.getDefaultState()) == Blocks.DARK_OAK_LEAVES;

        // Axis orientations
        BlockState spruceY = Blocks.SPRUCE_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.Y);
        BlockState spruceX = Blocks.SPRUCE_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.X);
        BlockState darkOakZ = Blocks.DARK_OAK_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.Z);

        // Spruce log: side texture slot = 23, top ring slot = 24
        assert Blocks.SPRUCE_LOG.getTextureSlot(spruceY, 2) == 24 : "Spruce Y top should be ring (24)";
        assert Blocks.SPRUCE_LOG.getTextureSlot(spruceY, 0) == 23 : "Spruce Y side should be bark (23)";
        assert Blocks.SPRUCE_LOG.getTextureSlot(spruceX, 0) == 24 : "Spruce X face 0 should be ring (24)";
        assert Blocks.SPRUCE_LOG.getTextureSlot(spruceX, 2) == 23 : "Spruce X face 2 should be bark (23)";

        // Dark oak log: side texture slot = 26, top ring slot = 27
        assert Blocks.DARK_OAK_LOG.getTextureSlot(darkOakZ, 4) == 27 : "Dark Oak Z face 4 should be ring (27)";
        assert Blocks.DARK_OAK_LOG.getTextureSlot(darkOakZ, 2) == 26 : "Dark Oak Z face 2 should be bark (26)";

        // Leaves texture slots: Birch = 22, Spruce = 25, Dark Oak = 28, Oak = 9
        assert Blocks.BIRCH_LEAVES.getTextureSlot(Blocks.BIRCH_LEAVES.getDefaultState(), 0) == 22 : "Birch leaves slot mismatch";
        assert Blocks.SPRUCE_LEAVES.getTextureSlot(Blocks.SPRUCE_LEAVES.getDefaultState(), 0) == 25 : "Spruce leaves slot mismatch";
        assert Blocks.DARK_OAK_LEAVES.getTextureSlot(Blocks.DARK_OAK_LEAVES.getDefaultState(), 0) == 28 : "Dark oak leaves slot mismatch";
        assert Blocks.OAK_LEAVES.getTextureSlot(Blocks.OAK_LEAVES.getDefaultState(), 0) == 9 : "Oak leaves slot mismatch";
    }

    private static void testToolEffectiveness() {
        assert Item.isAxeEffective(Blocks.OAK_LOG) : "Axe should be effective on Oak Log";
        assert Item.isAxeEffective(Blocks.BIRCH_LOG) : "Axe should be effective on Birch Log";
        assert Item.isAxeEffective(Blocks.SPRUCE_LOG) : "Axe should be effective on Spruce Log";
        assert Item.isAxeEffective(Blocks.DARK_OAK_LOG) : "Axe should be effective on Dark Oak Log";

        assert Item.isAxeEffective(Blocks.OAK_LEAVES) : "Axe should be effective on Oak Leaves";
        assert Item.isAxeEffective(Blocks.BIRCH_LEAVES) : "Axe should be effective on Birch Leaves";
        assert Item.isAxeEffective(Blocks.SPRUCE_LEAVES) : "Axe should be effective on Spruce Leaves";
        assert Item.isAxeEffective(Blocks.DARK_OAK_LEAVES) : "Axe should be effective on Dark Oak Leaves";

        assert !Item.isAxeEffective(Blocks.STONE) : "Axe should not be effective on Stone";
        assert !Item.isAxeEffective(Blocks.DIRT) : "Axe should not be effective on Dirt";
    }

    private static void testTextureAtlasFoliagePalettes() {
        ByteBuffer buf = ByteBuffer.allocateDirect(TextureAtlas.ATLAS_SIZE * TextureAtlas.ATLAS_SIZE * 4);
        ProceduralTextureGenerator.generateAtlas(buf, TextureAtlas.ATLAS_GRID);

        // Verify leaf textures have valid alpha channel (strict binary cutout 0 or 255)
        int[] leafSlots = {9, 22, 25, 28};
        for (int slot : leafSlots) {
            int col = slot % TextureAtlas.ATLAS_GRID;
            int row = slot / TextureAtlas.ATLAS_GRID;
            int slotX = col * ProceduralTextureGenerator.TILE_SIZE;
            int slotY = ((TextureAtlas.ATLAS_GRID - 1) - row) * ProceduralTextureGenerator.TILE_SIZE;

            int solidCount = 0;
            int cutoutCount = 0;

            for (int y = 0; y < ProceduralTextureGenerator.TILE_SIZE; y++) {
                for (int x = 0; x < ProceduralTextureGenerator.TILE_SIZE; x++) {
                    int idx = ((slotY + y) * TextureAtlas.ATLAS_SIZE + (slotX + x)) * 4;
                    int a = buf.get(idx + 3) & 0xFF;
                    assert a == 0 || a == 255 : "Alpha must be strict binary (0 or 255) for slot " + slot + ", got: " + a;
                    if (a == 255) solidCount++;
                    else cutoutCount++;
                }
            }

            assert solidCount > 2500 : "Leaves slot " + slot + " must have solid pixels, found: " + solidCount;
            assert cutoutCount > 50 : "Leaves slot " + slot + " must have cutout holes, found: " + cutoutCount;
        }

        // Verify distinct leaf color identities:
        // Birch leaves (slot 22) should be lighter and more yellowish than Dark Oak (slot 28)
        int birchR = sampleAverageChannel(buf, 22, 0);
        int birchG = sampleAverageChannel(buf, 22, 1);
        int darkOakG = sampleAverageChannel(buf, 28, 1);

        assert birchG > darkOakG : "Birch foliage must be significantly brighter green than dark oak (" + birchG + " vs " + darkOakG + ")";
        assert birchR > 60 : "Birch foliage must have warm yellow-green tint, R: " + birchR;

        // Spruce leaves (slot 25) cool blue-green tint: blue channel ratio to red
        int spruceR = sampleAverageChannel(buf, 25, 0);
        int spruceB = sampleAverageChannel(buf, 25, 2);
        assert spruceB > spruceR : "Spruce conifer foliage must have cool blue-green undertone (B: " + spruceB + " > R: " + spruceR + ")";
    }

    private static int sampleAverageChannel(ByteBuffer buf, int slot, int channelOffset) {
        int col = slot % TextureAtlas.ATLAS_GRID;
        int row = slot / TextureAtlas.ATLAS_GRID;
        int slotX = col * ProceduralTextureGenerator.TILE_SIZE;
        int slotY = ((TextureAtlas.ATLAS_GRID - 1) - row) * ProceduralTextureGenerator.TILE_SIZE;

        long sum = 0;
        int count = 0;
        for (int y = 0; y < ProceduralTextureGenerator.TILE_SIZE; y++) {
            for (int x = 0; x < ProceduralTextureGenerator.TILE_SIZE; x++) {
                int idx = ((slotY + y) * TextureAtlas.ATLAS_SIZE + (slotX + x)) * 4;
                int a = buf.get(idx + 3) & 0xFF;
                if (a == 255) {
                    sum += (buf.get(idx + channelOffset) & 0xFF);
                    count++;
                }
            }
        }
        return count > 0 ? (int)(sum / count) : 0;
    }

    private static void testTreeHeightsAndSpruceDominance() {
        Chunk testChunk = new Chunk(0, 0);
        TerrainGenerator gen = new TerrainGenerator(12345L);

        // Plant each tree type on a flat test chunk column
        // ty = 64
        int ty4 = gen.computeHeight(4, 4);
        testChunk.setStateId(4, ty4, 4, Blocks.GRASS.getDefaultState().getStateId());
        gen.plantTree(testChunk, 0, 0, 4, 4, TerrainGenerator.TreeType.OAK, 0, 16, 16);
        int oakHeight = measureTreeHeight(testChunk, 4, 4, ty4);

        int ty8 = gen.computeHeight(8, 4);
        testChunk.setStateId(8, ty8, 4, Blocks.GRASS.getDefaultState().getStateId());
        gen.plantTree(testChunk, 0, 0, 8, 4, TerrainGenerator.TreeType.BIRCH, 0, 16, 16);
        int birchHeight = measureTreeHeight(testChunk, 8, 4, ty8);

        int ty12 = gen.computeHeight(12, 4);
        testChunk.setStateId(12, ty12, 4, Blocks.GRASS.getDefaultState().getStateId());
        gen.plantTree(testChunk, 0, 0, 12, 4, TerrainGenerator.TreeType.DARK_OAK, 0, 16, 16);
        int darkOakHeight = measureTreeHeight(testChunk, 12, 4, ty12);

        int tySpruce = gen.computeHeight(8, 12);
        testChunk.setStateId(8, tySpruce, 12, Blocks.GRASS.getDefaultState().getStateId());
        gen.plantTree(testChunk, 0, 0, 8, 12, TerrainGenerator.TreeType.SPRUCE, 0, 16, 16);
        int spruceHeight = measureTreeHeight(testChunk, 8, 12, tySpruce);

        System.out.println(String.format("[Tree Heights] Oak: %d blocks | Birch: %d blocks | Dark Oak: %d blocks | Spruce: %d blocks",
                oakHeight, birchHeight, darkOakHeight, spruceHeight));

        assert oakHeight >= 5 && oakHeight <= 8 : "Oak tree height out of expected range: " + oakHeight;
        assert birchHeight >= 6 && birchHeight <= 9 : "Birch tree height out of expected range: " + birchHeight;
        assert darkOakHeight >= 7 && darkOakHeight <= 11 : "Dark oak tree height out of expected range: " + darkOakHeight;
        assert spruceHeight >= 13 && spruceHeight <= 17 : "Spruce tree height out of expected range: " + spruceHeight;

        // Core requirement: "whilst spruce being the tallest of them all"
        assert spruceHeight > oakHeight : "Spruce (" + spruceHeight + ") must be taller than Oak (" + oakHeight + ")";
        assert spruceHeight > birchHeight : "Spruce (" + spruceHeight + ") must be taller than Birch (" + birchHeight + ")";
        assert spruceHeight > darkOakHeight : "Spruce (" + spruceHeight + ") must be taller than Dark Oak (" + darkOakHeight + ")";

        // Detailed verification of 2x2 trunk and 8x8 canopy spread for Dark Oak
        testDarkOak2x2Structure(gen);
    }

    private static void testDarkOak2x2Structure(TerrainGenerator gen) {
        Chunk darkOakChunk = new Chunk(0, 0);
        int tyCenter = gen.computeHeight(7, 7);
        darkOakChunk.setStateId(7, tyCenter, 7, Blocks.GRASS.getDefaultState().getStateId());
        darkOakChunk.setStateId(8, tyCenter, 7, Blocks.GRASS.getDefaultState().getStateId());
        darkOakChunk.setStateId(7, tyCenter, 8, Blocks.GRASS.getDefaultState().getStateId());
        darkOakChunk.setStateId(8, tyCenter, 8, Blocks.GRASS.getDefaultState().getStateId());
        gen.plantTree(darkOakChunk, 0, 0, 7, 7, TerrainGenerator.TreeType.DARK_OAK, 0, 16, 16);

        int idDarkOakLog = Blocks.DARK_OAK_LOG.getDefaultState().getStateId();
        int idDarkOakLeaves = Blocks.DARK_OAK_LEAVES.getDefaultState().getStateId();

        // 1. Verify 2x2 trunk
        assert darkOakChunk.getStateId(7, tyCenter + 1, 7) == idDarkOakLog : "Dark Oak trunk (7, 7) must be log";
        assert darkOakChunk.getStateId(8, tyCenter + 1, 7) == idDarkOakLog : "Dark Oak trunk (8, 7) must be log";
        assert darkOakChunk.getStateId(7, tyCenter + 1, 8) == idDarkOakLog : "Dark Oak trunk (7, 8) must be log";
        assert darkOakChunk.getStateId(8, tyCenter + 1, 8) == idDarkOakLog : "Dark Oak trunk (8, 8) must be log";

        // 2. Verify 8x8 canopy width
        int minLeafX = 16, maxLeafX = -1;
        int minLeafZ = 16, maxLeafZ = -1;
        for (int y = tyCenter + 1; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (darkOakChunk.getStateId(x, y, z) == idDarkOakLeaves) {
                        if (x < minLeafX) minLeafX = x;
                        if (x > maxLeafX) maxLeafX = x;
                        if (z < minLeafZ) minLeafZ = z;
                        if (z > maxLeafZ) maxLeafZ = z;
                    }
                }
            }
        }
        int canopyWidthX = maxLeafX - minLeafX + 1;
        int canopyWidthZ = maxLeafZ - minLeafZ + 1;
        System.out.println(String.format("[Dark Oak Structure] Trunk: 2x2 | Canopy Spread: %dx%d blocks", canopyWidthX, canopyWidthZ));
        assert canopyWidthX >= 8 && canopyWidthZ >= 8 : "Dark Oak canopy must be at least 8x8 wide, got: " + canopyWidthX + "x" + canopyWidthZ;
    }

    private static int measureTreeHeight(Chunk chunk, int tx, int tz, int groundY) {
        int highestY = groundY;
        for (int y = groundY + 1; y < Chunk.HEIGHT; y++) {
            int stateId = chunk.getStateId(tx, y, tz);
            if (stateId != Blocks.AIR.getDefaultState().getStateId()) {
                highestY = y;
            }
        }
        return highestY - groundY;
    }

    private static void testWorldGenerationTreeSpecies() {
        TerrainGenerator gen = new TerrainGenerator(987654321L);

        int spruceLogs = 0;
        int spruceLeaves = 0;
        int darkOakLogs = 0;
        int darkOakLeaves = 0;
        int birchLogs = 0;
        int birchLeaves = 0;
        int oakLogs = 0;
        int oakLeaves = 0;

        int idSpruceLog = Blocks.SPRUCE_LOG.getDefaultState().getStateId();
        int idSpruceLeaves = Blocks.SPRUCE_LEAVES.getDefaultState().getStateId();
        int idDarkOakLog = Blocks.DARK_OAK_LOG.getDefaultState().getStateId();
        int idDarkOakLeaves = Blocks.DARK_OAK_LEAVES.getDefaultState().getStateId();
        int idBirchLog = Blocks.BIRCH_LOG.getDefaultState().getStateId();
        int idBirchLeaves = Blocks.BIRCH_LEAVES.getDefaultState().getStateId();
        int idOakLog = Blocks.OAK_LOG.getDefaultState().getStateId();
        int idOakLeaves = Blocks.OAK_LEAVES.getDefaultState().getStateId();

        // Sample a wide forest region
        for (int cx = 2; cx <= 7; cx++) {
            for (int cz = 2; cz <= 7; cz++) {
                Chunk chunk = gen.generateChunk(cx, cz);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = 50; y < 140; y++) {
                            int sid = chunk.getStateId(lx, y, lz);
                            if (sid == idSpruceLog) spruceLogs++;
                            else if (sid == idSpruceLeaves) spruceLeaves++;
                            else if (sid == idDarkOakLog) darkOakLogs++;
                            else if (sid == idDarkOakLeaves) darkOakLeaves++;
                            else if (sid == idBirchLog) birchLogs++;
                            else if (sid == idBirchLeaves) birchLeaves++;
                            else if (sid == idOakLog) oakLogs++;
                            else if (sid == idOakLeaves) oakLeaves++;
                        }
                    }
                }
            }
        }

        System.out.println(String.format("[World Generation Trees] Oak: %d logs / %d leaves | Birch: %d logs / %d leaves | Dark Oak: %d logs / %d leaves | Spruce: %d logs / %d leaves",
                oakLogs, oakLeaves, birchLogs, birchLeaves, darkOakLogs, darkOakLeaves, spruceLogs, spruceLeaves));

        assert oakLogs > 0 && oakLeaves > 0 : "Expected natural Oak trees in world";
        assert birchLogs > 0 && birchLeaves > 0 : "Expected natural Birch trees in world";
        assert darkOakLogs > 0 && darkOakLeaves > 0 : "Expected natural Dark Oak trees in world";
        assert spruceLogs > 0 && spruceLeaves > 0 : "Expected natural Spruce trees in world";
    }
}
