package com.mcjournal;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance 32x32 Chunk Region File Manager (.jmc).
 * Stores up to 1024 compressed chunks in a single random-access region file with 4 KB sector allocation.
 */
public class RegionFile implements AutoCloseable {
    public static final int CHUNKS_PER_AXIS = 32;
    public static final int TOTAL_CHUNKS = CHUNKS_PER_AXIS * CHUNKS_PER_AXIS; // 1024
    public static final int SECTOR_SIZE = 4096;
    public static final int HEADER_SECTORS = 2; // 8192 bytes (Table of offsets + Table of lengths)

    private final File file;
    private RandomAccessFile raf;
    private final int[] sectorOffsets = new int[TOTAL_CHUNKS];
    private final int[] chunkLengths = new int[TOTAL_CHUNKS];
    private final List<Boolean> sectorFreeList = new ArrayList<>();

    public RegionFile(File file) throws IOException {
        this.file = file;
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();

        this.raf = new RandomAccessFile(file, "rw");

        if (raf.length() < HEADER_SECTORS * SECTOR_SIZE) {
            // Initialize fresh empty 8 KB header
            byte[] emptyHeader = new byte[HEADER_SECTORS * SECTOR_SIZE];
            raf.write(emptyHeader);
            sectorFreeList.add(false); // Header sector 0
            sectorFreeList.add(false); // Header sector 1
        } else {
            // Read existing header
            raf.seek(0);
            for (int i = 0; i < TOTAL_CHUNKS; i++) {
                sectorOffsets[i] = raf.readInt();
            }
            for (int i = 0; i < TOTAL_CHUNKS; i++) {
                chunkLengths[i] = raf.readInt();
            }

            // Build sector free list based on file length
            int numSectors = (int) (raf.length() / SECTOR_SIZE);
            for (int i = 0; i < numSectors; i++) {
                sectorFreeList.add(true);
            }
            if (sectorFreeList.size() >= 2) {
                sectorFreeList.set(0, false);
                sectorFreeList.set(1, false);
            }

            // Mark allocated sectors as occupied
            for (int i = 0; i < TOTAL_CHUNKS; i++) {
                int offset = sectorOffsets[i];
                int len = chunkLengths[i];
                if (offset >= HEADER_SECTORS && len > 0) {
                    int sectorsNeeded = (len + SECTOR_SIZE - 1) / SECTOR_SIZE;
                    for (int s = 0; s < sectorsNeeded && (offset + s) < sectorFreeList.size(); s++) {
                        sectorFreeList.set(offset + s, false);
                    }
                }
            }
        }
    }

    private static int getIndex(int localCx, int localCz) {
        return (localCz & 31) * CHUNKS_PER_AXIS + (localCx & 31);
    }

    public synchronized boolean hasChunk(int localCx, int localCz) {
        int idx = getIndex(localCx, localCz);
        return sectorOffsets[idx] >= HEADER_SECTORS && chunkLengths[idx] > 0;
    }

    public synchronized byte[] readChunkData(int localCx, int localCz) throws IOException {
        int idx = getIndex(localCx, localCz);
        int offset = sectorOffsets[idx];
        int length = chunkLengths[idx];

        if (offset < HEADER_SECTORS || length <= 0) {
            return null;
        }

        long fileOffset = (long) offset * SECTOR_SIZE;
        if (fileOffset + length > raf.length()) {
            return null; // File truncated / corrupted entry
        }

        raf.seek(fileOffset);
        byte[] data = new byte[length];
        raf.readFully(data);
        return data;
    }

    public synchronized void writeChunkData(int localCx, int localCz, byte[] data) throws IOException {
        if (data == null || data.length == 0) return;

        int idx = getIndex(localCx, localCz);
        int oldOffset = sectorOffsets[idx];
        int oldLength = chunkLengths[idx];

        int sectorsNeeded = (data.length + SECTOR_SIZE - 1) / SECTOR_SIZE;

        // Free old sectors if modifying existing chunk
        if (oldOffset >= HEADER_SECTORS && oldLength > 0) {
            int oldSectors = (oldLength + SECTOR_SIZE - 1) / SECTOR_SIZE;
            for (int s = 0; s < oldSectors && (oldOffset + s) < sectorFreeList.size(); s++) {
                sectorFreeList.set(oldOffset + s, true);
            }
        }

        // Find consecutive free sectors or append to end of file
        int targetSector = findFreeSectors(sectorsNeeded);

        // Mark sectors as allocated
        while (sectorFreeList.size() < targetSector + sectorsNeeded) {
            sectorFreeList.add(true);
        }
        for (int s = 0; s < sectorsNeeded; s++) {
            sectorFreeList.set(targetSector + s, false);
        }

        // Write chunk payload to target sector
        long fileOffset = (long) targetSector * SECTOR_SIZE;
        raf.seek(fileOffset);
        raf.write(data);

        // Write zero padding to fill out sector
        int remainder = data.length % SECTOR_SIZE;
        if (remainder != 0) {
            byte[] padding = new byte[SECTOR_SIZE - remainder];
            raf.write(padding);
        }

        // Update in-memory tables and write back header entry
        sectorOffsets[idx] = targetSector;
        chunkLengths[idx] = data.length;

        // Write offset entry to header (sector 0)
        raf.seek((long) idx * 4);
        raf.writeInt(targetSector);

        // Write length entry to header (sector 1)
        raf.seek((long) (TOTAL_CHUNKS + idx) * 4);
        raf.writeInt(data.length);
    }

    private int findFreeSectors(int needed) {
        int consecutive = 0;
        int startSector = -1;

        for (int i = HEADER_SECTORS; i < sectorFreeList.size(); i++) {
            if (sectorFreeList.get(i)) {
                if (consecutive == 0) startSector = i;
                consecutive++;
                if (consecutive >= needed) {
                    return startSector;
                }
            } else {
                consecutive = 0;
            }
        }

        // If no existing gap is large enough, allocate at the end of the file
        return sectorFreeList.size();
    }

    @Override
    public synchronized void close() throws IOException {
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }
}
