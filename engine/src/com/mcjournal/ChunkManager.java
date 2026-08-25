package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {
    private final int radius;
    private final TerrainGenerator generator;
    private final ConcurrentMap<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ChunkMeshBuilder.MeshData> meshes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BlockState> modifiedBlockStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Byte> modifiedBlocksLegacy = new ConcurrentHashMap<>();
    private final Set<String> solidObstacles = ConcurrentHashMap.newKeySet();

    public ChunkManager(int radiusChunks, long seed) {
        this.radius = radiusChunks;
        this.generator = new TerrainGenerator(seed);
        initWorld();
    }

    public static String getChunkKey(int cx, int cz) {
        return cx + "," + cz;
    }

    private void initWorld() {
        // Phase 1: Generate Chunk Block Data in Parallel
        List<CompletableFuture<Void>> genFutures = new ArrayList<>();
        for (int cx = -radius; cx < radius; cx++) {
            for (int cz = -radius; cz < radius; cz++) {
                final int finalCx = cx;
                final int finalCz = cz;
                genFutures.add(CompletableFuture.runAsync(() -> {
                    Chunk chunk = generator.generateChunk(finalCx, finalCz);
                    chunks.put(getChunkKey(finalCx, finalCz), chunk);
                }));
            }
        }
        CompletableFuture.allOf(genFutures.toArray(new CompletableFuture[0])).join();

        // Phase 2: Compute Full Chunk Meshes with Ambient Occlusion in Parallel
        buildAllMeshes();
    }

    public void buildAllMeshes() {
        List<CompletableFuture<Void>> meshFutures = new ArrayList<>();
        for (Chunk chunk : chunks.values()) {
            meshFutures.add(CompletableFuture.runAsync(() -> {
                ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
                meshes.put(getChunkKey(chunk.getCx(), chunk.getCz()), mesh);
            }));
        }
        CompletableFuture.allOf(meshFutures.toArray(new CompletableFuture[0])).join();
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
        Set<String> dirtyChunkKeys = new HashSet<>();

        for (Map.Entry<String, BlockState> entry : deltas.entrySet()) {
            try {
                String[] pos = entry.getKey().split(",");
                int wx = Integer.parseInt(pos[0]);
                int wy = Integer.parseInt(pos[1]);
                int wz = Integer.parseInt(pos[2]);
                BlockState state = entry.getValue();

                int cx = Math.floorDiv(wx, 16);
                int cz = Math.floorDiv(wz, 16);
                Chunk chunk = getChunk(cx, cz);
                if (chunk != null) {
                    int lx = Math.floorMod(wx, 16);
                    int lz = Math.floorMod(wz, 16);
                    chunk.setBlockState(lx, wy, lz, state);
                    modifiedBlockStates.put(entry.getKey(), state);
                    modifiedBlocksLegacy.put(entry.getKey(), state.getLegacyId());

                    dirtyChunkKeys.add(cx + "," + cz);
                    if (lx == 0) dirtyChunkKeys.add((cx - 1) + "," + cz);
                    if (lx == 15) dirtyChunkKeys.add((cx + 1) + "," + cz);
                    if (lz == 0) dirtyChunkKeys.add(cx + "," + (cz - 1));
                    if (lz == 15) dirtyChunkKeys.add(cx + "," + (cz + 1));
                }
            } catch (Exception e) {
                // Ignore malformed key
            }
        }

        // Rebuild meshes for all modified chunks
        for (String key : dirtyChunkKeys) {
            String[] parts = key.split(",");
            rebuildSingleMesh(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(getChunkKey(cx, cz));
    }

    public Collection<Chunk> getAllChunks() {
        return chunks.values();
    }

    public ChunkMeshBuilder.MeshData getChunkMesh(int cx, int cz) {
        return meshes.get(getChunkKey(cx, cz));
    }

    public Map<String, ChunkMeshBuilder.MeshData> getAllMeshes() {
        return meshes;
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
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return false;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        chunk.setBlockState(lx, wy, lz, state);
        modifiedBlockStates.put(wx + "," + wy + "," + wz, state);
        modifiedBlocksLegacy.put(wx + "," + wy + "," + wz, state.getLegacyId());

        // Rebuild mesh for this chunk
        rebuildSingleMesh(cx, cz);

        // Rebuild neighbor chunk meshes if on chunk boundary
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
        Chunk chunk = getChunk(cx, cz);
        if (chunk != null) {
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
            meshes.put(getChunkKey(cx, cz), mesh);
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
}
