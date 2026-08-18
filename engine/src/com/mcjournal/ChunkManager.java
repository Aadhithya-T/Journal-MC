package com.mcjournal;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {
    private final int radius;
    private final TerrainGenerator generator;
    private final ConcurrentMap<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ChunkMeshBuilder.MeshData> meshes = new ConcurrentHashMap<>();
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

    public byte getBlockAt(int wx, int wy, int wz) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return Block.AIR;

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return Block.AIR;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        return chunk.getBlock(lx, wy, lz);
    }

    public boolean setBlockAt(int wx, int wy, int wz, byte type) {
        if (wy < 0 || wy >= Chunk.HEIGHT) return false;

        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return false;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        chunk.setBlock(lx, wy, lz, type);

        // Rebuild mesh for this chunk
        rebuildSingleMesh(cx, cz);

        // Rebuild neighbor chunk meshes if on chunk boundary
        if (lx == 0) rebuildSingleMesh(cx - 1, cz);
        if (lx == 15) rebuildSingleMesh(cx + 1, cz);
        if (lz == 0) rebuildSingleMesh(cx, cz - 1);
        if (lz == 15) rebuildSingleMesh(cx, cz + 1);

        return true;
    }

    public void rebuildSingleMesh(int cx, int cz) {
        Chunk chunk = getChunk(cx, cz);
        if (chunk != null) {
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, this);
            meshes.put(getChunkKey(cx, cz), mesh);
        }
    }

    public record BreakResult(byte blockType, String name, String color, int x, int y, int z) {}

    public BreakResult breakBlock(int wx, int wy, int wz) {
        byte current = getBlockAt(wx, wy, wz);
        if (current == Block.AIR || current == Block.BEDROCK || current == Block.WATER) {
            return null;
        }

        setBlockAt(wx, wy, wz, Block.AIR);
        return new BreakResult(current, Block.getName(current), Block.getColor(current), wx, wy, wz);
    }

    public float getGroundHeight(float wx, float wz, Float currentY) {
        int rx = (int) Math.floor(wx);
        int rz = (int) Math.floor(wz);

        int startY = (currentY != null) ? Math.min(28, (int) Math.floor(currentY + 0.6f)) : 28;

        for (int y = startY; y >= 0; y--) {
            byte block = getBlockAt(rx, y, rz);
            if (Block.isSolid(block)) {
                return y + 1.0f;
            }
        }

        return 2.0f;
    }

    public boolean isCollidingWithSolid(float px, float py, float pz, float radius, float height) {
        int minBX = (int) Math.floor(px - radius);
        int maxBX = (int) Math.floor(px + radius);
        int minBZ = (int) Math.floor(pz - radius);
        int maxBZ = (int) Math.floor(pz + radius);

        int minBY = (int) Math.floor(py + 0.15f);
        int maxBY = (int) Math.floor(py + height - 0.05f);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int bz = minBZ; bz <= maxBZ; bz++) {
                for (int by = minBY; by <= maxBY; by++) {
                    if (solidObstacles.contains(bx + "," + by + "," + bz)) {
                        return true;
                    }

                    byte block = getBlockAt(bx, by, bz);
                    if (Block.isSolid(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<TerrainGenerator.POI> getPois() {
        return generator.getPois();
    }
}
