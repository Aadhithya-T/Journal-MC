package com.mcjournal;

import com.mcjournal.client.ChunkRenderer;
import com.mcjournal.client.ParticleManager;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FluidPhysicsManager {
    public static class FluidUpdate {
        public final int x, y, z;
        public final int distance; // Flow distance from source (0..7)

        public FluidUpdate(int x, int y, int z, int distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.distance = distance;
        }
    }

    private final Queue<FluidUpdate> updateQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedThisStep = new HashSet<>();
    private int tickTimer = 0;
    private static final int MAX_FLOW_DISTANCE = 6;

    /**
     * Called whenever a block is broken or modified.
     * Checks if adjacent water should rush in and fill the cavity.
     */
    public void onBlockChanged(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, int wx, int wy, int wz) {
        int[][] neighbors = {
            {0, 1, 0},   // Above
            {1, 0, 0},   // East
            {-1, 0, 0},  // West
            {0, 0, 1},   // South
            {0, 0, -1},  // North
            {0, -1, 0}   // Below
        };

        boolean hasAdjacentWater = false;
        int waterNeighbors = 0;

        for (int[] offset : neighbors) {
            int nx = wx + offset[0];
            int ny = wy + offset[1];
            int nz = wz + offset[2];

            if (world.getBlockAt(nx, ny, nz) == Block.WATER) {
                hasAdjacentWater = true;
                if (offset[1] == 0) waterNeighbors++;
            }
        }

        if (hasAdjacentWater && world.getBlockAt(wx, wy, wz) == Block.AIR) {
            // Immediate cavity fill or queued fluid step
            queueWaterSpread(wx, wy, wz, 1);
        }
    }

    public void queueWaterSpread(int x, int y, int z, int distance) {
        if (distance <= MAX_FLOW_DISTANCE) {
            updateQueue.add(new FluidUpdate(x, y, z, distance));
        }
    }

    public void updateTicks(ChunkManager world, ChunkRenderer renderer, ParticleManager particles) {
        if (updateQueue.isEmpty()) return;

        tickTimer++;
        if (tickTimer % 2 != 0) return; // Flow every 2 game ticks (100ms) for realistic liquid cascade

        int updatesThisTick = Math.min(32, updateQueue.size());
        Set<String> chunksToReupload = new HashSet<>();
        visitedThisStep.clear();

        for (int i = 0; i < updatesThisTick; i++) {
            FluidUpdate update = updateQueue.poll();
            if (update == null) break;

            int x = update.x;
            int y = update.y;
            int z = update.z;

            String posKey = x + "," + y + "," + z;
            if (visitedThisStep.contains(posKey)) continue;
            visitedThisStep.add(posKey);

            byte current = world.getBlockAt(x, y, z);
            if (current != Block.AIR && !Block.isPlant(current)) {
                continue;
            }

            // Fill this voxel with water
            world.setBlockAt(x, y, z, Block.WATER);

            // Water ripple/splash particles
            if (particles != null) {
                particles.spawnMiningHitParticles(x, y, z, Block.WATER, 0, 1, 0);
            }

            // Track modified chunks for batch GPU re-upload
            addChunkAndNeighbors(x, z, chunksToReupload);

            // 1. Downward Flow (Highest Priority)
            int belowY = y - 1;
            if (belowY >= 0) {
                byte below = world.getBlockAt(x, belowY, z);
                if (below == Block.AIR || Block.isPlant(below)) {
                    queueWaterSpread(x, belowY, z, 1); // Downward flow resets distance counter
                    continue; // When flowing down, horizontal spread is deferred
                }
            }

            // 2. Horizontal Spread (If downward flow is blocked by floor)
            if (update.distance < MAX_FLOW_DISTANCE) {
                int[][] horizontalDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] dir : horizontalDirs) {
                    int nx = x + dir[0];
                    int nz = z + dir[1];
                    byte neighbor = world.getBlockAt(nx, y, nz);

                    if (neighbor == Block.AIR || neighbor == Block.TALL_GRASS || neighbor == Block.POPPY || neighbor == Block.DANDELION) {
                        queueWaterSpread(nx, y, nz, update.distance + 1);
                    }
                }
            }
        }

        // Batch re-upload modified chunk meshes to GPU
        for (String chunkKey : chunksToReupload) {
            String[] parts = chunkKey.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            ChunkMeshBuilder.MeshData mesh = world.getChunkMesh(cx, cz);
            if (mesh != null && renderer != null) {
                renderer.uploadChunkMesh(cx, cz, mesh);
            }
        }
    }

    private void addChunkAndNeighbors(int wx, int wz, Set<String> set) {
        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        set.add(cx + "," + cz);
        if (lx == 0) set.add((cx - 1) + "," + cz);
        if (lx == 15) set.add((cx + 1) + "," + cz);
        if (lz == 0) set.add(cx + "," + (cz - 1));
        if (lz == 15) set.add(cx + "," + (cz + 1));
    }
}
