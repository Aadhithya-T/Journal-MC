package com.mcjournal.client;

import java.nio.ByteBuffer;
import java.util.Random;

public class ProceduralTextureGenerator {
    public static final int TILE_SIZE = 64;

    public static void generateAtlas(ByteBuffer atlasBuffer, int atlasGrid) {
        int atlasSize = atlasGrid * TILE_SIZE;

        for (int slot = 0; slot < 16; slot++) {
            int col = slot % atlasGrid;
            int row = slot / atlasGrid;

            int slotX = col * TILE_SIZE;
            int slotY = ((atlasGrid - 1) - row) * TILE_SIZE;

            byte[] tileData = generateTile(slot);

            for (int y = 0; y < TILE_SIZE; y++) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    int srcIdx = (y * TILE_SIZE + x) * 4;
                    int dstIdx = ((slotY + y) * atlasSize + (slotX + x)) * 4;

                    atlasBuffer.put(dstIdx, tileData[srcIdx]);         // R
                    atlasBuffer.put(dstIdx + 1, tileData[srcIdx + 1]); // G
                    atlasBuffer.put(dstIdx + 2, tileData[srcIdx + 2]); // B
                    atlasBuffer.put(dstIdx + 3, tileData[srcIdx + 3]); // A
                }
            }
        }
    }

    private static byte[] generateTile(int slot) {
        byte[] tile = new byte[TILE_SIZE * TILE_SIZE * 4];
        Random rand = new Random(slot * 31337L + 42L);

        switch (slot) {
            case 0 -> generateGrassTop(tile, rand);
            case 1 -> generateGrassSide(tile, rand);
            case 2 -> generateDirt(tile, rand);
            case 3 -> generateStone(tile, rand);
            case 4 -> generateCobblestone(tile, rand);
            case 5 -> generateSand(tile, rand);
            case 6 -> generateBedrock(tile, rand);
            case 7 -> generateOakLogSide(tile, rand);
            case 8 -> generateOakLogTop(tile, rand);
            case 9 -> generateOakLeaves(tile, rand);
            case 10 -> generateDiamondOre(tile, rand);
            case 11 -> generateWater(tile, rand);
            case 12 -> generateTallGrass(tile, rand);
            case 13 -> generatePoppy(tile, rand);
            case 14 -> generateDandelion(tile, rand);
            case 15 -> generateBirchLogSide(tile, rand);
            default -> fillSolid(tile, 128, 128, 128, 255);
        }

        return tile;
    }

    // 0: Grass Top (Rich, natural earthy green - tuned to avoid neon overexposure)
    private static void generateGrassTop(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int blockX = x / 4;
                int blockY = y / 4;
                int blockNoise = (int) (Math.sin(blockX * 0.7) * Math.cos(blockY * 0.7) * 8.0);
                
                int r = Math.clamp(74 + blockNoise, 0, 255);
                int g = Math.clamp(136 + blockNoise * 2, 0, 255);
                int b = Math.clamp(48 + blockNoise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }

        // Scatter organic 2x3 blade clumps
        for (int i = 0; i < 36; i++) {
            int gx = rand.nextInt(TILE_SIZE - 4);
            int gy = rand.nextInt(TILE_SIZE - 4);
            int shade = rand.nextInt(20) - 10;

            for (int dy = 0; dy < 3; dy++) {
                for (int dx = 0; dx < 2; dx++) {
                    int r = Math.clamp(64 + shade, 0, 255);
                    int g = Math.clamp(124 + shade * 2, 0, 255);
                    int b = Math.clamp(38 + shade, 0, 255);
                    setPixel(tile, gx + dx, gy + dy, r, g, b, 255);
                }
            }
        }
    }

    // 1: Grass Side (Dirt with organic hanging grass fringe)
    private static void generateGrassSide(byte[] tile, Random rand) {
        generateDirt(tile, rand);

        for (int x = 0; x < TILE_SIZE; x++) {
            int depth = 14 + (int) (Math.sin(x * 0.35) * 3) + ((x % 8 == 0) ? 3 : 0);
            for (int y = TILE_SIZE - depth; y < TILE_SIZE; y++) {
                int blockX = x / 4;
                int blockY = y / 4;
                int blockNoise = (int) (Math.sin(blockX * 0.7) * 8.0);
                int r = Math.clamp(74 + blockNoise, 0, 255);
                int g = Math.clamp(136 + blockNoise * 2, 0, 255);
                int b = Math.clamp(48 + blockNoise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 2: Dirt (Rich earthen soil with structured pebble clusters)
    private static void generateDirt(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;
                int noise = (int) (Math.sin(bx * 0.8) * Math.cos(by * 0.8) * 12.0);
                int r = Math.clamp(126 + noise, 0, 255);
                int g = Math.clamp(88 + noise, 0, 255);
                int b = Math.clamp(52 + noise / 2, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 3: Stone (Smooth slate gray with natural cleavage lines)
    private static void generateStone(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;
                int noise = (int) (Math.sin(bx * 0.6) * Math.cos(by * 0.6) * 10.0);
                int r = Math.clamp(122 + noise, 0, 255);
                int g = Math.clamp(122 + noise, 0, 255);
                int b = Math.clamp(124 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 4: Cobblestone (Masonry stones separated by dark mortar)
    private static void generateCobblestone(byte[] tile, Random rand) {
        generateStone(tile, rand);

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                boolean mortar = (y % 16 == 0 && ((y / 16) % 2 == 0 ? x % 16 == 0 : (x + 8) % 16 == 0))
                              || (y % 16 == 0) || (x % 32 == 0);
                if (mortar) {
                    setPixel(tile, x, y, 68, 68, 72, 255);
                }
            }
        }
    }

    // 5: Sand (Warm golden grains)
    private static void generateSand(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;
                int noise = (int) (Math.sin(bx * 0.5) * 8.0);
                int r = Math.clamp(218 + noise, 0, 255);
                int g = Math.clamp(204 + noise, 0, 255);
                int b = Math.clamp(152 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 6: Bedrock (Dark obsidian fractures)
    private static void generateBedrock(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int val = ((x / 4) ^ (y / 4)) % 3 == 0 ? 32 : 54;
                setPixel(tile, x, y, val, val, val, 255);
            }
        }
    }

    // 7: Oak Log Side (Vertical tree bark)
    private static void generateOakLogSide(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int strip = (x / 6) % 3;
                int r = strip == 0 ? 108 : (strip == 1 ? 92 : 78);
                int g = strip == 0 ? 82 : (strip == 1 ? 68 : 56);
                int b = strip == 0 ? 46 : (strip == 1 ? 38 : 30);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 8: Oak Log Top (Concentric tree rings)
    private static void generateOakLogTop(byte[] tile, Random rand) {
        int cx = TILE_SIZE / 2;
        int cy = TILE_SIZE / 2;

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                if (dist > 28) {
                    setPixel(tile, x, y, 78, 55, 29, 255);
                } else {
                    int ring = ((int) dist / 5) % 2;
                    int r = ring == 0 ? 194 : 172;
                    int g = ring == 0 ? 154 : 136;
                    int b = ring == 0 ? 101 : 88;
                    setPixel(tile, x, y, r, g, b, 255);
                }
            }
        }
    }

    // 9: Oak Leaves (Dense, lush, volumetric foliage with natural random notch cutouts - STRICT alpha 0 or 255)
    private static void generateOakLeaves(byte[] tile, Random rand) {
        // Base solid lush green foliage
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;
                int tone = ((bx * 3 + by * 5 + (bx ^ by)) % 4);
                
                int r = (tone == 0) ? 48 : ((tone == 1) ? 58 : ((tone == 2) ? 42 : 68));
                int g = (tone == 0) ? 122 : ((tone == 1) ? 138 : ((tone == 2) ? 110 : 148));
                int b = (tone == 0) ? 26 : ((tone == 1) ? 32 : ((tone == 2) ? 22 : 38));
                
                // Edge of 4x4 leaf sub-blocks shaded for depth
                if (x % 4 == 0 || y % 4 == 0) {
                    r = (int)(r * 0.85);
                    g = (int)(g * 0.85);
                    b = (int)(b * 0.85);
                }
                setPixel(tile, x, y, r, g, b, 255);
            }
        }

        // Carve small, organic, scattered leaf cutouts (approx 12% transparent holes)
        for (int by = 0; by < 16; by++) {
            for (int bx = 0; bx < 16; bx++) {
                // Pseudo-random deterministic scatter for holes (NO grid banding)
                int hash = ((bx * 37 + by * 53) ^ 0x4B3A) % 17;
                if (hash <= 2) {
                    int holeW = 2 + (hash % 2);
                    int holeH = 2 + ((hash + 1) % 2);
                    for (int dy = 0; dy < holeH; dy++) {
                        for (int dx = 0; dx < holeW; dx++) {
                            int px = bx * 4 + dx;
                            int py = by * 4 + dy;
                            if (px < TILE_SIZE && py < TILE_SIZE) {
                                setPixel(tile, px, py, 0, 0, 0, 0); // 100% transparent cutout hole
                            }
                        }
                    }
                }
            }
        }
    }

    // 10: Diamond Ore (Stone embedded with cyan gems)
    private static void generateDiamondOre(byte[] tile, Random rand) {
        generateStone(tile, rand);

        int[][] gems = {{16, 20}, {40, 16}, {24, 44}, {46, 42}, {32, 28}};
        for (int[] g : gems) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) <= 4) {
                        int px = g[0] + dx;
                        int py = g[1] + dy;
                        if (px >= 0 && px < TILE_SIZE && py >= 0 && py < TILE_SIZE) {
                            boolean core = Math.abs(dx) + Math.abs(dy) <= 1;
                            int r = core ? 210 : 75;
                            int gr = core ? 255 : 235;
                            int b = core ? 255 : 245;
                            setPixel(tile, px, py, r, gr, b, 255);
                        }
                    }
                }
            }
        }
    }

    // 11: Water (Vibrant crystalline animated base)
    private static void generateWater(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int wave = (int) (Math.sin(x * 0.25) * Math.cos(y * 0.25) * 8.0);
                int r = Math.clamp(32 + wave / 2, 0, 255);
                int g = Math.clamp(102 + wave, 0, 255);
                int b = Math.clamp(204 + wave, 0, 255);
                setPixel(tile, x, y, r, g, b, 210);
            }
        }
    }

    // 12: Tall Grass (STRICT alpha 0 or 255)
    private static void generateTallGrass(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);
        int[] blades = {14, 22, 30, 38, 46, 52};
        for (int bx : blades) {
            int h = 36 + rand.nextInt(18);
            for (int y = 0; y < h; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int px = bx + dx + (int)(Math.sin(y * 0.15) * 3);
                    if (px >= 0 && px < TILE_SIZE) {
                        setPixel(tile, px, y, 78, 154, 42, 255);
                    }
                }
            }
        }
    }

    // 13: Poppy (STRICT alpha 0 or 255)
    private static void generatePoppy(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);
        // Green stem
        for (int y = 0; y < 40; y++) {
            setPixel(tile, 31, y, 64, 132, 38, 255);
            setPixel(tile, 32, y, 64, 132, 38, 255);
        }
        // Red flower head
        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                if (dx * dx + dy * dy <= 56) {
                    boolean center = (dx * dx + dy * dy <= 8);
                    int r = center ? 48 : 218;
                    int g = center ? 32 : 36;
                    int b = center ? 24 : 36;
                    setPixel(tile, 32 + dx, 44 + dy, r, g, b, 255);
                }
            }
        }
    }

    // 14: Dandelion (STRICT alpha 0 or 255)
    private static void generateDandelion(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);
        for (int y = 0; y < 34; y++) {
            setPixel(tile, 31, y, 64, 132, 38, 255);
            setPixel(tile, 32, y, 64, 132, 38, 255);
        }
        for (int dy = -7; dy <= 7; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                if (dx * dx + dy * dy <= 42) {
                    boolean core = (dx * dx + dy * dy <= 6);
                    int r = core ? 245 : 230;
                    int g = core ? 208 : 185;
                    int b = core ? 38 : 16;
                    setPixel(tile, 32 + dx, 38 + dy, r, g, b, 255);
                }
            }
        }
    }

    // 15: Birch Log Side
    private static void generateBirchLogSide(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                setPixel(tile, x, y, 224, 224, 218, 255);
            }
        }
        int[][] markings = {{12, 16, 14, 5}, {36, 32, 18, 6}, {18, 48, 12, 4}, {44, 10, 10, 4}};
        for (int[] m : markings) {
            for (int dy = 0; dy < m[3]; dy++) {
                for (int dx = 0; dx < m[2]; dx++) {
                    int px = (m[0] + dx) % TILE_SIZE;
                    int py = m[1] + dy;
                    if (py < TILE_SIZE) {
                        setPixel(tile, px, py, 42, 42, 44, 255);
                    }
                }
            }
        }
    }

    private static void fillSolid(byte[] tile, int r, int g, int b, int a) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                setPixel(tile, x, y, r, g, b, a);
            }
        }
    }

    private static void setPixel(byte[] tile, int x, int y, int r, int g, int b, int a) {
        if (x < 0 || x >= TILE_SIZE || y < 0 || y >= TILE_SIZE) return;
        int idx = (y * TILE_SIZE + x) * 4;
        tile[idx] = (byte) r;
        tile[idx + 1] = (byte) g;
        tile[idx + 2] = (byte) b;
        tile[idx + 3] = (byte) a;
    }
}
