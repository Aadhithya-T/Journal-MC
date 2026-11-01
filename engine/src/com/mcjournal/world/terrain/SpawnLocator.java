package com.mcjournal.world.terrain;

import com.mcjournal.Chunk;
import com.mcjournal.EngineConstants;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.world.WorldBounds;

/**
 * Deterministic, terrain-aware spawn resolution.
 * Searches outward from (1250, 1250) to locate a safe, dry, level surface position
 * with sufficient vertical clearance where the player's full AABB is contained within world bounds.
 */
public final class SpawnLocator {

    public record SpawnPosition(int x, int y, int z) {}

    private SpawnLocator() {}

    /**
     * Resolves a safe surface spawn position for the given generator and world bounds.
     */
    public static SpawnPosition findSafeSpawn(TerrainGenerator generator, WorldBounds bounds) {
        int centerX = EngineConstants.DEFAULT_SPAWN_X;
        int centerZ = EngineConstants.DEFAULT_SPAWN_Z;

        int seaLevel = TerrainGenerator.SEA_LEVEL;
        int maxRadius = 64;

        SpawnPosition bestFallback = null;
        double bestScore = Double.MAX_VALUE;

        // Spiral search outward from (1250, 1250)
        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;

                    int wx = centerX + dx;
                    int wz = centerZ + dz;

                    int surfaceY = generator.computeHeight(wx, wz);
                    int spawnY = surfaceY + 1; // Stand directly on top of surface block

                    // Verify player AABB fits within world bounds
                    float px = wx + 0.5f;
                    float py = (float) spawnY;
                    float pz = wz + 0.5f;
                    if (!bounds.isPlayerAABBInsideWorld(px, py, pz)) {
                        continue;
                    }

                    // Must have vertical clearance for player height (1.8m)
                    if (spawnY + 2 >= Chunk.HEIGHT) {
                        continue;
                    }

                    // Check dry land (above sea level)
                    boolean isDry = surfaceY > seaLevel;

                    // Evaluate local ground flatness / slope
                    int hEast = generator.computeHeight(wx + 1, wz);
                    int hWest = generator.computeHeight(wx - 1, wz);
                    int hSouth = generator.computeHeight(wx, wz + 1);
                    int hNorth = generator.computeHeight(wx, wz - 1);

                    int maxSlope = Math.max(
                            Math.max(Math.abs(surfaceY - hEast), Math.abs(surfaceY - hWest)),
                            Math.max(Math.abs(surfaceY - hSouth), Math.abs(surfaceY - hNorth))
                    );

                    // Ideal candidate: dry surface with gentle slope (<= 1 block drop)
                    if (isDry && maxSlope <= 1) {
                        return new SpawnPosition(wx, spawnY, wz);
                    }

                    // Calculate score for fallback: lower score is better
                    double dist = Math.hypot(dx, dz);
                    double penalty = (isDry ? 0 : 500) + maxSlope * 20.0 + dist;
                    if (penalty < bestScore) {
                        bestScore = penalty;
                        bestFallback = new SpawnPosition(wx, spawnY, wz);
                    }
                }
            }
        }

        if (bestFallback != null) {
            return bestFallback;
        }

        // Ultimate fallback: (1250, h + 1, 1250) clamped safely
        int h = generator.computeHeight(centerX, centerZ);
        return new SpawnPosition(centerX, Math.min(h + 1, 250), centerZ);
    }
}
