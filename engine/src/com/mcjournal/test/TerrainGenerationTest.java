package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.Blocks;

/**
 * P11 Unit Test: Terrain System
 * Covers: same seed -> same terrain determinism, different seed -> different terrain divergence,
 * height bounds, and bedrock layer continuity.
 */
public class TerrainGenerationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: TERRAIN GENERATION TESTS     ");
        System.out.println("=================================================");

        testSameSeedDeterminism();
        testDifferentSeedDivergence();
        testTerrainHeightBoundsAndBedrock();

        System.out.println(">>> ALL TERRAIN GENERATION TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testSameSeedDeterminism() {
        System.out.print("[P11 - Terrain] Same seed -> identical voxel terrain determinism... ");

        long seed = 8847291034L;
        TerrainGenerator gen1 = new TerrainGenerator(seed);
        TerrainGenerator gen2 = new TerrainGenerator(seed);

        int[][] testChunks = {
            {0, 0}, {3, -4}, {-7, 11}, {-16, -16}
        };

        int totalVoxelsVerified = 0;

        for (int[] cp : testChunks) {
            int cx = cp[0];
            int cz = cp[1];

            Chunk chunkA = gen1.generateChunk(cx, cz);
            Chunk chunkB = gen2.generateChunk(cx, cz);

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    int wx = cx * 16 + x;
                    int wz = cz * 16 + z;

                    int heightA = gen1.computeHeight(wx, wz);
                    int heightB = gen2.computeHeight(wx, wz);
                    assert heightA == heightB : "Heightmap mismatch at (" + wx + ", " + wz + ")";

                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        int stateIdA = chunkA.getBlockState(x, y, z).getStateId();
                        int stateIdB = chunkB.getBlockState(x, y, z).getStateId();
                        assert stateIdA == stateIdB : "Voxel mismatch at (" + wx + ", " + y + ", " + wz + ")";
                        totalVoxelsVerified++;
                    }
                }
            }
        }

        System.out.println("PASSED (" + totalVoxelsVerified + " voxels verified identical)");
    }

    private static void testDifferentSeedDivergence() {
        System.out.print("[P11 - Terrain] Different seed -> distinct terrain divergence... ");

        long seedA = 1234567L;
        long seedB = 7654321L;

        TerrainGenerator genA = new TerrainGenerator(seedA);
        TerrainGenerator genB = new TerrainGenerator(seedB);

        int samplePoints = 256;
        int heightDifferences = 0;
        int voxelDifferences = 0;

        Chunk chunkA = genA.generateChunk(0, 0);
        Chunk chunkB = genB.generateChunk(0, 0);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int hA = genA.computeHeight(x, z);
                int hB = genB.computeHeight(x, z);
                if (hA != hB) heightDifferences++;

                for (int y = 50; y < 90; y++) {
                    if (chunkA.getBlockState(x, y, z).getStateId() != chunkB.getBlockState(x, y, z).getStateId()) {
                        voxelDifferences++;
                    }
                }
            }
        }

        assert heightDifferences > (samplePoints * 0.40) : "Different seeds must produce diverging heightmaps (got " + heightDifferences + "/" + samplePoints + ")";
        assert voxelDifferences > 500 : "Different seeds must produce significantly distinct voxel distributions (got " + voxelDifferences + ")";

        System.out.println("PASSED (" + heightDifferences + "/" + samplePoints + " heights diverged, " + voxelDifferences + " voxel diffs)");
    }

    private static void testTerrainHeightBoundsAndBedrock() {
        System.out.print("[P11 - Terrain] Height bounds, sea level, and bedrock floor... ");

        TerrainGenerator gen = new TerrainGenerator(424242L);
        Chunk chunk = gen.generateChunk(2, -3);

        int bedrockId = Blocks.BEDROCK.getDefaultState().getStateId();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Bedrock at Y=0
                assert chunk.getBlockState(x, 0, z).getStateId() == bedrockId : "Y=0 must always be bedrock";

                int wx = 2 * 16 + x;
                int wz = -3 * 16 + z;
                int h = gen.computeHeight(wx, wz);
                assert h >= 1 && h < Chunk.HEIGHT : "Height " + h + " must be strictly in [1, 255]";

                // If terrain is below sea level, water should fill up to SEA_LEVEL
                if (h < TerrainGenerator.SEA_LEVEL) {
                    for (int y = h + 1; y <= TerrainGenerator.SEA_LEVEL; y++) {
                        assert chunk.getBlockState(x, y, z).isWater() : "Cavity below sea level should be water at y=" + y;
                    }
                }
            }
        }

        System.out.println("PASSED");
    }
}
