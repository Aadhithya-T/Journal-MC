package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.BlockType;

public class NewBlocksWorldGenTest {

    public static void main(String[] args) {
        testBlockPropertiesAndDrops();
        testTerrainGenerationDistribution();
        System.out.println("All NewBlocksWorldGenTest assertions passed!");
    }

    private static void testBlockPropertiesAndDrops() {
        assert "gravel".equals(Blocks.GRAVEL.getId()) && Blocks.GRAVEL.getLegacyId() == 16 : "Gravel ID mismatch";
        assert "andesite".equals(Blocks.ANDESITE.getId()) && Blocks.ANDESITE.getLegacyId() == 17 : "Andesite ID mismatch";
        assert "diorite".equals(Blocks.DIORITE.getId()) && Blocks.DIORITE.getLegacyId() == 18 : "Diorite ID mismatch";
        assert "stone".equals(Blocks.STONE.getId()) && Blocks.STONE.getLegacyId() == 3 : "Stone ID mismatch";

        assert Blocks.GRAVEL.getHardness() == 0.6f : "Gravel hardness mismatch: " + Blocks.GRAVEL.getHardness();
        assert Blocks.ANDESITE.getHardness() == 1.5f : "Andesite hardness mismatch: " + Blocks.ANDESITE.getHardness();
        assert Blocks.DIORITE.getHardness() == 1.5f : "Diorite hardness mismatch: " + Blocks.DIORITE.getHardness();
        assert Blocks.STONE.getHardness() == 1.5f : "Stone hardness mismatch: " + Blocks.STONE.getHardness();

        assert Blocks.GRAVEL.getDrop(Blocks.GRAVEL.getDefaultState()) == Blocks.GRAVEL : "Gravel drop mismatch";
        assert Blocks.ANDESITE.getDrop(Blocks.ANDESITE.getDefaultState()) == Blocks.ANDESITE : "Andesite drop mismatch";
        assert Blocks.DIORITE.getDrop(Blocks.DIORITE.getDefaultState()) == Blocks.DIORITE : "Diorite drop mismatch";
        assert Blocks.STONE.getDrop(Blocks.STONE.getDefaultState()) == Blocks.COBBLESTONE : "Stone should drop Cobblestone";
    }

    private static void testTerrainGenerationDistribution() {
        TerrainGenerator gen = new TerrainGenerator(424242L);
        int stoneCount = 0;
        int andesiteCount = 0;
        int dioriteCount = 0;
        int gravelCount = 0;

        int idStone = Blocks.STONE.getDefaultState().getStateId();
        int idAndesite = Blocks.ANDESITE.getDefaultState().getStateId();
        int idDiorite = Blocks.DIORITE.getDefaultState().getStateId();
        int idGravel = Blocks.GRAVEL.getDefaultState().getStateId();

        // Sample a 4x4 chunk region
        for (int cx = -2; cx <= 1; cx++) {
            for (int cz = -2; cz <= 1; cz++) {
                Chunk chunk = gen.generateChunk(cx, cz);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = 0; y < 128; y++) {
                            int stateId = chunk.getStateId(lx, y, lz);
                            if (stateId == idStone) stoneCount++;
                            else if (stateId == idAndesite) andesiteCount++;
                            else if (stateId == idDiorite) dioriteCount++;
                            else if (stateId == idGravel) gravelCount++;
                        }
                    }
                }
            }
        }

        assert stoneCount > 1000 : "Expected abundant Stone in underground mantle, got: " + stoneCount;
        assert andesiteCount > 50 : "Expected Andesite veins in terrain, got: " + andesiteCount;
        assert dioriteCount > 50 : "Expected Diorite veins in terrain, got: " + dioriteCount;
        assert gravelCount > 50 : "Expected Gravel deposits/veins in terrain, got: " + gravelCount;
    }
}
