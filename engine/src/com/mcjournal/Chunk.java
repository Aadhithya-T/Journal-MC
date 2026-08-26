package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.Blocks;

import java.util.Base64;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 256; // Minecraft 1.17 Standard Full World Height (0 to 256)
    public static final int TOTAL_VOXELS = SIZE * SIZE * HEIGHT; // 65,536 voxels per chunk

    private final int cx;
    private final int cz;
    private final short[] blockStates;
    private boolean isDirty = false;

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.blockStates = new short[TOTAL_VOXELS];
    }

    public Chunk(int cx, int cz, short[] states) {
        this.cx = cx;
        this.cz = cz;
        this.blockStates = (states != null) ? states : new short[TOTAL_VOXELS];
    }

    public Chunk(int cx, int cz, byte[] legacyBlocks) {
        this.cx = cx;
        this.cz = cz;
        this.blockStates = new short[TOTAL_VOXELS];
        if (legacyBlocks != null) {
            int len = Math.min(legacyBlocks.length, TOTAL_VOXELS);
            for (int i = 0; i < len; i++) {
                this.blockStates[i] = (short) BlockStateRegistry.getDefaultState(legacyBlocks[i]).getStateId();
            }
        }
    }

    public int getCx() {
        return cx;
    }

    public int getCz() {
        return cz;
    }

    public ChunkPos getPos() {
        return new ChunkPos(cx, cz);
    }

    public short[] getBlockStates() {
        return blockStates;
    }

    public static int getIndex(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return Blocks.AIR.getDefaultState();
        }
        return BlockStateRegistry.getStateById(blockStates[getIndex(x, y, z)] & 0xFFFF);
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT || state == null) {
            return;
        }
        blockStates[getIndex(x, y, z)] = (short) state.getStateId();
        isDirty = true;
    }

    public byte getBlock(int x, int y, int z) {
        return getBlockState(x, y, z).getLegacyId();
    }

    public void setBlock(int x, int y, int z, byte type) {
        setBlockState(x, y, z, BlockStateRegistry.getDefaultState(type));
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
