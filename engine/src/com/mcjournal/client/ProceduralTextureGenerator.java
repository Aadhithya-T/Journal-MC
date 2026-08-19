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

    // Deterministic bit-permutation hash function (integer pixel coordinates)
    private static int hash(int x, int y, int seed) {
        int h = (x & 0x3F) * 374761393 + (y & 0x3F) * 668265263 + seed;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h ^ (h >> 16)) & 0x7FFFFFFF;
    }

    // 0: Grass Top (Authentic Minecraft-style lush meadow green with clustered micro-blades)
    private static void generateGrassTop(byte[] tile, Random rand) {
        // Base 5-color calibrated pixel-art palette (sRGB)
        final int[][] PALETTE = {
            {64, 116, 32},  // 0: Deep blade shadow
            {76, 134, 38},  // 1: Dark green
            {88, 150, 46},  // 2: Base meadow green
            {104, 168, 56}, // 3: Fresh green highlight
            {118, 184, 66}  // 4: Bright leaf tip highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Macro-level tone cluster (4x4 block scale)
                int macro = (hash(bx, by, 101) % 100);
                int baseTone = 2;
                if (macro < 22) baseTone = 1;
                else if (macro > 78) baseTone = 3;

                // Micro-level pixel variation (1x1 & 2x2 texels)
                int micro = (hash(x, y, 202) % 100);
                int tone = baseTone;
                if (micro < 18) {
                    tone = Math.max(0, baseTone - 1);
                } else if (micro > 82) {
                    tone = Math.min(4, baseTone + 1);
                }

                // Fine blade highlights
                if ((x % 4 == 1 || x % 4 == 2) && (y % 4 == 1) && ((hash(x, y, 303) % 10) < 4)) {
                    tone = Math.min(4, tone + 1);
                }
                // Fine blade shadows
                if ((x % 4 == 0 || x % 4 == 3) && (y % 4 == 3) && ((hash(x, y, 404) % 10) < 3)) {
                    tone = Math.max(0, tone - 1);
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // Scatter organic micro-blade clusters (2x2 to 2x3 blade tufts)
        for (int i = 0; i < 48; i++) {
            int cx = (hash(i, 17, 505) % (TILE_SIZE - 3));
            int cy = (hash(i, 31, 606) % (TILE_SIZE - 3));
            int type = hash(i, 43, 707) % 3;

            if (type == 0) {
                // Light blade tuft
                setPixel(tile, cx, cy, 104, 168, 56, 255);
                setPixel(tile, cx + 1, cy, 118, 184, 66, 255);
                setPixel(tile, cx, cy + 1, 88, 150, 46, 255);
                setPixel(tile, cx, cy - 1, 64, 116, 32, 255);
            } else if (type == 1) {
                // Dark shadow tuft
                setPixel(tile, cx, cy, 76, 134, 38, 255);
                setPixel(tile, cx + 1, cy, 64, 116, 32, 255);
                setPixel(tile, cx, cy + 1, 64, 116, 32, 255);
            }
        }
    }

    // 1: Grass Side (Rich earthen soil with organic, irregular drooping grass fringe)
    private static void generateGrassSide(byte[] tile, Random rand) {
        // 1. Fill entire block with calibrated dirt
        generateDirt(tile, rand);

        // 2. Overlay authentic pixel-art grass fringe along the top (y increases upward in texture)
        // Upper 4 pixels are solid grass top
        for (int y = TILE_SIZE - 4; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int tone = 2 + ((hash(x, y, 808) % 100) > 65 ? 1 : 0) - ((hash(x, y, 809) % 100) > 75 ? 1 : 0);
                tone = Math.clamp(tone, 0, 4);
                final int[][] GRASS_PALETTE = {
                    {64, 116, 32}, {76, 134, 38}, {88, 150, 46}, {104, 168, 56}, {118, 184, 66}
                };
                int[] rgb = GRASS_PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // Jagged, organic hanging grass roots & blade drips
        for (int x = 0; x < TILE_SIZE; x++) {
            // Irregular hanging blade depth (4 to 22 pixels down from top)
            int bx = x / 4;
            int macroHang = 8 + (hash(bx, 7, 909) % 10);
            int microHang = (hash(x, 13, 1010) % 5) - 2;
            int hangDepth = Math.clamp(macroHang + microHang, 5, 20);

            // Periodic longer blade tips
            if ((x % 8 == 2 || x % 8 == 6) && ((hash(x, 29, 1111) % 10) < 7)) {
                hangDepth += 4;
            }

            for (int d = 4; d <= hangDepth; d++) {
                int py = TILE_SIZE - 1 - d;
                if (py < 0) continue;

                // Color grading down the blade: brighter top, darker tip/shadow
                int r = 88, g = 150, b = 46;
                if (d == hangDepth) {
                    // Blade root tip / shadow
                    r = 58; g = 104; b = 28;
                } else if (d == hangDepth - 1) {
                    r = 76; g = 134; b = 38;
                } else if (d < 8 && (x % 2 == 0)) {
                    r = 104; g = 168; b = 56;
                }

                setPixel(tile, x, py, r, g, b, 255);

                // Subtle contact shadow underneath the grass blade onto the dirt below
                if (d == hangDepth && py > 0) {
                    int underY = py - 1;
                    int curR = tile[(underY * TILE_SIZE + x) * 4] & 0xFF;
                    int curG = tile[(underY * TILE_SIZE + x) * 4 + 1] & 0xFF;
                    int curB = tile[(underY * TILE_SIZE + x) * 4 + 2] & 0xFF;
                    setPixel(tile, x, underY, (int)(curR * 0.72), (int)(curG * 0.72), (int)(curB * 0.72), 255);
                }
            }
        }
    }

    // 2: Dirt (Rich warm earthen soil with clustered loam patches and embedded pebbles)
    private static void generateDirt(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {92, 64, 42},   // 0: Deep loam shadow
            {114, 82, 54},  // 1: Dark brown
            {134, 98, 66},  // 2: Base warm soil
            {154, 116, 80}, // 3: Light soil grain
            {172, 134, 98}  // 4: Embedded pebble highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Macro soil variation (4x4 texels)
                int macro = hash(bx, by, 1212) % 100;
                int baseTone = 2;
                if (macro < 25) baseTone = 1;
                else if (macro > 75) baseTone = 3;

                // Micro soil granularity
                int micro = hash(x, y, 1313) % 100;
                int tone = baseTone;
                if (micro < 20) tone = Math.max(0, baseTone - 1);
                else if (micro > 80) tone = Math.min(3, baseTone + 1);

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // Scatter structured pebble and stone clusters (2x2 and 3x2 pebble shapes)
        for (int p = 0; p < 28; p++) {
            int px = hash(p, 19, 1414) % (TILE_SIZE - 4);
            int py = hash(p, 41, 1515) % (TILE_SIZE - 4);
            int pType = hash(p, 59, 1616) % 3;

            if (pType == 0) {
                // 2x2 rounded pebble
                setPixel(tile, px, py + 1, 172, 134, 98, 255);     // Top-left highlight
                setPixel(tile, px + 1, py + 1, 154, 116, 80, 255); // Top-right
                setPixel(tile, px, py, 134, 98, 66, 255);          // Bottom-left
                setPixel(tile, px + 1, py, 92, 64, 42, 255);       // Bottom-right shadow
            } else if (pType == 1) {
                // 3x2 small flat stone
                setPixel(tile, px, py + 1, 154, 116, 80, 255);
                setPixel(tile, px + 1, py + 1, 172, 134, 98, 255);
                setPixel(tile, px + 2, py + 1, 154, 116, 80, 255);
                setPixel(tile, px, py, 114, 82, 54, 255);
                setPixel(tile, px + 1, py, 92, 64, 42, 255);
                setPixel(tile, px + 2, py, 92, 64, 42, 255);
            } else {
                // Dark humus pit
                setPixel(tile, px, py, 92, 64, 42, 255);
                setPixel(tile, px + 1, py, 92, 64, 42, 255);
                setPixel(tile, px, py + 1, 114, 82, 54, 255);
            }
        }
    }

    // 3: Stone (Natural slate gray with mineral cleavage planes and quartz flecks)
    private static void generateStone(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {88, 88, 92},    // 0: Deep fissure shadow
            {106, 106, 110}, // 1: Dark slate gray
            {126, 126, 128}, // 2: Neutral stone base
            {144, 144, 148}, // 3: Light mineral grain
            {164, 164, 170}  // 4: Quartz highlight fleck
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Macro mineral distribution
                int macro = hash(bx, by, 1717) % 100;
                int baseTone = 2;
                if (macro < 24) baseTone = 1;
                else if (macro > 76) baseTone = 3;

                // Micro rock granularity
                int micro = hash(x, y, 1818) % 100;
                int tone = baseTone;
                if (micro < 18) tone = Math.max(0, baseTone - 1);
                else if (micro > 82) tone = Math.min(4, baseTone + 1);

                // Subtle sedimentary fracture lines (subtle diagonal rock cleavage)
                if ((x + y * 2) % 16 == 0 && (hash(x, y, 1919) % 10 < 7)) {
                    tone = Math.max(0, tone - 1);
                } else if ((x + y * 2) % 16 == 1 && (hash(x, y, 2020) % 10 < 6)) {
                    tone = Math.min(4, tone + 1);
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // Scatter embedded mineral patches & quartz speckles
        for (int m = 0; m < 24; m++) {
            int mx = hash(m, 23, 2121) % (TILE_SIZE - 3);
            int my = hash(m, 47, 2222) % (TILE_SIZE - 3);
            int mType = hash(m, 67, 2323) % 2;

            if (mType == 0) {
                // Quartz sparkle
                setPixel(tile, mx, my, 164, 164, 170, 255);
                setPixel(tile, mx + 1, my, 144, 144, 148, 255);
                setPixel(tile, mx, my - 1, 88, 88, 92, 255);
            } else {
                // Dark basalt mineral cluster
                setPixel(tile, mx, my, 88, 88, 92, 255);
                setPixel(tile, mx + 1, my, 106, 106, 110, 255);
                setPixel(tile, mx, my + 1, 106, 106, 110, 255);
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

    // 5: Sand (Pale warm beige sand with interlocking grains and sandstone specks)
    private static void generateSand(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {196, 184, 136}, // 0: Dark sandstone grain / dune shadow
            {210, 198, 150}, // 1: Tan sand
            {224, 214, 166}, // 2: Base warm beige sand
            {236, 228, 182}, // 3: Light cream sand
            {246, 240, 198}  // 4: Sunlit silica highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Macro dune drift variation (4x4 texels)
                int macro = hash(bx, by, 2424) % 100;
                int baseTone = 2;
                if (macro < 22) baseTone = 1;
                else if (macro > 78) baseTone = 3;

                // Micro sand grain granularity (interlocking 1x1 & 2x1 grains)
                int micro = hash(x, y, 2525) % 100;
                int tone = baseTone;
                if (micro < 22) tone = Math.max(0, baseTone - 1);
                else if (micro > 78) tone = Math.min(4, baseTone + 1);

                // Subtle wind-drift banding (soft diagonal ripples)
                if ((x * 2 + y) % 12 == 0 && (hash(x, y, 2626) % 10 < 6)) {
                    tone = Math.min(4, tone + 1);
                } else if ((x * 2 + y) % 12 == 1 && (hash(x, y, 2727) % 10 < 5)) {
                    tone = Math.max(0, tone - 1);
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // Scatter sparse dark sandstone specks & bright silica grains
        for (int s = 0; s < 32; s++) {
            int sx = hash(s, 11, 2828) % (TILE_SIZE - 2);
            int sy = hash(s, 29, 2929) % (TILE_SIZE - 2);
            int sType = hash(s, 37, 3030) % 2;

            if (sType == 0) {
                // Bright silica fleck
                setPixel(tile, sx, sy, 246, 240, 198, 255);
                setPixel(tile, sx + 1, sy, 236, 228, 182, 255);
            } else {
                // Dark sandstone grain
                setPixel(tile, sx, sy, 196, 184, 136, 255);
                setPixel(tile, sx, sy + 1, 210, 198, 150, 255);
            }
        }
    }

    // 6: Bedrock (Dense volcanic basalt fractures and dark crystalline obsidian)
    private static void generateBedrock(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {16, 16, 18},   // 0: Void black crevice
            {32, 32, 36},   // 1: Deep charcoal
            {52, 52, 58},   // 2: Dark basalt
            {74, 74, 82},   // 3: Obsidian gray
            {102, 102, 114} // 4: Ash highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Multi-frequency macro volcanic structure
                int macro = hash(bx, by, 3333) % 100;
                int baseTone = 1;
                if (macro < 28) baseTone = 0;
                else if (macro > 72) baseTone = 2;

                // Micro basalt texture
                int micro = hash(x, y, 3434) % 100;
                int tone = baseTone;
                if (micro < 18) tone = Math.max(0, baseTone - 1);
                else if (micro > 80) tone = Math.min(4, baseTone + 1);

                // Jagged obsidian fracture fissures
                if ((x * 3 + y * 2) % 14 == 0 && (hash(x, y, 3535) % 10 < 8)) {
                    tone = 0; // Void crack
                } else if ((x * 3 + y * 2) % 14 == 1 && (hash(x, y, 3636) % 10 < 7)) {
                    tone = Math.min(4, tone + 2); // Bright fracture rim
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }
    }

    // 7: Oak Log Side (Vertical fibrous tree bark with crevice lines and natural ridges)
    private static void generateOakLogSide(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {54, 38, 20},   // 0: Deep bark fissure
            {74, 54, 28},   // 1: Dark bark shadow
            {96, 72, 40},   // 2: Base oak bark
            {118, 90, 52},  // 3: Light bark ridge
            {138, 108, 64}  // 4: Sunlit bark highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                // Vertical bark striping with organic wander
                int wander = (hash(y / 6, 7, 3737) % 3) - 1;
                int barkCol = (x + wander + TILE_SIZE) % 16;

                int tone = 2; // Base bark

                if (barkCol == 0 || barkCol == 8) {
                    tone = 0; // Deep vertical fissure groove
                } else if (barkCol == 1 || barkCol == 9) {
                    tone = 4; // Highlighted ridge next to fissure
                } else if (barkCol == 2 || barkCol == 10) {
                    tone = 3; // Light bark
                } else if (barkCol == 7 || barkCol == 15) {
                    tone = 1; // Shaded side of bark strip
                } else {
                    int grain = hash(x, y, 3838) % 100;
                    if (grain < 25) tone = 1;
                    else if (grain > 75) tone = 3;
                }

                // Horizontal bark knot whorls
                if ((y % 28 >= 12 && y % 28 <= 16) && (x % 32 >= 14 && x % 32 <= 18)) {
                    tone = (x % 32 == 16 && y % 28 == 14) ? 0 : 4;
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }
    }

    // 8: Oak Log Top (Concentric annual growth rings with bark rim & heartwood core)
    private static void generateOakLogTop(byte[] tile, Random rand) {
        int cx = TILE_SIZE / 2;
        int cy = TILE_SIZE / 2;

        final int[][] WOOD_PALETTE = {
            {138, 108, 64}, // 0: Dark growth ring
            {162, 130, 80}, // 1: Heartwood shadow
            {182, 148, 94}, // 2: Base sapwood
            {200, 166, 110},// 3: Light wood grain
            {216, 182, 124} // 4: Cream ring highlight
        };

        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int dx = x - cx;
                int dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist >= 27.5) {
                    // Outer bark ring
                    int bTone = (hash(x, y, 3939) % 100 < 35) ? 0 : 2;
                    int[] bRgb = (bTone == 0) ? new int[]{54, 38, 20} : new int[]{96, 72, 40};
                    setPixel(tile, x, y, bRgb[0], bRgb[1], bRgb[2], 255);
                } else if (dist >= 25.5) {
                    // Bark/wood inner boundary shadow
                    setPixel(tile, x, y, 74, 54, 28, 255);
                } else {
                    // Concentric growth rings
                    int ringVal = (int)(dist + (hash(dx / 4, dy / 4, 4040) % 3) * 0.5);
                    int tone = 2; // Base sapwood

                    if (ringVal % 5 == 0) {
                        tone = 0; // Dark annual ring
                    } else if (ringVal % 5 == 1) {
                        tone = 4; // Bright annual ring edge
                    } else if (dist < 4.0) {
                        tone = 1; // Pith heartwood core
                    } else {
                        int grain = hash(x, y, 4141) % 100;
                        if (grain < 20) tone = 1;
                        else if (grain > 80) tone = 3;
                    }

                    // Radial wood grain rays
                    double angle = Math.atan2(dy, dx);
                    if (Math.abs(Math.sin(angle * 12.0)) > 0.92 && hash(x, y, 4242) % 10 < 6) {
                        tone = Math.max(0, tone - 1);
                    }

                    int[] rgb = WOOD_PALETTE[tone];
                    setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
                }
            }
        }
    }

    // 9: Oak Leaves (Volumetric clustered foliage with organic leaf cutouts - STRICT alpha 0 or 255)
    private static void generateOakLeaves(byte[] tile, Random rand) {
        final int[][] PALETTE = {
            {36, 84, 18},   // 0: Deep foliage shadow
            {48, 110, 24},  // 1: Dark leaf
            {64, 136, 32},  // 2: Base lush green
            {82, 162, 44},  // 3: Light leaf highlight
            {104, 188, 58}  // 4: Sunlit leaf tip
        };

        // 1. Base solid lush leaf clusters (NO 4x4 rectangular grid banding)
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int bx = x / 4;
                int by = y / 4;

                // Clustered leaf clump tone
                int clump = hash(bx, by, 4343) % 100;
                int baseTone = 2;
                if (clump < 25) baseTone = 1;
                else if (clump > 75) baseTone = 3;

                // Micro leaf blade structure
                int micro = hash(x, y, 4444) % 100;
                int tone = baseTone;
                if (micro < 20) tone = Math.max(0, baseTone - 1);
                else if (micro > 80) tone = Math.min(4, baseTone + 1);

                // Leaf clump 3D shading: top of clump gets highlight, bottom gets shadow
                if (y % 4 == 3 && (hash(x, y, 4545) % 10 < 7)) {
                    tone = Math.min(4, tone + 1);
                } else if (y % 4 == 0 && (hash(x, y, 4646) % 10 < 6)) {
                    tone = Math.max(0, tone - 1);
                }

                int[] rgb = PALETTE[tone];
                setPixel(tile, x, y, rgb[0], rgb[1], rgb[2], 255);
            }
        }

        // 2. Carve organic, scattered leaf cutouts (approx 12% transparent holes with STRICT alpha 0)
        for (int by = 0; by < 16; by++) {
            for (int bx = 0; bx < 16; bx++) {
                int holeHash = hash(bx, by, 4747) % 100;
                if (holeHash < 16) {
                    int holeW = 2 + (holeHash % 2);
                    int holeH = 2 + ((holeHash / 2) % 2);
                    for (int dy = 0; dy < holeH; dy++) {
                        for (int dx = 0; dx < holeW; dx++) {
                            int px = (bx * 4 + dx) % TILE_SIZE;
                            int py = (by * 4 + dy) % TILE_SIZE;
                            setPixel(tile, px, py, 0, 0, 0, 0); // Strict binary alpha 0
                        }
                    }
                }
            }
        }
    }

    // 10: Diamond Ore (Natural Stone mantle embedded with brilliant cyan gemstone crystals)
    private static void generateDiamondOre(byte[] tile, Random rand) {
        // 1. Generate calibrated neutral stone background
        generateStone(tile, rand);

        // 2. Embed 5 faceted diamond crystal clusters
        int[][] gems = {
            {14, 18, 7, 7},
            {42, 14, 8, 8},
            {22, 42, 9, 8},
            {44, 40, 7, 7},
            {30, 26, 8, 8}
        };

        for (int[] g : gems) {
            int gx0 = g[0], gy0 = g[1], gw = g[2], gh = g[3];

            for (int dy = 0; dy < gh; dy++) {
                for (int dx = 0; dx < gw; dx++) {
                    int px = (gx0 + dx) % TILE_SIZE;
                    int py = (gy0 + dy) % TILE_SIZE;

                    // Faceted diamond shape (diamond / hexagon mask)
                    int mDist = Math.abs(dx - gw / 2) + Math.abs(dy - gh / 2);
                    if (mDist > (gw / 2 + 1)) continue;

                    int r, gr, b;
                    if (mDist <= 1) {
                        // Brilliant sparkle core
                        r = 220; gr = 255; b = 255;
                    } else if (dx <= gw / 2 && dy <= gh / 2) {
                        // Lit top-left facet
                        r = 110; gr = 240; b = 255;
                    } else if (mDist == 2) {
                        // Rich cyan body
                        r = 46; gr = 204; b = 228;
                    } else {
                        // Deep crystal border shadow
                        r = 22; gr = 136; b = 158;
                    }

                    setPixel(tile, px, py, r, gr, b, 255);
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

    // 12: Tall Grass (Authentic pixel-art wild grass stems - STRICT alpha 0 or 255)
    private static void generateTallGrass(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);

        int[] blades = {10, 18, 26, 34, 42, 50, 56};
        for (int bx : blades) {
            int h = 34 + (hash(bx, 19, 4848) % 22);
            int curve = (hash(bx, 23, 4949) % 3) - 1;

            for (int y = 0; y < h; y++) {
                int cx = bx + (int)(Math.sin(y * 0.08) * curve * 4.0);
                int r = 82, g = 156, b = 44;
                if (y > h - 4) {
                    // Bright tip
                    r = 114; g = 186; b = 64;
                } else if (y < 8) {
                    // Dark base
                    r = 58; g = 118; b = 30;
                }

                setPixel(tile, cx, y, r, g, b, 255);
                setPixel(tile, cx + 1, y, (int)(r * 0.85), (int)(g * 0.85), (int)(b * 0.85), 255);
            }
        }
    }

    // 13: Poppy (Authentic Minecraft red flower - STRICT alpha 0 or 255)
    private static void generatePoppy(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);

        // Green stem
        for (int y = 0; y < 42; y++) {
            setPixel(tile, 31, y, 58, 124, 32, 255);
            setPixel(tile, 32, y, 76, 148, 42, 255);
        }

        // Red poppy flower head
        int cx = 32, cy = 46;
        for (int dy = -9; dy <= 9; dy++) {
            for (int dx = -9; dx <= 9; dx++) {
                int distSq = dx * dx + dy * dy;
                if (distSq <= 64) {
                    int r, g, b;
                    if (distSq <= 8) {
                        // Dark flower center
                        r = 44; g = 28; b = 22;
                    } else if (dy > 2 && dx < 0) {
                        // Lit top petal
                        r = 236; g = 48; b = 48;
                    } else if (distSq >= 48) {
                        // Dark petal rim
                        r = 178; g = 24; b = 24;
                    } else {
                        // Base red petal
                        r = 214; g = 36; b = 36;
                    }
                    setPixel(tile, cx + dx, cy + dy, r, g, b, 255);
                }
            }
        }
    }

    // 14: Dandelion (Authentic Minecraft yellow flower - STRICT alpha 0 or 255)
    private static void generateDandelion(byte[] tile, Random rand) {
        fillSolid(tile, 0, 0, 0, 0);

        // Green stem
        for (int y = 0; y < 36; y++) {
            setPixel(tile, 31, y, 58, 124, 32, 255);
            setPixel(tile, 32, y, 76, 148, 42, 255);
        }

        // Yellow flower head
        int cx = 32, cy = 40;
        for (int dy = -8; dy <= 8; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                int distSq = dx * dx + dy * dy;
                if (distSq <= 50) {
                    int r, g, b;
                    if (distSq <= 6) {
                        // Orange-gold core
                        r = 248; g = 172; b = 18;
                    } else if (dy > 1) {
                        // Bright sunlit petal
                        r = 255; g = 234; b = 42;
                    } else if (distSq >= 36) {
                        // Shaded outer petal
                        r = 216; g = 184; b = 18;
                    } else {
                        // Base golden yellow
                        r = 244; g = 212; b = 28;
                    }
                    setPixel(tile, cx + dx, cy + dy, r, g, b, 255);
                }
            }
        }
    }

    // 15: Birch Log Side (Smooth pale birch bark with authentic horizontal black dash lenticels)
    private static void generateBirchLogSide(byte[] tile, Random rand) {
        // 1. Base pale white-cream paper bark with subtle vertical texture
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int grain = hash(x, y, 5050) % 100;
                int r = 228, g = 226, b = 218;
                if (grain < 20) {
                    r = 212; g = 210; b = 202; // Shadow streak
                } else if (grain > 80) {
                    r = 240; g = 238; b = 232; // Pure white highlight
                }
                setPixel(tile, x, y, r, g, b, 255);
            }
        }

        // 2. Authentic horizontal black lenticel markings (horizontal dashes with dark centers & shaded rims)
        int[][] markings = {
            {8, 12, 14, 3},
            {34, 8, 18, 4},
            {16, 26, 12, 3},
            {42, 22, 16, 3},
            {6, 40, 20, 4},
            {36, 42, 14, 3},
            {18, 54, 16, 3},
            {46, 56, 10, 3}
        };

        for (int[] m : markings) {
            int mx0 = m[0], my0 = m[1], mw = m[2], mh = m[3];
            for (int dy = 0; dy < mh; dy++) {
                for (int dx = 0; dx < mw; dx++) {
                    int px = (mx0 + dx) % TILE_SIZE;
                    int py = (my0 + dy) % TILE_SIZE;

                    int r, g, b;
                    if (dy == 0 || dy == mh - 1 || dx == 0 || dx == mw - 1) {
                        // Gray rim
                        r = 68; g = 68; b = 72;
                    } else {
                        // Deep charcoal core
                        r = 28; g = 28; b = 30;
                    }
                    setPixel(tile, px, py, r, g, b, 255);
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
