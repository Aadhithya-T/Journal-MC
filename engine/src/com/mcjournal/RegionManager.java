package com.mcjournal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages open RegionFile handles across infinite chunk space.
 * Provides thread-safe chunk loading, saving, and cache management.
 */
public class RegionManager implements AutoCloseable {
    private final File regionDir;
    private final Map<RegionPos, RegionFile> openRegions = new ConcurrentHashMap<>();

    public RegionManager(File worldDirectory) {
        this.regionDir = new File(worldDirectory, "regions");
        this.regionDir.mkdirs();
    }

    private synchronized RegionFile getRegionFile(RegionPos pos) {
        return openRegions.computeIfAbsent(pos, p -> {
            try {
                File file = new File(regionDir, p.getFileName());
                return new RegionFile(file);
            } catch (IOException e) {
                System.err.println("[RegionManager] Failed to open region file " + p.getFileName() + ": " + e.getMessage());
                return null;
            }
        });
    }

    public Chunk loadChunk(int cx, int cz) {
        RegionPos rpos = RegionPos.fromChunkCoords(cx, cz);
        RegionFile region = getRegionFile(rpos);
        if (region == null) return null;

        try {
            byte[] data = region.readChunkData(rpos.getLocalChunkX(cx), rpos.getLocalChunkZ(cz));
            if (data == null) return null;
            return ChunkSerializer.deserialize(data);
        } catch (IOException e) {
            System.err.println("[RegionManager] Failed to read chunk (" + cx + ", " + cz + "): " + e.getMessage());
            return null;
        }
    }

    public boolean hasSavedChunk(int cx, int cz) {
        RegionPos rpos = RegionPos.fromChunkCoords(cx, cz);
        RegionFile region = getRegionFile(rpos);
        if (region == null) return false;
        return region.hasChunk(rpos.getLocalChunkX(cx), rpos.getLocalChunkZ(cz));
    }

    public void saveChunk(Chunk chunk) {
        if (chunk == null) return;
        RegionPos rpos = RegionPos.fromChunkCoords(chunk.getCx(), chunk.getCz());
        RegionFile region = getRegionFile(rpos);
        if (region == null) return;

        try {
            byte[] data = ChunkSerializer.serialize(chunk);
            region.writeChunkData(rpos.getLocalChunkX(chunk.getCx()), rpos.getLocalChunkZ(chunk.getCz()), data);
            chunk.setDirty(false);
        } catch (IOException e) {
            System.err.println("[RegionManager] Failed to save chunk (" + chunk.getCx() + ", " + chunk.getCz() + "): " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        for (RegionFile region : openRegions.values()) {
            try {
                if (region != null) region.close();
            } catch (IOException ignored) {}
        }
        openRegions.clear();
    }
}
