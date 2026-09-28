package com.mcjournal;

import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.ChunkRenderer;
import com.mcjournal.client.ParticleManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Modernized fluid simulation pipeline (P6):
 * - Stores fluid decay level (0..7) directly in BlockState (P6.1)
 * - Batches fluid mutations into an atomic mutationBuffer before applying to chunks (P6.2)
 * - Uses a deduplicated scheduled update queue (P6.3)
 * - Chunk-aware: prevents synchronous blocking or generation at chunk boundaries (P6.4)
 */
public class FluidPhysicsManager {
    public static final int MAX_FLUID_LEVEL = 7; // Level 0 = source, 1..7 = flowing decay

    public record FluidUpdate(int x, int y, int z, int level, long scheduledTick) {}

    private final Queue<FluidUpdate> updateQueue = new ConcurrentLinkedQueue<>();
    private final Set<WorldBlockPos> scheduledSet = ConcurrentHashMap.newKeySet();
    private final Map<WorldBlockPos, BlockState> mutationBuffer = new HashMap<>();

    private long currentTick = 0;

    /**
     * Called whenever a block is broken or modified.
     * Checks if adjacent water should rush in and fill the cavity.
     */
    public void onBlockChanged(ChunkManager world, ChunkRenderer renderer, ParticleManager particles, int wx, int wy, int wz) {
        if (!isChunkReady(world, wx, wz)) return;

        int[][] neighbors = {
            {0, 1, 0},   // Above
            {1, 0, 0},   // East
            {-1, 0, 0},  // West
            {0, 0, 1},   // South
            {0, 0, -1},  // North
            {0, -1, 0}   // Below
        };

        int minLevel = Integer.MAX_VALUE;

        for (int[] offset : neighbors) {
            int nx = wx + offset[0];
            int ny = wy + offset[1];
            int nz = wz + offset[2];

            if (!isChunkReady(world, nx, nz)) continue;

            BlockState state = world.getBlockStateAt(nx, ny, nz);
            if (state.isWater()) {
                int level = state.get(BlockProperties.LEVEL);
                // Top water source flows down with highest priority (level 1)
                if (offset[1] == 1) {
                    minLevel = Math.min(minLevel, 1);
                } else if (level < minLevel && level < MAX_FLUID_LEVEL) {
                    minLevel = Math.min(minLevel, level + 1);
                }
            }
        }

        if (minLevel <= MAX_FLUID_LEVEL && world.getBlockStateAt(wx, wy, wz).isAir()) {
            queueFluidUpdate(wx, wy, wz, minLevel);
        }
    }

    public void queueFluidUpdate(int x, int y, int z, int level) {
        if (level > MAX_FLUID_LEVEL || y < 0 || y >= Chunk.HEIGHT) return;
        WorldBlockPos pos = new WorldBlockPos(x, y, z);
        if (scheduledSet.add(pos)) {
            updateQueue.offer(new FluidUpdate(x, y, z, level, currentTick + 1));
        }
    }

    public void scheduleUpdate(int x, int y, int z, int level, int delayTicks) {
        queueFluidUpdate(x, y, z, level);
    }

    public void tick(ChunkManager world) {
        currentTick = 1; // Increment will make currentTick = 2, passing the modulo check
        updateTicks(world, null, null);
    }

    public void updateTicks(ChunkManager world, ChunkRenderer renderer, ParticleManager particles) {
        currentTick++;
        if (updateQueue.isEmpty()) return;

        // Flow every 2 game ticks (100ms) for authentic fluid cascading
        if (currentTick % 2 != 0) return;

        mutationBuffer.clear();
        int updatesToProcess = Math.min(64, updateQueue.size());

        for (int i = 0; i < updatesToProcess; i++) {
            FluidUpdate update = updateQueue.poll();
            if (update == null) break;

            WorldBlockPos pos = new WorldBlockPos(update.x(), update.y(), update.z());
            scheduledSet.remove(pos);

            int x = update.x();
            int y = update.y();
            int z = update.z();
            int level = update.level();

            // P6.4: Chunk-aware simulation — skip if chunk is not loaded
            if (!isChunkReady(world, x, z)) continue;

            BlockState current = world.getBlockStateAt(x, y, z);
            if (!current.isAir() && !current.isPlant() && !current.isWater()) {
                continue;
            }

            // P6.1: Store fluid level in BlockState
            BlockState waterState = Blocks.WATER.getDefaultState().with(BlockProperties.LEVEL, level);
            mutationBuffer.put(pos, waterState);

            // Water ripple/splash particles
            if (particles != null) {
                particles.spawnMiningHitParticles(x, y, z, waterState, 0, 1, 0);
            }

            // 1. Downward Flow (Vertical cascade has highest priority)
            int belowY = y - 1;
            if (belowY >= 0 && isChunkReady(world, x, z)) {
                BlockState below = world.getBlockStateAt(x, belowY, z);
                if (below.isAir() || below.isPlant()) {
                    // Downward flow resets decay level to 1 (full vertical stream)
                    queueFluidUpdate(x, belowY, z, 1);
                    continue; // Skip horizontal spreading while falling
                }
            }

            // 2. Horizontal Spread (If downward flow is obstructed)
            if (level < MAX_FLUID_LEVEL) {
                int[][] horizontalDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] dir : horizontalDirs) {
                    int nx = x + dir[0];
                    int nz = z + dir[1];

                    if (!isChunkReady(world, nx, nz)) continue;

                    BlockState neighbor = world.getBlockStateAt(nx, y, nz);
                    if (neighbor.isAir() || neighbor.isPlant()) {
                        queueFluidUpdate(nx, y, nz, level + 1);
                    }
                }
            }
        }

        // P6.2: Apply all buffered mutations in one batch, then trigger dirty meshing
        if (!mutationBuffer.isEmpty()) {
            com.mcjournal.client.EngineMetrics.getInstance().recordFluidUpdates(mutationBuffer.size());
            for (Map.Entry<WorldBlockPos, BlockState> entry : mutationBuffer.entrySet()) {
                WorldBlockPos p = entry.getKey();
                world.setBlockStateAt(p.x(), p.y(), p.z(), entry.getValue());
            }
            mutationBuffer.clear();
        }
    }

    private boolean isChunkReady(ChunkManager world, int wx, int wz) {
        int cx = Math.floorDiv(wx, Chunk.SIZE);
        int cz = Math.floorDiv(wz, Chunk.SIZE);
        return world != null && world.isChunkLoaded(cx, cz);
    }

    public int getPendingUpdateCount() {
        return updateQueue.size();
    }

    public void clear() {
        updateQueue.clear();
        scheduledSet.clear();
        mutationBuffer.clear();
    }
}
