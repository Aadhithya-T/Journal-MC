package com.mcjournal.world.terrain;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkPos;
import com.mcjournal.EngineConstants;
import com.mcjournal.TerrainGenerator;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.world.WorldBounds;

import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic, terrain-aware spawn resolution.
 * Searches outward from (1250, 1250) to locate a safe, dry, level surface position
 * with verified voxel occupancy:
 * - Solid, safe ground beneath player's feet
 * - Complete 1.8m vertical clearance with no solid or fluid blocks intersecting player's AABB
 * - Overhead clearance (no low overhangs or cave ceilings directly above head)
 * - Gentle local terrain (no steep drop-offs or sheer cliff transitions)
 * - Strict containment inside WorldBounds
 * - Evaluates actual generated chunk voxels on-demand without generating the entire world
 */
public final class SpawnLocator {

    public record SpawnPosition(int x, int y, int z) {}

    @FunctionalInterface
    public interface BlockAccessor {
        BlockState getBlockState(int wx, int y, int wz);
    }

    private SpawnLocator() {}

    /**
     * Validates whether coordinate (wx, spawnY, wz) is a safe spawn location for the player.
     * Checks:
     * 1. Player AABB containment inside WorldBounds.
     * 2. Vertical world height bounds (spawnY >= 1 and spawnY + 2 < Chunk.HEIGHT).
     * 3. Solid, non-fluid ground beneath the player (at spawnY - 1).
     * 4. Empty clearance at feet/body (spawnY) and head (spawnY + 1) with no solid or fluid blocks.
     * 5. Overhead clearance (spawnY + 2) with no low overhangs or ceilings.
     * 6. Ground slope to adjacent cardinal blocks is gentle (<= 1 block delta).
     */
    public static boolean isSafeSpawnPosition(TerrainGenerator generator, WorldBounds bounds,
                                              int wx, int spawnY, int wz, BlockAccessor accessor) {
        float px = wx + 0.5f;
        float py = (float) spawnY;
        float pz = wz + 0.5f;

        // 1. Player AABB must be completely inside world bounds
        if (!bounds.isPlayerAABBInsideWorld(px, py, pz)) {
            return false;
        }

        // 2. Vertical world bounds: need ground at spawnY-1, player at spawnY..spawnY+1, headroom at spawnY+2
        if (spawnY < 1 || spawnY + 2 >= Chunk.HEIGHT) {
            return false;
        }

        // 3. Ground beneath player must be solid and non-fluid
        BlockState ground = accessor.getBlockState(wx, spawnY - 1, wz);
        if (ground == null || !ground.isSolid() || ground.isWater() || ground.isFluid()) {
            return false;
        }

        // 4. Player collision volume (spawnY and spawnY + 1) must be completely non-solid and non-fluid
        BlockState body = accessor.getBlockState(wx, spawnY, wz);
        if (body == null || body.isSolid() || body.isWater() || body.isFluid()) {
            return false;
        }

        BlockState head = accessor.getBlockState(wx, spawnY + 1, wz);
        if (head == null || head.isSolid() || head.isWater() || head.isFluid()) {
            return false;
        }

        // 5. Headroom clearance: no low overhangs or ceiling immediately above head (spawnY + 2)
        BlockState overhead = accessor.getBlockState(wx, spawnY + 2, wz);
        if (overhead != null && overhead.isSolid()) {
            return false;
        }

        // 6. Terrain slope: ground height of 4 cardinal neighbors must not create a steep drop-off or sheer wall
        int groundY = spawnY - 1;
        int hEast = generator.computeHeight(wx + 1, wz);
        int hWest = generator.computeHeight(wx - 1, wz);
        int hSouth = generator.computeHeight(wx, wz + 1);
        int hNorth = generator.computeHeight(wx, wz - 1);

        int maxSlope = Math.max(
                Math.max(Math.abs(groundY - hEast), Math.abs(groundY - hWest)),
                Math.max(Math.abs(groundY - hSouth), Math.abs(groundY - hNorth))
        );

        return maxSlope <= 1;
    }

    /**
     * Convenience method validating a position using on-demand chunk generation.
     */
    public static boolean isSafeSpawnPosition(TerrainGenerator generator, WorldBounds bounds,
                                              int wx, int spawnY, int wz) {
        return isSafeSpawnPosition(generator, bounds, wx, spawnY, wz, (x, y, z) -> {
            if (!bounds.isValidBlock(x, y, z)) {
                return Blocks.AIR.getDefaultState();
            }
            int cx = WorldBounds.toChunkX(x);
            int cz = WorldBounds.toChunkZ(z);
            int lx = WorldBounds.toLocalX(x);
            int lz = WorldBounds.toLocalZ(z);
            Chunk chunk = generator.generateChunk(cx, cz);
            return chunk.getBlockState(lx, y, lz);
        });
    }

    /**
     * Resolves a safe surface spawn position for the given generator and world bounds.
     * Caches generated chunks in a local map so only the immediate candidate chunks around
     * (1250, 1250) are generated on demand.
     */
    public static SpawnPosition findSafeSpawn(TerrainGenerator generator, WorldBounds bounds) {
        int centerX = EngineConstants.DEFAULT_SPAWN_X;
        int centerZ = EngineConstants.DEFAULT_SPAWN_Z;
        int maxRadius = 64;

        Map<ChunkPos, Chunk> chunkCache = new HashMap<>();

        BlockAccessor accessor = (wx, y, wz) -> {
            if (!bounds.isValidBlock(wx, y, wz)) {
                return Blocks.AIR.getDefaultState();
            }
            int cx = WorldBounds.toChunkX(wx);
            int cz = WorldBounds.toChunkZ(wz);
            int lx = WorldBounds.toLocalX(wx);
            int lz = WorldBounds.toLocalZ(wz);
            ChunkPos cpos = new ChunkPos(cx, cz);
            Chunk chunk = chunkCache.computeIfAbsent(cpos, p -> generator.generateChunk(p.x(), p.z()));
            return chunk.getBlockState(lx, y, lz);
        };

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

                    // Full validation of actual generated blocks, clearance, slope and bounds
                    if (isSafeSpawnPosition(generator, bounds, wx, spawnY, wz, accessor)) {
                        return new SpawnPosition(wx, spawnY, wz);
                    }

                    // Score fallback candidate in case no ideal position exists within radius
                    float px = wx + 0.5f;
                    float py = (float) spawnY;
                    float pz = wz + 0.5f;
                    if (bounds.isPlayerAABBInsideWorld(px, py, pz) && spawnY + 2 < Chunk.HEIGHT) {
                        BlockState ground = accessor.getBlockState(wx, surfaceY, wz);
                        boolean isDrySolid = ground != null && ground.isSolid() && !ground.isWater();
                        int hEast = generator.computeHeight(wx + 1, wz);
                        int hWest = generator.computeHeight(wx - 1, wz);
                        int maxSlope = Math.max(Math.abs(surfaceY - hEast), Math.abs(surfaceY - hWest));
                        double dist = Math.hypot(dx, dz);
                        double penalty = (isDrySolid ? 0 : 500) + maxSlope * 20.0 + dist;
                        if (penalty < bestScore) {
                            bestScore = penalty;
                            bestFallback = new SpawnPosition(wx, spawnY, wz);
                        }
                    }
                }
            }
        }

        if (bestFallback != null) {
            return bestFallback;
        }

        // Ultimate fallback: (1250, h + 1, 1250)
        int h = generator.computeHeight(centerX, centerZ);
        return new SpawnPosition(centerX, Math.min(h + 1, 250), centerZ);
    }
}
