package com.mcjournal;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {
    private final int radius;
    private final TerrainGenerator generator;
    private final ConcurrentMap<String, Chunk> chunks = new ConcurrentHashMap<>();

    public ChunkManager(int radiusChunks, long seed) {
        this.radius = radiusChunks;
        this.generator = new TerrainGenerator(seed);
        initWorld();
    }

    public static String getChunkKey(int cx, int cz) {
        return cx + "," + cz;
    }

    private void initWorld() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int cx = -radius; cx < radius; cx++) {
            for (int cz = -radius; cz < radius; cz++) {
                final int finalCx = cx;
                final int finalCz = cz;
                futures.add(CompletableFuture.runAsync(() -> {
                    Chunk chunk = generator.generateChunk(finalCx, finalCz);
                    chunks.put(getChunkKey(finalCx, finalCz), chunk);
                }));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(getChunkKey(cx, cz));
    }

    public Collection<Chunk> getAllChunks() {
        return chunks.values();
    }

    public byte getBlockAt(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return Block.AIR;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        return chunk.getBlock(lx, wy, lz);
    }

    public boolean setBlockAt(int wx, int wy, int wz, byte type) {
        int cx = Math.floorDiv(wx, 16);
        int cz = Math.floorDiv(wz, 16);
        Chunk chunk = getChunk(cx, cz);
        if (chunk == null) return false;

        int lx = Math.floorMod(wx, 16);
        int lz = Math.floorMod(wz, 16);

        chunk.setBlock(lx, wy, lz, type);
        return true;
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

    public List<TerrainGenerator.POI> getPois() {
        return generator.getPois();
    }
}
