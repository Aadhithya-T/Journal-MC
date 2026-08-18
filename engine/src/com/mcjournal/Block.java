package com.mcjournal;

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

    public static String getName(byte type) {
        return switch (type) {
            case GRASS -> "Grass Block";
            case DIRT -> "Dirt";
            case STONE -> "Stone";
            case COBBLESTONE -> "Cobblestone";
            case SAND -> "Sand";
            case BEDROCK -> "Bedrock";
            case OAK_LOG -> "Oak Log";
            case OAK_LEAVES -> "Oak Leaves";
            case DIAMOND_ORE -> "Diamond Ore";
            case WATER -> "Water";
            case BIRCH_LOG -> "Birch Log";
            case BIRCH_LEAVES -> "Birch Leaves";
            case TALL_GRASS -> "Tall Grass";
            case POPPY -> "Poppy";
            case DANDELION -> "Dandelion";
            default -> "Air";
        };
    }

    public static String getColor(byte type) {
        return switch (type) {
            case GRASS -> "#5fa832";
            case DIRT -> "#866043";
            case STONE -> "#787878";
            case COBBLESTONE -> "#555555";
            case SAND -> "#dbd3a0";
            case BEDROCK -> "#222222";
            case OAK_LOG -> "#674a27";
            case OAK_LEAVES -> "#4ca028";
            case DIAMOND_ORE -> "#55ffff";
            case WATER -> "#2762d6";
            case BIRCH_LOG -> "#eaeaea";
            case BIRCH_LEAVES -> "#5db532";
            case TALL_GRASS -> "#5fa832";
            case POPPY -> "#dd2222";
            case DANDELION -> "#ffdd00";
            default -> "#888888";
        };
    }

    public static boolean isSolid(byte type) {
        return type != AIR && type != WATER && type != TALL_GRASS && type != POPPY && type != DANDELION;
    }
}
