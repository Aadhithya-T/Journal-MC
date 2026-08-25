package com.mcjournal.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Blocks {
    private static final List<BlockType> ALL_BLOCKS = new ArrayList<>();

    private static BlockType register(BlockType block) {
        ALL_BLOCKS.add(block);
        return block;
    }

    public static final BlockType AIR = register(new BlockType.Builder("air", (byte) 0)
            .name("Air").color("#000000").solid(false).transparent(true).hardness(-1.0f).build());

    public static final BlockType GRASS = register(new BlockType.Builder("grass_block", (byte) 1)
            .name("Grass Block").color("#5fa832").hardness(0.6f).properties(BlockProperties.SNOWY).build());

    public static final BlockType DIRT = register(new BlockType.Builder("dirt", (byte) 2)
            .name("Dirt").color("#866043").hardness(0.5f).build());

    public static final BlockType STONE = register(new BlockType.Builder("stone", (byte) 3)
            .name("Stone").color("#787878").hardness(1.5f).build());

    public static final BlockType COBBLESTONE = register(new BlockType.Builder("cobblestone", (byte) 4)
            .name("Cobblestone").color("#555555").hardness(2.0f).build());

    public static final BlockType SAND = register(new BlockType.Builder("sand", (byte) 5)
            .name("Sand").color("#dbd3a0").hardness(0.5f).build());

    public static final BlockType BEDROCK = register(new BlockType.Builder("bedrock", (byte) 6)
            .name("Bedrock").color("#222222").hardness(-1.0f).build());

    public static final BlockType OAK_LOG = register(new BlockType.Builder("oak_log", (byte) 7)
            .name("Oak Log").color("#674a27").hardness(2.0f).properties(BlockProperties.AXIS).build());

    public static final BlockType OAK_LEAVES = register(new BlockType.Builder("oak_leaves", (byte) 8)
            .name("Oak Leaves").color("#4ca028").hardness(0.2f).transparent(true).build());

    public static final BlockType DIAMOND_ORE = register(new BlockType.Builder("diamond_ore", (byte) 9)
            .name("Diamond Ore").color("#55ffff").hardness(3.0f).build());

    public static final BlockType WATER = register(new BlockType.Builder("water", (byte) 10)
            .name("Water").color("#2762d6").fluid(true).properties(BlockProperties.LEVEL).build());

    public static final BlockType BIRCH_LOG = register(new BlockType.Builder("birch_log", (byte) 11)
            .name("Birch Log").color("#eaeaea").hardness(2.0f).properties(BlockProperties.AXIS).build());

    public static final BlockType BIRCH_LEAVES = register(new BlockType.Builder("birch_leaves", (byte) 12)
            .name("Birch Leaves").color("#5db532").hardness(0.2f).transparent(true).build());

    public static final BlockType TALL_GRASS = register(new BlockType.Builder("tall_grass", (byte) 13)
            .name("Tall Grass").color("#5fa832").plant(true).build());

    public static final BlockType POPPY = register(new BlockType.Builder("poppy", (byte) 14)
            .name("Poppy").color("#dd2222").plant(true).build());

    public static final BlockType DANDELION = register(new BlockType.Builder("dandelion", (byte) 15)
            .name("Dandelion").color("#ffdd00").plant(true).build());

    private Blocks() {}

    public static List<BlockType> getAll() {
        return Collections.unmodifiableList(ALL_BLOCKS);
    }
}
