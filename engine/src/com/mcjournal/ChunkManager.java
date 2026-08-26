package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {
    public static final int DEFAULT_RENDER_DISTANCE = 12; // 12 chunks = 192 blocks radius
    public static final int UNLOAD_PADDING = 3;            // Unload at renderDistance + 3 (15 chunks)

    private int renderDistance;
    private int unloadDistance;
    private final TerrainGenerator generator;
    private final RegionManager regionManager;

    private final ConcurrentMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkPos, ChunkMeshBuilder.MeshData> meshes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BlockState> modifiedBlockStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Byte> modifiedBlocksLegacy = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkPos, Map<Integer, BlockState>> chunkDeltas = new ConcurrentHashMap<>();

    private final Set<ChunkPos> loadingInProgress = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<ChunkPos> pendingMeshUploads = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChunkPos> pendingMeshUnloads = new ConcurrentLinkedQueue<>();

    private final ExecutorService chunkWorkers;
    private volatile List<ChunkOffset> spiralOffsets;
    private volatile ChunkPos lastPlayerChunkPos = null;

    private record ChunkOffset(int dx, int dz, int distSq) implements Comparable<ChunkOffset> {
        @Override
        public int compareTo(ChunkOffset o) {
            return Integer.compare(this.distSq, o.distSq);
        }
    }

    public ChunkManager(long seed) {
        this(DEFAULT_RENDER_DISTANCE, seed, null);
    }

    public ChunkManager(int renderDistance, long seed) {
        this(renderDistance, seed, null);
    }

    public ChunkManager(int renderDistance, long seed, java.io.File worldDir) {
        this.renderDistance = Math.max(2, renderDistance);
        this.unloadDistance = this.renderDistance + UNLOAD_PADDING;
        this.generator = new TerrainGenerator(seed);
        this.regionManager = (worldDir != null) ? new RegionManager(worldDir) : null;

        int numWorkers = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 2, 8);
        this.chunkWorkers = Executors.newFixedThreadPool(numWorkers, r -> {
            Thread t = new Thread(r, "ChunkWorker");
            t.setDaemon(true);
            return t;
        });

        this.spiralOffsets = computeSpiralOffsets(this.renderDistance);
    }

    private static List<ChunkOffset> computeSpiralOffsets(int radius) {
        List<ChunkOffset> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= radius) {
                    offsets.add(new ChunkOffset(dx, dz, dx * dx + dz * dz));
                }
            }
        }
        Collections.sort(offsets);
        return Collections.unmodifiableList(offsets);
    }

    public synchronized void setRenderDistance(int newDistance) {
        this.renderDistance = Math.clamp(newDistance, 4, 24);
        this.unloadDistance = this.renderDistance + UNLOAD_PADDING;
        this.spiralOffsets = computeSpiralOffsets(this.renderDistance);
    }

    public TerrainGenerator getGenerator() {
        return generator;
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Synchronously generates and meshes the initial spawn area (e.g. 5x5 chunks around spawn)
     * so the player spawns with solid ground and immediate surroundings rendered.
     */
    public void waitForInitialChunks(int spawnCx, int spawnCz, int radius) {
        List<CompletableFuture<Void>> genFutures = new ArrayList<>();
        List<ChunkPos> initialPositions = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(spawnCx + dx, spawnCz + dz);
                initialPositions.add(pos);
                loadingInProgress.add(pos);

                genFutures.add(CompletableFuture.runAsync(() -> {
                    generateChunkInternal(pos);
                }, chunkWorkers));
            }
        }

        CompletableFuture.allOf(genFutures.toArray(new CompletableFuture[0])).join();

        // Mesh initial chunks in parallel
        List<CompletableFuture<Void>> meshFutures = new ArrayList<>();
        for (ChunkPos pos : initialPositions) {
            meshFutures.add(CompletableFuture.runAsync(() -> {
                Chunk chunk = chunks.get(pos);
                if (chunk != null) {
                    ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
                    meshes.put(pos, mesh);
                    pendingMeshUploads.offer(pos);
                }
                loadingInProgress.remove(pos);
            }, chunkWorkers));
        }

        CompletableFuture.allOf(meshFutures.toArray(new CompletableFuture[0])).join();
        this.lastPlayerChunkPos = new ChunkPos(spawnCx, spawnCz);
    }

    private void submitWorkerTask(Runnable r) {
        if (chunkWorkers.isShutdown()) return;
        try {
            chunkWorkers.submit(r);
        } catch (RejectedExecutionException ignored) {
            // Pool is shutting down
        }
    }

    public void updatePlayerPosition(int playerCx, int playerCz) {
        updatePlayerPosition(playerCx, playerCz, 0.0f, 0.0f);
    }

    /**
     * Called every tick to update world streaming around the player.
     * Loads newly visible chunks in spiral order and unloads distant chunks.
     */
    public void updatePlayerPosition(int playerCx, int playerCz, float lookDirX, float lookDirZ) {
        if (chunkWorkers.isShutdown()) return;
        ChunkPos currentPos = new ChunkPos(playerCx, playerCz);

        // 1. Queue generation for chunks in render distance (spiral closest-first)
        for (ChunkOffset offset : spiralOffsets) {
            ChunkPos pos = new ChunkPos(playerCx + offset.dx, playerCz + offset.dz);
            if (!chunks.containsKey(pos) && loadingInProgress.add(pos)) {
                submitWorkerTask(() -> {
                    try {
                        generateAndMeshChunk(pos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        loadingInProgress.remove(pos);
                    }
                });
            }
        }

        // 2. Unload chunks beyond unload distance
        if (lastPlayerChunkPos == null || !lastPlayerChunkPos.equals(currentPos)) {
            lastPlayerChunkPos = currentPos;
            for (ChunkPos pos : chunks.keySet()) {
                if (pos.distanceChebyshev(currentPos) > unloadDistance) {
                    Chunk chunkToUnload = chunks.remove(pos);
                    if (chunkToUnload != null && regionManager != null && chunkToUnload.isDirty()) {
                        submitWorkerTask(() -> regionManager.saveChunk(chunkToUnload));
                    }
                    meshes.remove(pos);
                    loadingInProgress.remove(pos);
                    pendingMeshUnloads.offer(pos);
                }
            }
        }
    }

    private void generateChunkInternal(ChunkPos pos) {
        Chunk chunk = null;
        if (regionManager != null) {
            chunk = regionManager.loadChunk(pos.x(), pos.z());
        }
        if (chunk == null) {
            chunk = generator.generateChunk(pos.x(), pos.z());
        }

        // Apply any saved block state deltas for this chunk
        Map<Integer, BlockState> deltas = chunkDeltas.get(pos);
        if (deltas != null) {
            short[] blockStates = chunk.getBlockStates();
            for (Map.Entry<Integer, BlockState> entry : deltas.entrySet()) {
                int idx = entry.getKey();
                if (idx >= 0 && idx < blockStates.length) {
                    blockStates[idx] = (short) entry.getValue().getStateId();
                }
            }
            chunk.setDirty(true);
        }

        chunks.put(pos, chunk);
    }

    private void generateAndMeshChunk(ChunkPos pos) {
        generateChunkInternal(pos);

        Chunk chunk = chunks.get(pos);
        if (chunk != null) {
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
            meshes.put(pos, mesh);
            pendingMeshUploads.offer(pos);

            // Rebuild loaded neighbor chunk boundary meshes if they were generated before this chunk
            rebuildNeighborIfLoaded(new ChunkPos(pos.x() - 1, pos.z()));
            rebuildNeighborIfLoaded(new ChunkPos(pos.x() + 1, pos.z()));
            rebuildNeighborIfLoaded(new ChunkPos(pos.x(), pos.z() - 1));
            rebuildNeighborIfLoaded(new ChunkPos(pos.x(), pos.z() + 1));
        }
    }

    private void rebuildNeighborIfLoaded(ChunkPos neighborPos) {
        Chunk neighborChunk = chunks.get(neighborPos);
        if (neighborChunk != null && !loadingInProgress.contains(neighborPos)) {
            submitWorkerTask(() -> {
                ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborChunk, this);
                meshes.put(neighborPos, mesh);
                pendingMeshUploads.offer(neighborPos);
            });
        }
    }

    public ChunkPos pollPendingMeshUpload() {
        return pendingMeshUploads.poll();
    }

    public ChunkPos pollPendingMeshUnload() {
        return pendingMeshUnloads.poll();
    }

    public boolean hasPendingUploads() {
        return !pendingMeshUploads.isEmpty();
    }

    public boolean hasPendingUnloads() {
        return !pendingMeshUnloads.isEmpty();
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public boolean isChunkLoaded(int cx, int cz) {
        return isChunkLoaded(new ChunkPos(cx, cz));
    }

    public boolean isChunkLoaded(ChunkPos pos) {
        return chunks.containsKey(pos);
    }

    public Chunk getChunk(int cx, int cz) {
        return getChunk(new ChunkPos(cx, cz));
    }

    public Chunk getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }

    public Collection<Chunk> getAllChunks() {
        return chunks.values();
    }

    public ChunkMeshBuilder.MeshData getChunkMesh(int cx, int cz) {
        return getChunkMesh(new ChunkPos(cx, cz));
    }

    public ChunkMeshBuilder.MeshData getChunkMesh(ChunkPos pos) {
        return meshes.get(pos);
    }

    public Map<ChunkPos, ChunkMeshBuilder.MeshData> getAllMeshes() {
        return meshes;
    }

    public Map<String, Byte> getModifiedBlocks() {
        return modifiedBlocksLegacy;
    }

    public Map<String, BlockState> getModifiedBlockStates() {
        return modifiedBlockStates;
    }

    public void applyModifiedBlocks(Map<String, Byte> deltas) {
        if (deltas == null || deltas.isEmpty()) return;
        Map<String, BlockState> stateDeltas = new HashMap<>();
        for (Map.Entry<String, Byte> entry : deltas.entrySet()) {
            stateDeltas.put(entry.getKey(), BlockStateRegistry.getDefaultState(entry.getValue()));
        }
        applyModifiedBlockStates(stateDeltas);
    }

    public void applyModifiedBlockStates(Map<String, BlockState> deltas) {
        if (deltas == null || deltas.isEmpty()) return;
        Set<ChunkPos> dirtyChunks = new HashSet<>();

        for (Map.Entry<String, BlockState> entry : deltas.entrySet()) {
            try {
                String[] pos = entry.getKey().split(",");
                int wx = Integer.parseInt(pos[0]);
                int wy = Integer.parseInt(pos[1]);
                int wz = Integer.parseInt(pos[2]);
                BlockState state = entry.getValue();

                int cx = Math.floorDiv(wx, 16);
                int cz = Math.floorDiv(wz, 16);
                int lx = Math.floorMod(wx, 16);
                int lz = Math.floorMod(wz, 16);
                int idx = Chunk.getIndex(lx, wy, lz);

                ChunkPos cpos = new ChunkPos(cx, cz);
                chunkDeltas.computeIfAbsent(cpos, k -> new ConcurrentHashMap<>()).put(idx, state);
                modifiedBlockStates.put(entry.getKey(), state);
                modifiedBlocksLegacy.put(entry.getKey(), state.getLegacyId());

                Chunk chunk = chunks.get(cpos);
                if (chunk != null) {
                    chunk.setBlockState(lx, wy, lz, state);
                    dirtyChunks.add(cpos);
                    if (lx == 0) dirtyChunks.add(new ChunkPos(cx - 1, cz));
                    if (lx == 15) dirtyChunks.add(new ChunkPos(cx + 1, cz));
                    if (lz == 0) dirtyChunks.add(new ChunkPos(cx, cz - 1));
                    if (lz == 15) dirtyChunks.add(new ChunkPos(cx, cz + 1));
                }
            } catch (Exception e) {
                // Ignore malformed key
            }
        }

        for (ChunkPos cpos : dirtyChunks) {
            rebuildSingleMesh(cpos.x(), cpos.z());
        }
    }

    public BlockState getBlockStateAt(int wx, int wy, int wz) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return Blocks.AIR.getDefaultState();

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return Blocks.AIR.getDefaultState();

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        return chunk.getBlockState(lx, wy, lz);
    }

    public byte getBlockAt(int wx, int wy, int wz) {
        return getBlockStateAt(wx, wy, wz).getLegacyId();
    }

    public boolean setBlockStateAt(int wx, int wy, int wz, BlockState state) {
        if (wy < 0 || wy >= Chunk.HEIGHT || state == null) return false;

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        ChunkPos cpos = new ChunkPos(cx, cz);
        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);
        int idx = Chunk.getIndex(lx, wy, lz);

        chunkDeltas.computeIfAbsent(cpos, k -> new ConcurrentHashMap<>()).put(idx, state);
        modifiedBlockStates.put(wx + "," + wy + "," + wz, state);
        modifiedBlocksLegacy.put(wx + "," + wy + "," + wz, state.getLegacyId());

        Chunk chunk = getChunk(cx, cz);
        if (chunk != null) {
            chunk.setBlockState(lx, wy, lz, state);
        }

        // Rebuild mesh for this chunk and neighbors
        rebuildSingleMesh(cx, cz);

        if (lx == 0) rebuildSingleMesh(cx - 1, cz);
        if (lx == 15) rebuildSingleMesh(cx + 1, cz);
        if (lz == 0) rebuildSingleMesh(cx, cz - 1);
        if (lz == 15) rebuildSingleMesh(cx, cz + 1);

        if (lx == 0 && lz == 0) rebuildSingleMesh(cx - 1, cz - 1);
        if (lx == 0 && lz == 15) rebuildSingleMesh(cx - 1, cz + 1);
        if (lx == 15 && lz == 0) rebuildSingleMesh(cx + 1, cz - 1);
        if (lx == 15 && lz == 15) rebuildSingleMesh(cx + 1, cz + 1);

        return true;
    }

    public boolean setBlockAt(int wx, int wy, int wz, byte type) {
        return setBlockStateAt(wx, wy, wz, BlockStateRegistry.getDefaultState(type));
    }

    public void rebuildSingleMesh(int cx, int cz) {
        rebuildSingleMesh(new ChunkPos(cx, cz));
    }

    public void rebuildSingleMesh(ChunkPos pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null) {
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
            meshes.put(pos, mesh);
            pendingMeshUploads.offer(pos);
        }
    }

    public record BreakResult(byte blockType, BlockState blockState, String name, String color, int x, int y, int z) {}

    public BreakResult breakBlock(int wx, int wy, int wz) {
        BlockState current = getBlockStateAt(wx, wy, wz);
        if (current.isAir() || current.is(Blocks.BEDROCK) || current.is(Blocks.WATER)) {
            return null;
        }

        setBlockStateAt(wx, wy, wz, Blocks.AIR.getDefaultState());
        return new BreakResult(current.getLegacyId(), current, current.getBlock().getName(), current.getBlock().getColorHex(), wx, wy, wz);
    }

    public float getGroundHeight(float wx, float wz, Float currentY) {
        int rx = (int) Math.floor(wx);
        int rz = (int) Math.floor(wz);

        int startY = (currentY != null) ? Math.min(Chunk.HEIGHT - 2, (int) Math.floor(currentY + 0.6f)) : Chunk.HEIGHT - 2;

        for (int y = startY; y >= 0; y--) {
            BlockState block = getBlockStateAt(rx, y, rz);
            if (block.isSolid()) {
                return y + 1.0f;
            }
        }
        return 1.0f;
    }

    public void saveAllModifiedChunks() {
        if (regionManager == null) return;
        for (Chunk chunk : chunks.values()) {
            if (chunk != null && chunk.isDirty()) {
                regionManager.saveChunk(chunk);
            }
        }
    }

    public void shutdown() {
        saveAllModifiedChunks();
        chunkWorkers.shutdownNow();
        if (regionManager != null) {
            regionManager.close();
        }
    }
}
