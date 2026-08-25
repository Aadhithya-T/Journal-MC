package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;

public final class Block {
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte COBBLESTONE = 4;
    public static final byte SAND = 5;
    public static final byte BEDROCK = 6;
    public static final byte OAK_LOG = 7;
    public static final byte OAK_LEAVES = 8;
    public static final byte DIAMOND_ORE = 9;
    public static final byte WATER = 10;
    public static final byte BIRCH_LOG = 11;
    public static final byte BIRCH_LEAVES = 12;
    public static final byte TALL_GRASS = 13;
    public static final byte POPPY = 14;
    public static final byte DANDELION = 15;

    private Block() {}

    public static BlockState getState(int type) {
        return BlockStateRegistry.getDefaultState((byte) type);
    }

    public static BlockType getType(int type) {
        return BlockStateRegistry.getBlockType((byte) type);
    }

    public static String getName(int type) {
        return getType(type).getName();
    }

    public static String getColor(int type) {
        return getType(type).getColorHex();
    }

    public static float getHardness(int type) {
        return getType(type).getHardness();
    }

    public static byte getDrop(int type) {
        return getType(type).getDrop(getState(type)).getLegacyId();
    }

    public static int getDisplayFaceTile(int type) {
        return getType(type).getDisplayFaceTile();
    }

    public static int getBlockFaceSlot(int blockType, int faceIndex) {
        return getState(blockType).getFaceTextureSlot(faceIndex);
    }

    public static int getBlockFaceSlot(BlockState state, int faceIndex) {
        return state != null ? state.getFaceTextureSlot(faceIndex) : 2;
    }

    public static boolean isSolid(int type) {
        return getType(type).isSolid();
    }

    public static boolean isSolid(BlockState state) {
        return state != null && state.isSolid();
    }

    public static boolean isPlant(int type) {
        return getType(type).isPlant();
    }

    public static boolean isPlant(BlockState state) {
        return state != null && state.isPlant();
    }

    public static boolean isTransparent(int type) {
        return getType(type).isTransparent();
    }

    public static boolean isTransparent(BlockState state) {
        return state != null && state.isTransparent();
    }

    public static boolean canPlantSurviveOn(int blockBelow) {
        return blockBelow == GRASS || blockBelow == DIRT;
    }

    public static boolean canPlantSurviveOn(BlockState stateBelow) {
        return stateBelow != null && (stateBelow.is(Blocks.GRASS) || stateBelow.is(Blocks.DIRT));
    }
}
