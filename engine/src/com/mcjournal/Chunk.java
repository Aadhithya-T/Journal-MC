package com.mcjournal;

import java.util.Arrays;
import java.util.Base64;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 256; // Minecraft 1.17 Standard Full World Height (0 to 256)
    public static final int TOTAL_VOXELS = SIZE * SIZE * HEIGHT; // 65,536 voxels per chunk

    private final int cx;
    private final int cz;
    private final byte[] blocks;
    private boolean isDirty = false;

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.blocks = new byte[TOTAL_VOXELS];
    }

    public Chunk(int cx, int cz, byte[] blocks) {
        this.cx = cx;
        this.cz = cz;
        this.blocks = blocks;
    }

    public int getCx() {
        return cx;
    }

    public int getCz() {
        return cz;
    }

    public byte[] getBlocks() {
        return blocks;
    }

    public static int getIndex(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return Block.AIR;
        }
        return blocks[getIndex(x, y, z)];
    }

    public void setBlock(int x, int y, int z, byte type) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return;
        }
        blocks[getIndex(x, y, z)] = type;
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(blocks);
    }
}
