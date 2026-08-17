package com.mcjournal;

import java.util.*;

public class TerrainGenerator {
    private final SimplexNoise baseNoise;
    private final SimplexNoise detailNoise;
    private final SimplexNoise mountainNoise;
    private final List<POI> pois = new ArrayList<>();

    public record POI(String id, String name, String subtitle, String icon, int x, int z, String excerpt) {}

    public TerrainGenerator(long seed) {
        this.baseNoise = new SimplexNoise(seed);
        this.detailNoise = new SimplexNoise(seed + 1337);
        this.mountainNoise = new SimplexNoise(seed + 9999);
        initPOIs();
    }

    private void initPOIs() {
        pois.add(new POI("spawn_lectern", "Adventurer's Lectern", "River Clearing", "📜", 6, -10, "A hand-carved oak pedestal holding an open journal."));
        pois.add(new POI("crystal_lake", "Crystal Lake Study", "Mirror Water's Edge", "🌊", 14, 28, "A tranquil research post set up beside clear spring waters."));
        pois.add(new POI("shrine", "Forgotten Stone Shrine", "Eastern Overlook", "🏛️", 22, 16, "An ancient stone altar flanked by moss-weathered pillars."));
        pois.add(new POI("wanderer_cache", "Wanderer's Cache", "Dense Birch Border", "📦", -18, -24, "A sturdy chest resting beside a cluster of oak logs."));
        pois.add(new POI("mineshaft_entrance", "Abandoned Mine Entrance", "Deep Ravine Edge", "⛏️", 32, -28, "Weathered oak beam scaffolding leading into a dark mineral-rich tunnel."));
        pois.add(new POI("deep_ridge", "Deep Ridge Mineral Pocket", "Southern Granite Crags", "💎", -26, 32, "Exposed veins of blue diamond ore glittering under the mountain sun." ));
    }

    public List<POI> getPois() {
        return Collections.unmodifiableList(pois);
    }

    public int computeHeight(int wx, int wz) {
        double riverPath = Math.sin(wz * 0.045) * 14.0 + Math.cos(wz * 0.02) * 5.0;
        double distToRiver = Math.abs(wx - riverPath);

        if (distToRiver < 5.0) {
            return 2; // River bed
        }

        if (distToRiver < 7.5) {
            double t = (distToRiver - 5.0) / 2.5;
            return (int) Math.round(2.0 + t * 2.0);
        }

        double n1 = baseNoise.noise2D(wx * 0.025, wz * 0.025) * 3.5;
        double n2 = detailNoise.noise2D(wx * 0.08, wz * 0.08) * 1.5;
        double m = Math.max(0, mountainNoise.noise2D(wx * 0.012, wz * 0.012)) * 5.0;

        int h = (int) Math.round(5.0 + n1 + n2 + m);
        return Math.clamp(h, 3, 22);
    }

    public Chunk generateChunk(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = cx * 16 + lx;
                int wz = cz * 16 + lz;

                int surfaceY = computeHeight(wx, wz);

                // Bedrock
                chunk.setBlock(lx, 0, lz, Block.BEDROCK);

                // Underground Strata
                for (int y = 1; y < surfaceY; y++) {
                    if (y < surfaceY - 3) {
                        double oreNoise = detailNoise.noise2D(wx * 0.25, wz * 0.25 + y * 0.15);
                        if (oreNoise > 0.62 && y <= 4) {
                            chunk.setBlock(lx, y, lz, Block.DIAMOND_ORE);
                        } else {
                            chunk.setBlock(lx, y, lz, Block.STONE);
                        }
                    } else {
                        chunk.setBlock(lx, y, lz, Block.DIRT);
                    }
                }

                // River Bed, Shoreline & Surface
                if (surfaceY <= 2) {
                    chunk.setBlock(lx, surfaceY, lz, Block.SAND);
                    for (int wy = surfaceY + 1; wy <= 4; wy++) {
                        chunk.setBlock(lx, wy, lz, Block.WATER);
                    }
                } else if (surfaceY <= 3) {
                    chunk.setBlock(lx, surfaceY, lz, Block.SAND);
                    if (surfaceY < 4) {
                        for (int wy = surfaceY + 1; wy <= 4; wy++) {
                            chunk.setBlock(lx, wy, lz, Block.WATER);
                        }
                    }
                } else if (surfaceY > 11) {
                    chunk.setBlock(lx, surfaceY, lz, Block.STONE);
                } else {
                    chunk.setBlock(lx, surfaceY, lz, Block.GRASS);

                    // Subtle Wildflower & Grass Scatter
                    double vegNoise = detailNoise.noise2D(wx * 0.35, wz * 0.35);
                    if (surfaceY + 1 < Chunk.HEIGHT) {
                        if (vegNoise > 0.82) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.POPPY);
                        } else if (vegNoise > 0.74) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.DANDELION);
                        } else if (vegNoise > 0.58) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.TALL_GRASS);
                        }
                    }
                }
            }
        }

        // Oak & White Birch Forest Placement
        int seedHash = Math.abs((cx * 31 + cz) ^ 0x5DEECE66);
        if (seedHash % 3 <= 1) {
            int t1x = (seedHash >> 2) % 8 + 4;
            int t1z = (seedHash >> 5) % 8 + 4;
            plantTree(chunk, cx, cz, t1x, t1z, seedHash % 2 == 0);

            if (seedHash % 3 == 0) {
                int t2x = ((seedHash >> 7) % 6) + 2;
                int t2z = ((seedHash >> 9) % 6) + 8;
                plantTree(chunk, cx, cz, t2x, t2z, seedHash % 4 == 0);
            }
        }

        return chunk;
    }

    private void plantTree(Chunk chunk, int cx, int cz, int tx, int tz, boolean isBirch) {
        int wx = cx * 16 + tx;
        int wz = cz * 16 + tz;
        int ty = computeHeight(wx, wz);

        if (ty >= 4 && ty <= 10) {
            byte logType = isBirch ? Block.BIRCH_LOG : Block.OAK_LOG;
            byte leafType = isBirch ? Block.BIRCH_LEAVES : Block.OAK_LEAVES;
            int trunkHeight = isBirch ? 5 : 4;

            chunk.setBlock(tx, ty + 1, tz, Block.AIR);

            for (int y = 1; y <= trunkHeight; y++) {
                chunk.setBlock(tx, ty + y, tz, logType);
            }

            int leafBase = ty + trunkHeight - 1;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (tx + dx >= 0 && tx + dx < 16 && tz + dz >= 0 && tz + dz < 16) {
                        chunk.setBlock(tx + dx, leafBase, tz + dz, leafType);
                        chunk.setBlock(tx + dx, leafBase + 1, tz + dz, leafType);
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (tx + dx >= 0 && tx + dx < 16 && tz + dz >= 0 && tz + dz < 16) {
                        chunk.setBlock(tx + dx, leafBase + 2, tz + dz, leafType);
                    }
                }
            }
            chunk.setBlock(tx, leafBase + 3, tz, leafType);
            if (tx + 1 < 16) chunk.setBlock(tx + 1, leafBase + 3, tz, leafType);
            if (tx - 1 >= 0) chunk.setBlock(tx - 1, leafBase + 3, tz, leafType);
            if (tz + 1 < 16) chunk.setBlock(tx, leafBase + 3, tz + 1, leafType);
            if (tz - 1 >= 0) chunk.setBlock(tx, leafBase + 3, tz - 1, leafType);
        }
    }
}
