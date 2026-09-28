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
    private final short[] stateIds;
    private boolean isDirty = false;

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        this.stateIds = new short[TOTAL_VOXELS];
    }

    public Chunk(int cx, int cz, short[] stateIds) {
        this.cx = cx;
        this.cz = cz;
        this.stateIds = (stateIds != null) ? stateIds : new short[TOTAL_VOXELS];
    }

    @Deprecated
    public Chunk(int cx, int cz, byte[] legacyBlocks) {
        this.cx = cx;
        this.cz = cz;
        this.stateIds = new short[TOTAL_VOXELS];
        if (legacyBlocks != null) {
            int len = Math.min(legacyBlocks.length, TOTAL_VOXELS);
            for (int i = 0; i < len; i++) {
                this.stateIds[i] = (short) BlockStateRegistry.getDefaultState(legacyBlocks[i]).getStateId();
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

    public short[] getStateIds() {
        return stateIds;
    }

    public short[] getBlockStates() {
        return stateIds;
    }

    public static int getIndex(int x, int y, int z) {
        return (y * SIZE + z) * SIZE + x;
    }

    public int getStateId(int x, int y, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return 0; // Air state ID
        }
        return stateIds[getIndex(x, y, z)] & 0xFFFF;
    }

    public void setStateId(int x, int y, int z, int stateId) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return;
        }
        stateIds[getIndex(x, y, z)] = (short) stateId;
        isDirty = true;
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE || y < 0 || y >= HEIGHT) {
            return Blocks.AIR.getDefaultState();
        }
        return BlockStateRegistry.getStateById(getStateId(x, y, z));
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        if (state == null) return;
        setStateId(x, y, z, state.getStateId());
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
