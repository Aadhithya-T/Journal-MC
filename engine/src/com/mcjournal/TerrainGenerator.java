package com.mcjournal;

import java.util.*;

public class TerrainGenerator {
    public static final int SEA_LEVEL = 62; // Minecraft 1.17 Standard Sea Level
    public static final int BASE_GROUND_LEVEL = 64; // Minecraft 1.17 Plains Ground Level

    private final SimplexNoise baseNoise;
    private final SimplexNoise detailNoise;
    private final SimplexNoise mountainNoise;
    private final SimplexNoise caveNoise1;
    private final SimplexNoise caveNoise2;
    private final List<POI> pois = new ArrayList<>();

    public record POI(String id, String name, String subtitle, String icon, int x, int z, String excerpt) {}

    public TerrainGenerator(long seed) {
        this.baseNoise = new SimplexNoise(seed);
        this.detailNoise = new SimplexNoise(seed + 1337);
        this.mountainNoise = new SimplexNoise(seed + 9999);
        this.caveNoise1 = new SimplexNoise(seed + 4242);
        this.caveNoise2 = new SimplexNoise(seed + 8484);
        initPOIs();
    }

    private void initPOIs() {
        pois.add(new POI("spawn_lectern", "Adventurer's Lectern", "River Clearing", "📜", 6, -10, "A hand-carved oak pedestal holding an open journal."));
        pois.add(new POI("crystal_lake", "Crystal Lake Study", "Mirror Water's Edge", "🌊", 14, 28, "A tranquil research post set up beside clear spring waters."));
        pois.add(new POI("shrine", "Forgotten Stone Shrine", "Eastern Overlook", "🏛️", 22, 16, "An ancient stone altar flanked by moss-weathered pillars."));
        pois.add(new POI("wanderer_cache", "Wanderer's Cache", "Dense Birch Border", "📦", -18, -24, "A sturdy chest resting beside a cluster of oak logs."));
        pois.add(new POI("mineshaft_entrance", "Abandoned Mine Entrance", "Deep Ravine Edge", "⛏️", 32, -28, "Weathered oak beam scaffolding leading into a dark mineral-rich tunnel."));
        pois.add(new POI("deep_ridge", "Deep Ridge Mineral Pocket", "Southern Granite Crags", "💎", -26, 32, "Exposed veins of blue diamond ore glittering under the mountain sun."));
    }

    public List<POI> getPois() {
        return Collections.unmodifiableList(pois);
    }

    /**
     * Minecraft 1.17 Multi-Octave Continental & Mountain Heightmap.
     * Produces plains at Y=64-72, rolling hills at Y=73-85, rivers down to Y=52, and mountains up to Y=125.
     */
    public int computeHeight(int wx, int wz) {
        // Continental Base Noise (Plains & Gentle Rolling Hills)
        double continental = baseNoise.noise2D(wx * 0.012, wz * 0.012) * 8.0;
        double detail = detailNoise.noise2D(wx * 0.035, wz * 0.035) * 3.5;

        // Mountain Ridge Noise (Cliffs & Peaks)
        double ridge = Math.max(0, mountainNoise.noise2D(wx * 0.008, wz * 0.008));
        double mountain = Math.pow(ridge, 2.2) * 45.0;

        // River Channel Carving
        double riverCurve = Math.sin(wz * 0.025) * 22.0 + Math.cos(wz * 0.012) * 8.0;
        double distToRiver = Math.abs(wx - riverCurve);

        double baseHeight = BASE_GROUND_LEVEL + continental + detail + mountain;

        if (distToRiver < 7.0) {
            // Deep river bed (Y = 52 to 56)
            double depth = (1.0 - (distToRiver / 7.0));
            baseHeight -= depth * 16.0;
        } else if (distToRiver < 11.0) {
            // River bank slope
            double slope = (1.0 - ((distToRiver - 7.0) / 4.0));
            baseHeight -= slope * 6.0;
        }

        return Math.clamp((int) Math.round(baseHeight), 45, 230);
    }

    public Chunk generateChunk(int cx, int cz) {
        Chunk chunk = new Chunk(cx, cz);
        Random rng = new Random((long) cx * 341873128712L + (long) cz * 132897987541L);

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = cx * 16 + lx;
                int wz = cz * 16 + lz;

                int surfaceY = computeHeight(wx, wz);

                // 1. Bedrock Floor (Y = 0 with random bedrock bumps up to Y = 3)
                chunk.setBlock(lx, 0, lz, Block.BEDROCK);
                for (int by = 1; by <= 3; by++) {
                    if (rng.nextFloat() < (1.0f - by * 0.28f)) {
                        chunk.setBlock(lx, by, lz, Block.BEDROCK);
                    }
                }

                // 2. Underground Mantle (Stone, Ores & 3D Caves)
                for (int y = 1; y < surfaceY; y++) {
                    if (chunk.getBlock(lx, y, lz) == Block.BEDROCK) continue;

                    if (y < surfaceY - 4) {
                        // 3D Minecraft 1.17 Cave & Tunnel Carving
                        if (y >= 5) {
                            double c1 = caveNoise1.noise3D(wx * 0.042, y * 0.062, wz * 0.042);
                            double c2 = caveNoise2.noise3D(wx * 0.042, y * 0.062, wz * 0.042);

                            // Noodle / Worm Tunnels & Spacious Caverns
                            double tunnelMetric = c1 * c1 + c2 * c2;
                            boolean isWormCave = (tunnelMetric < 0.016);
                            boolean isCavern = (c1 > 0.60 && Math.abs(c2) < 0.32);

                            if (isWormCave || isCavern) {
                                // Deep underground spring pools below Y = 12
                                if (y <= 11 && y >= 9 && !isWormCave) {
                                    chunk.setBlock(lx, y, lz, Block.WATER);
                                } else {
                                    chunk.setBlock(lx, y, lz, Block.AIR);
                                }
                                continue;
                            }
                        }

                        // Diamond Ore (Minecraft 1.17: Y = 1 to 16, peak density near bedrock)
                        if (y <= 16) {
                            double diamondNoise = detailNoise.noise2D(wx * 0.45, wz * 0.45 + y * 0.3);
                            if (diamondNoise > 0.72) {
                                chunk.setBlock(lx, y, lz, Block.DIAMOND_ORE);
                                continue;
                            }
                        }

                        // Cobblestone vein clusters in deep stone
                        double cobbleNoise = detailNoise.noise2D(wx * 0.15, wz * 0.15 + y * 0.1);
                        if (cobbleNoise > 0.75) {
                            chunk.setBlock(lx, y, lz, Block.COBBLESTONE);
                        } else {
                            chunk.setBlock(lx, y, lz, Block.STONE);
                        }
                    } else {
                        // Subsurface Dirt / Sand Layer (3-4 blocks below surface)
                        if (surfaceY <= SEA_LEVEL + 1) {
                            chunk.setBlock(lx, y, lz, Block.SAND);
                        } else {
                            chunk.setBlock(lx, y, lz, Block.DIRT);
                        }
                    }
                }

                // 3. Surface & Shoreline Layering
                if (surfaceY <= SEA_LEVEL) {
                    // Underwater River / Ocean Floor
                    chunk.setBlock(lx, surfaceY, lz, Block.SAND);

                    // Water column filling up to Sea Level (Y = 62)
                    for (int wy = surfaceY + 1; wy <= SEA_LEVEL; wy++) {
                        chunk.setBlock(lx, wy, lz, Block.WATER);
                    }
                } else if (surfaceY <= SEA_LEVEL + 2) {
                    // Beach / Shoreline
                    chunk.setBlock(lx, surfaceY, lz, Block.SAND);
                } else if (surfaceY > 105) {
                    // High Mountain Cliff / Rocky Peaks
                    chunk.setBlock(lx, surfaceY, lz, Block.STONE);
                } else {
                    // Lush Plains / Forest Surface (Grass Block)
                    chunk.setBlock(lx, surfaceY, lz, Block.GRASS);

                    // 4. Wildflower & Foliage Scatter
                    double vegNoise = detailNoise.noise2D(wx * 0.28, wz * 0.28);
                    if (surfaceY + 1 < Chunk.HEIGHT) {
                        if (vegNoise > 0.83) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.POPPY);
                        } else if (vegNoise > 0.76) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.DANDELION);
                        } else if (vegNoise > 0.52) {
                            chunk.setBlock(lx, surfaceY + 1, lz, Block.TALL_GRASS);
                        }
                    }
                }
            }
        }

        // 5. Forest Tree Generation (Oak & Birch on grass terrain)
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

        // Only plant on grass above sea level and below high mountain peaks
        if (ty >= SEA_LEVEL + 2 && ty <= 95 && chunk.getBlock(tx, ty, tz) == Block.GRASS) {
            byte logType = isBirch ? Block.BIRCH_LOG : Block.OAK_LOG;
            byte leafType = isBirch ? Block.BIRCH_LEAVES : Block.OAK_LEAVES;
            int trunkHeight = isBirch ? 5 : 4;

            // Clear any foliage above the ground
            chunk.setBlock(tx, ty + 1, tz, Block.AIR);

            // Wood Trunk
            for (int y = 1; y <= trunkHeight; y++) {
                if (ty + y < Chunk.HEIGHT) {
                    chunk.setBlock(tx, ty + y, tz, logType);
                }
            }

            // Canopy Leaves (3D Minecraft tree canopy)
            int leafBase = ty + trunkHeight - 1;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (tx + dx >= 0 && tx + dx < 16 && tz + dz >= 0 && tz + dz < 16) {
                        if (leafBase < Chunk.HEIGHT) chunk.setBlock(tx + dx, leafBase, tz + dz, leafType);
                        if (leafBase + 1 < Chunk.HEIGHT) chunk.setBlock(tx + dx, leafBase + 1, tz + dz, leafType);
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (tx + dx >= 0 && tx + dx < 16 && tz + dz >= 0 && tz + dz < 16) {
                        if (leafBase + 2 < Chunk.HEIGHT) chunk.setBlock(tx + dx, leafBase + 2, tz + dz, leafType);
                    }
                }
            }
            if (leafBase + 3 < Chunk.HEIGHT) {
                chunk.setBlock(tx, leafBase + 3, tz, leafType);
                if (tx + 1 < 16) chunk.setBlock(tx + 1, leafBase + 3, tz, leafType);
                if (tx - 1 >= 0) chunk.setBlock(tx - 1, leafBase + 3, tz, leafType);
                if (tz + 1 < 16) chunk.setBlock(tx, leafBase + 3, tz + 1, leafType);
                if (tz - 1 >= 0) chunk.setBlock(tx, leafBase + 3, tz - 1, leafType);
            }
        }
    }
}
