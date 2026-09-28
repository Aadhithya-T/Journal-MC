package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * High-level coordinator/facade for chunk management in the voxel world.
 * Delegates specialized tasks to:
 * - ChunkStreamer (spiral ordering, player tracking, distance calculations)
 * - ChunkPersistence (disk I/O and dirty saving)
 * - ChunkGenerator (terrain generation and voxel delta overlay)
 * - ChunkMesher (voxel polygonization and neighbor boundary rebuilds)
 * - ChunkUploadQueue (GPU upload and unload queues)
 */
public class ChunkManager {
    public static final int DEFAULT_RENDER_DISTANCE = 12; // 12 chunks = 192 blocks radius
    public static final int UNLOAD_PADDING = 3;            // Unload at renderDistance + 3 (15 chunks)

    private final ChunkStreamer streamer;
    private final ChunkPersistence persistence;
    private final ChunkGenerator generator;
    private final ChunkUploadQueue uploadQueue;
    private final ChunkMesher mesher;

    // Shared chunk data structures
    private final ConcurrentMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkPos, ChunkMeshBuilder.MeshData> meshes = new ConcurrentHashMap<>();
    private final ConcurrentMap<WorldBlockPos, BlockState> modifiedBlockStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChunkPos, ChunkStatus> chunkStatuses = new ConcurrentHashMap<>();
    private final DirtyChunkSet dirtyChunks = new DirtyChunkSet();

    private final ExecutorService chunkWorkers;

    public ChunkManager(long seed) {
        this(DEFAULT_RENDER_DISTANCE, seed, null);
    }

    public ChunkManager(int renderDistance, long seed) {
        this(renderDistance, seed, null);
    }

    public ChunkManager(int renderDistance, long seed, File worldDir) {
        this.streamer = new ChunkStreamer(renderDistance);
        this.persistence = new ChunkPersistence(worldDir);
        this.generator = new ChunkGenerator(seed, this.persistence);
        this.uploadQueue = new ChunkUploadQueue();
        this.mesher = new ChunkMesher(this.chunks, this.meshes, this.chunkStatuses, this.uploadQueue);

        int numWorkers = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 2, 8);
        this.chunkWorkers = Executors.newFixedThreadPool(numWorkers, r -> {
            Thread t = new Thread(r, "ChunkWorker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setRenderDistance(int newDistance) {
        streamer.setRenderDistance(newDistance);
    }

    public int getRenderDistance() {
        return streamer.getRenderDistance();
    }

    public TerrainGenerator getGenerator() {
        return generator.getTerrainGenerator();
    }

    public ChunkStreamer getStreamer() {
        return streamer;
    }

    public ChunkPersistence getPersistence() {
        return persistence;
    }

    public ChunkGenerator getChunkGenerator() {
        return generator;
    }

    public ChunkMesher getMesher() {
        return mesher;
    }

    public ChunkUploadQueue getUploadQueue() {
        return uploadQueue;
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
                chunkStatuses.put(pos, ChunkStatus.QUEUED);

                genFutures.add(CompletableFuture.runAsync(() -> {
                    chunkStatuses.put(pos, ChunkStatus.GENERATING);
                    Chunk chunk = generator.generateChunk(pos);
                    chunks.put(pos, chunk);
                    chunkStatuses.put(pos, ChunkStatus.GENERATED);
                }, chunkWorkers));
            }
        }

        CompletableFuture.allOf(genFutures.toArray(new CompletableFuture[0])).join();

        // Mesh initial chunks in parallel
        List<CompletableFuture<Void>> meshFutures = new ArrayList<>();
        for (ChunkPos pos : initialPositions) {
            meshFutures.add(CompletableFuture.runAsync(() -> {
                mesher.meshChunk(pos, this);
            }, chunkWorkers));
        }

        CompletableFuture.allOf(meshFutures.toArray(new CompletableFuture[0])).join();
        streamer.setLastPlayerChunkPos(new ChunkPos(spawnCx, spawnCz));
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
        int unloadDistance = streamer.getUnloadDistance();

        // 1. Queue generation for chunks in render distance (prioritized by look direction and distance)
        PriorityQueue<ChunkPos> loadQueue = streamer.createPriorityQueue(currentPos, lookDirX, lookDirZ);
        for (ChunkStreamer.ChunkOffset offset : streamer.getSpiralOffsets()) {
            ChunkPos pos = new ChunkPos(playerCx + offset.dx(), playerCz + offset.dz());
            ChunkStatus status = chunkStatuses.get(pos);
            if (status != null && status.isUnloading()) continue;
            if (!chunks.containsKey(pos) && chunkStatuses.putIfAbsent(pos, ChunkStatus.QUEUED) == null) {
                loadQueue.add(pos);
            }
        }

        while (!loadQueue.isEmpty()) {
            ChunkPos pos = loadQueue.poll();
            submitWorkerTask(() -> {
                try {
                    // Cancellation check: abort if player moved away before task executed
                    if (chunkStatuses.get(pos) != ChunkStatus.QUEUED) return;
                    ChunkPos lastPos = streamer.getLastPlayerChunkPos();
                    if (lastPos != null && pos.distanceChebyshev(lastPos) > unloadDistance) {
                        chunkStatuses.remove(pos);
                        return;
                    }

                    chunkStatuses.put(pos, ChunkStatus.GENERATING);
                    Chunk chunk = generator.generateChunk(pos);
                    chunks.put(pos, chunk);
                    chunkStatuses.put(pos, ChunkStatus.GENERATED);

                    // Cancellation check before meshing
                    if (lastPos != null && pos.distanceChebyshev(lastPos) > unloadDistance) {
                        chunkStatuses.remove(pos);
                        return;
                    }

                    mesher.meshChunk(pos, this);

                    // Rebuild loaded neighbor boundary meshes
                    mesher.rebuildNeighborIfLoaded(new ChunkPos(pos.x() - 1, pos.z()), this, chunkWorkers);
                    mesher.rebuildNeighborIfLoaded(new ChunkPos(pos.x() + 1, pos.z()), this, chunkWorkers);
                    mesher.rebuildNeighborIfLoaded(new ChunkPos(pos.x(), pos.z() - 1), this, chunkWorkers);
                    mesher.rebuildNeighborIfLoaded(new ChunkPos(pos.x(), pos.z() + 1), this, chunkWorkers);
                } catch (Exception e) {
                    chunkStatuses.remove(pos);
                    e.printStackTrace();
                }
            });
        }

        // 2. Unload chunks beyond unload distance
        ChunkPos lastPos = streamer.getLastPlayerChunkPos();
        if (lastPos == null || !lastPos.equals(currentPos)) {
            streamer.setLastPlayerChunkPos(currentPos);
            for (ChunkPos pos : streamer.computeChunksToUnload(currentPos, chunks.keySet())) {
                ChunkStatus status = chunkStatuses.get(pos);
                if (status != null && status.isUnloading()) continue;
                chunkStatuses.put(pos, ChunkStatus.UNLOAD_QUEUED);
                uploadQueue.queueUnload(pos);
            }
        }
    }

    public ChunkPos pollPendingMeshUpload() {
        return uploadQueue.pollUpload();
    }

    public ChunkPos pollPendingMeshUnload() {
        return uploadQueue.pollUnload();
    }

    public boolean hasPendingUploads() {
        return uploadQueue.hasPendingUploads();
    }

    public boolean hasPendingUnloads() {
        return uploadQueue.hasPendingUnloads();
    }

    public int getPendingUploadCount() {
        return uploadQueue.getPendingUploadCount();
    }

    public int getPendingUnloadCount() {
        return uploadQueue.getPendingUnloadCount();
    }

    public Map<ChunkStatus, Integer> getChunkStatusCounts() {
        Map<ChunkStatus, Integer> counts = new EnumMap<>(ChunkStatus.class);
        for (ChunkStatus status : ChunkStatus.values()) {
            counts.put(status, 0);
        }
        for (ChunkStatus status : chunkStatuses.values()) {
            counts.put(status, counts.get(status) + 1);
        }
        return counts;
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public boolean isChunkLoaded(int cx, int cz) {
        return isChunkLoaded(new ChunkPos(cx, cz));
    }

    public boolean isChunkLoaded(ChunkPos pos) {
        ChunkStatus status = chunkStatuses.get(pos);
        return status != null && !status.isUnloading() && status != ChunkStatus.UNLOADED && chunks.containsKey(pos);
    }

    public ChunkStatus getChunkStatus(int cx, int cz) {
        return getChunkStatus(new ChunkPos(cx, cz));
    }

    public ChunkStatus getChunkStatus(ChunkPos pos) {
        return chunkStatuses.getOrDefault(pos, ChunkStatus.UNLOADED);
    }

    public void markGpuLoaded(ChunkPos pos) {
        if (chunkStatuses.containsKey(pos)) {
            chunkStatuses.put(pos, ChunkStatus.GPU_LOADED);
        }
    }

    public void confirmGpuUnloaded(ChunkPos pos) {
        chunkStatuses.put(pos, ChunkStatus.GPU_UNLOADED);
        Chunk chunk = chunks.remove(pos);
        if (chunk != null && chunk.isDirty()) {
            submitWorkerTask(() -> persistence.saveToDisk(chunk));
        }
        meshes.remove(pos);
        chunkStatuses.remove(pos); // -> UNLOADED (absent = unloaded)
    }

    public Chunk getChunk(int cx, int cz) {
        return getChunk(new ChunkPos(cx, cz));
    }

    public Chunk getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }

    public void setChunk(ChunkPos pos, Chunk chunk) {
        if (chunk != null) {
            chunks.put(pos, chunk);
            chunkStatuses.put(pos, ChunkStatus.GENERATED);
        } else {
            chunks.remove(pos);
            chunkStatuses.remove(pos);
        }
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

    public Map<WorldBlockPos, BlockState> getModifiedBlockStates() {
        return modifiedBlockStates;
    }

    public Map<String, BlockState> getModifiedBlockStatesAsStringMap() {
        Map<String, BlockState> result = new HashMap<>();
        for (Map.Entry<WorldBlockPos, BlockState> entry : modifiedBlockStates.entrySet()) {
            result.put(entry.getKey().toStringKey(), entry.getValue());
        }
        return result;
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
        Map<WorldBlockPos, BlockState> blockPosDeltas = new HashMap<>();
        for (Map.Entry<String, BlockState> entry : deltas.entrySet()) {
            try {
                blockPosDeltas.put(WorldBlockPos.fromStringKey(entry.getKey()), entry.getValue());
            } catch (Exception e) {
                // Ignore malformed key
            }
        }
        applyModifiedWorldBlockStates(blockPosDeltas);
    }

    public void applyModifiedWorldBlockStates(Map<WorldBlockPos, BlockState> deltas) {
        if (deltas == null || deltas.isEmpty()) return;
        Set<ChunkPos> dirtyChunks = new HashSet<>();

        for (Map.Entry<WorldBlockPos, BlockState> entry : deltas.entrySet()) {
            WorldBlockPos pos = entry.getKey();
            if (pos == null) continue;
            int wx = pos.x();
            int wy = pos.y();
            int wz = pos.z();
            BlockState state = entry.getValue();

            int cx = Math.floorDiv(wx, 16);
            int cz = Math.floorDiv(wz, 16);
            int lx = Math.floorMod(wx, 16);
            int lz = Math.floorMod(wz, 16);
            int idx = Chunk.getIndex(lx, wy, lz);

            ChunkPos cpos = new ChunkPos(cx, cz);
            generator.registerDelta(cpos, idx, state);
            modifiedBlockStates.put(pos, state);

            Chunk chunk = chunks.get(cpos);
            if (chunk != null) {
                chunk.setBlockState(lx, wy, lz, state);
                dirtyChunks.add(cpos);
                if (lx == 0) dirtyChunks.add(new ChunkPos(cx - 1, cz));
                if (lx == 15) dirtyChunks.add(new ChunkPos(cx + 1, cz));
                if (lz == 0) dirtyChunks.add(new ChunkPos(cx, cz - 1));
                if (lz == 15) dirtyChunks.add(new ChunkPos(cx, cz + 1));
            }
        }

        for (ChunkPos cpos : dirtyChunks) {
            rebuildSingleMesh(cpos.x(), cpos.z());
        }
    }

    public int getStateIdAt(int wx, int wy, int wz) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return 0;

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return 0;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        return chunk.getStateId(lx, wy, lz);
    }

    public boolean setStateIdAt(int wx, int wy, int wz, int stateId) {
        BlockState state = BlockStateRegistry.getStateById(stateId);
        return setBlockStateAt(wx, wy, wz, state);
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

    public boolean setBlockStateAt(int wx, int wy, int wz, BlockState state) {
        if (wy < 0 || wy >= Chunk.HEIGHT || state == null) return false;

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        ChunkPos cpos = new ChunkPos(cx, cz);
        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);
        int idx = Chunk.getIndex(lx, wy, lz);

        generator.registerDelta(cpos, idx, state);
        modifiedBlockStates.put(new WorldBlockPos(wx, wy, wz), state);

        Chunk chunk = getChunk(cx, cz);
        if (chunk != null) {
            chunk.setBlockState(lx, wy, lz, state);
        }

        // Track dirty chunk and boundary neighbors for asynchronous deduplicated meshing
        dirtyChunks.markBlockModified(wx, wy, wz);

        return true;
    }

    public DirtyChunkSet getDirtyChunks() {
        return dirtyChunks;
    }

    public boolean hasDirtyChunks() {
        return !dirtyChunks.isEmpty();
    }

    /**
     * Drains all dirty chunks accumulated from block mutations and fluid updates,
     * deduplicates them, and dispatches background mesh rebuilds across the worker pool.
     */
    public void processDirtyChunks() {
        Set<ChunkPos> dirty = dirtyChunks.drainDirtyChunks();
        if (dirty.isEmpty()) return;

        for (ChunkPos pos : dirty) {
            Chunk chunk = chunks.get(pos);
            ChunkStatus status = chunkStatuses.getOrDefault(pos, ChunkStatus.UNLOADED);
            if (chunk != null && !status.isUnloading()) {
                submitWorkerTask(() -> {
                    if (chunks.containsKey(pos)) {
                        chunkStatuses.put(pos, ChunkStatus.MESHING);
                        ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(ChunkNeighborhood.of(chunk, this));
                        meshes.put(pos, mesh);
                        uploadQueue.queueUpload(pos, mesh.getTotalBytes());
                        chunkStatuses.put(pos, ChunkStatus.MESHED);
                    }
                });
            }
        }
    }

    /**
     * Synchronously flushes all dirty chunks, rebuilding their meshes immediately.
     */
    public void flushDirtyChunks() {
        Set<ChunkPos> dirty = dirtyChunks.drainDirtyChunks();
        for (ChunkPos pos : dirty) {
            Chunk chunk = chunks.get(pos);
            if (chunk != null) {
                chunkStatuses.put(pos, ChunkStatus.MESHING);
                ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(ChunkNeighborhood.of(chunk, this));
                meshes.put(pos, mesh);
                uploadQueue.queueUpload(pos, mesh.getTotalBytes());
                chunkStatuses.put(pos, ChunkStatus.MESHED);
            }
        }
    }

    /**
     * Drains pending GPU uploads with strict per-frame upload count and byte budgets.
     */
    public int processGpuUploads(com.mcjournal.client.ChunkRenderer renderer) {
        if (renderer == null) return 0;
        int uploads = 0;
        int bytesUploaded = 0;
        int maxUploads = uploadQueue.getMaxUploadsPerFrame();
        int maxBytes = uploadQueue.getMaxBytesPerFrame();

        while (uploadQueue.hasPendingUploads()) {
            if (uploads >= maxUploads) break;
            if (uploads > 0 && bytesUploaded >= maxBytes) break;

            ChunkUploadQueue.UploadTask task = uploadQueue.pollUploadTask();
            if (task == null) break;

            ChunkPos pos = task.pos();
            ChunkMeshBuilder.MeshData mesh = meshes.get(pos);
            if (mesh != null) {
                renderer.uploadChunkMesh(pos, mesh);
                markGpuLoaded(pos);
                bytesUploaded += mesh.getTotalBytes();
                uploads++;
            }
        }
        return uploads;
    }

    /**
     * Drains and confirms all pending GPU unloads.
     */
    public int processGpuUnloads(com.mcjournal.client.ChunkRenderer renderer) {
        if (renderer == null) return 0;
        int unloads = 0;
        ChunkPos unloadPos;
        while ((unloadPos = uploadQueue.pollUnload()) != null) {
            renderer.unloadChunkMesh(unloadPos);
            confirmGpuUnloaded(unloadPos);
            unloads++;
        }
        return unloads;
    }

    public void rebuildSingleMesh(int cx, int cz) {
        rebuildSingleMesh(new ChunkPos(cx, cz));
    }

    public void rebuildSingleMesh(ChunkPos pos) {
        mesher.rebuildSingleMesh(pos, this);
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
        persistence.saveAllDirty(chunks.values());
    }

    public java.util.concurrent.CompletableFuture<Void> saveAllModifiedChunksAsync() {
        return persistence.saveAllDirtyAsync(chunks.values());
    }

    public void shutdown() {
        saveAllModifiedChunks();
        chunkWorkers.shutdownNow();
        persistence.close();
    }
}
