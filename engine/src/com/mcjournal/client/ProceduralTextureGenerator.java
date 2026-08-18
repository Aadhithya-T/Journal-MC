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

    // 0: Grass Top (Rich vibrant green with blade noise)
    private static void generateGrassTop(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(35) - 17;
                int r = Math.clamp(92 + noise, 0, 255);
                int g = Math.clamp(164 + noise * 2, 0, 255);
                int b = Math.clamp(48 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 1: Grass Side (Dirt with organic hanging grass fringe)
    private static void generateGrassSide(byte[] tile, Random rand) {
        generateDirt(tile, rand);

        // Green fringe on top 14-22 pixels with jagged overhangs
        for (int x = 0; x < TILE_SIZE; x++) {
            int depth = 16 + (int) (Math.sin(x * 0.35) * 4) + rand.nextInt(4);
            for (int y = TILE_SIZE - depth; y < TILE_SIZE; y++) {
                int noise = rand.nextInt(30) - 15;
                int r = Math.clamp(92 + noise, 0, 255);
                int g = Math.clamp(164 + noise * 2, 0, 255);
                int b = Math.clamp(48 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 2: Dirt (Loam brown with soil grain and small pebbles)
    private static void generateDirt(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(30) - 15;
                int r = Math.clamp(134 + noise, 0, 255);
                int g = Math.clamp(96 + noise, 0, 255);
                int b = Math.clamp(67 + (noise / 2), 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 3: Stone (Natural granite gray with fine mineral variation)
    private static void generateStone(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(26) - 13;
                int v = Math.clamp(120 + noise, 0, 255);
                setPixel(tile, x, y, v, v, v, 255);
            }
        }
    }

    // 4: Cobblestone (Interlocking stone fragments with mortar borders)
    private static void generateCobblestone(byte[] tile, Random rand) {
        generateStone(tile, rand);
        // Mortar lines
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                if ((x % 16 == 0 || y % 16 == 0) && rand.nextFloat() > 0.3f) {
                    setPixel(tile, x, y, 60, 60, 60, 255);
                }
            }
        }
    }

    // 5: Sand (Warm golden sand grains)
    private static void generateSand(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(20) - 10;
                int r = Math.clamp(219 + noise, 0, 255);
                int g = Math.clamp(211 + noise, 0, 255);
                int b = Math.clamp(160 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 255);
            }
        }
    }

    // 6: Bedrock (Dark obsidian / slate pattern)
    private static void generateBedrock(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(40) - 20;
                int v = Math.clamp(35 + noise, 0, 255);
                setPixel(tile, x, y, v, v, v, 255);
            }
        }
    }

    // 7: Oak Log Side (Vertical bark grooves)
    private static void generateOakLogSide(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int column = (x / 8) % 2;
                int noise = rand.nextInt(20) - 10;
                int baseR = column == 0 ? 103 : 80;
                int baseG = column == 0 ? 74 : 56;
                int baseB = column == 0 ? 39 : 28;

                int r = Math.clamp(baseR + noise, 0, 255);
                int g = Math.clamp(baseG + noise, 0, 255);
                int b = Math.clamp(baseB + noise, 0, 255);
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
                    // Dark outer bark
                    setPixel(tile, x, y, 78, 55, 29, 255);
                } else {
                    int ring = ((int) dist / 4) % 2;
                    int r = ring == 0 ? 194 : 172;
                    int g = ring == 0 ? 154 : 136;
                    int b = ring == 0 ? 101 : 88;
                    setPixel(tile, x, y, r, g, b, 255);
                }
            }
        }
    }

    // 9: Oak Leaves (Foliage with alpha transparency cutouts)
    private static void generateOakLeaves(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                if (rand.nextFloat() < 0.22f) {
                    // Transparent foliage cutout
                    setPixel(tile, x, y, 0, 0, 0, 0);
                } else {
                    int noise = rand.nextInt(35) - 17;
                    int r = Math.clamp(65 + noise, 0, 255);
                    int g = Math.clamp(145 + noise * 2, 0, 255);
                    int b = Math.clamp(35 + noise, 0, 255);
                    setPixel(tile, x, y, r, g, b, 255);
                }
            }
        }
    }

    // 10: Diamond Ore (Stone embedded with brilliant cyan gems)
    private static void generateDiamondOre(byte[] tile, Random rand) {
        generateStone(tile, rand);

        // Gem clusters
        int[][] gems = {{16, 20}, {40, 16}, {24, 44}, {46, 42}, {32, 28}};
        for (int[] g : gems) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) <= 4) {
                        int px = g[0] + dx;
                        int py = g[1] + dy;
                        if (px >= 0 && px < TILE_SIZE && py >= 0 && py < TILE_SIZE) {
                            if (dx == 0 && dy == 0) {
                                setPixel(tile, px, py, 255, 255, 255, 255); // Specular highlight
                            } else {
                                setPixel(tile, px, py, 85, 255, 255, 255);  // Cyan diamond
                            }
                        }
                    }
                }
            }
        }
    }

    // 11: Water (Translucent blue)
    private static void generateWater(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(20) - 10;
                int r = Math.clamp(35 + noise, 0, 255);
                int g = Math.clamp(95 + noise, 0, 255);
                int b = Math.clamp(215 + noise, 0, 255);
                setPixel(tile, x, y, r, g, b, 205); // Alpha 205 (~80% opacity)
            }
        }
    }

    // 12: Tall Grass (Transparent background with rising blades)
    private static void generateTallGrass(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0); // Full transparent background
        for (int x = 12; x < 52; x++) {
            int h = 24 + rand.nextInt(32);
            for (int y = 0; y < h; y++) {
                int noise = rand.nextInt(20);
                setPixel(tile, x, y, 92 + noise, 164 + noise, 48 + noise, 255);
            }
        }
    }

    // 13: Poppy (Red petals with green stem)
    private static void generatePoppy(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);
        // Stem
        for (int y = 0; y < 36; y++) {
            setPixel(tile, 31, y, 55, 135, 30, 255);
            setPixel(tile, 32, y, 55, 135, 30, 255);
        }
        // Flower Petals
        for (int dy = -10; dy <= 10; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                if (dx * dx + dy * dy <= 100) {
                    int r = (dx == 0 && dy == 0) ? 255 : 221;
                    int g = (dx == 0 && dy == 0) ? 230 : 34;
                    int b = (dx == 0 && dy == 0) ? 0 : 34;
                    setPixel(tile, 32 + dx, 44 + dy, r, g, b, 255);
                }
            }
        }
    }

    // 14: Dandelion (Golden bloom with green stem)
    private static void generateDandelion(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);
        // Stem
        for (int y = 0; y < 32; y++) {
            setPixel(tile, 31, y, 55, 135, 30, 255);
            setPixel(tile, 32, y, 55, 135, 30, 255);
        }
        // Yellow Bloom
        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                if (dx * dx + dy * dy <= 64) {
                    setPixel(tile, 32 + dx, 38 + dy, 255, 221, 0, 255);
                }
            }
        }
    }

    // 15: Birch Log Side (White bark with black notch stripes)
    private static void generateBirchLogSide(byte[] tile, Random rand) {
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int noise = rand.nextInt(16) - 8;
                int v = Math.clamp(230 + noise, 0, 255);
                setPixel(tile, x, y, v, v, v, 255);
            }
        }
        // Iconic black birch notches
        for (int stripe = 0; stripe < 8; stripe++) {
            int sy = rand.nextInt(TILE_SIZE - 4);
            int sx = rand.nextInt(TILE_SIZE - 12);
            int len = 6 + rand.nextInt(10);
            for (int dx = 0; dx < len; dx++) {
                setPixel(tile, sx + dx, sy, 30, 30, 30, 255);
                setPixel(tile, sx + dx, sy + 1, 30, 30, 30, 255);
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

    private static void fillSolid(byte[] tile, int r, int g, int b, int a) {
        for (int i = 0; i < tile.length; i += 4) {
            tile[i] = (byte) r;
            tile[i + 1] = (byte) g;
            tile[i + 2] = (byte) b;
            tile[i + 3] = (byte) a;
        }
    }
}
