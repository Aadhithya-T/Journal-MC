package com.mcjournal;

public final class Item {
    public static final byte IRON_AXE = 100;
    public static final byte IRON_SHOVEL = 101;
    public static final byte IRON_PICKAXE = 102;

    private Item() {}

    public static boolean isTool(byte id) {
        return id == IRON_AXE || id == IRON_SHOVEL || id == IRON_PICKAXE;
    }

    public static boolean isAxe(byte id) {
        return id == IRON_AXE;
    }

    public static boolean isShovel(byte id) {
        return id == IRON_SHOVEL;
    }

    public static boolean isPickaxe(byte id) {
        return id == IRON_PICKAXE;
    }

    /**
     * Checks if the given block is effectively mined by an axe (logs & leaves).
     */
    public static boolean isAxeEffective(byte blockType) {
        return blockType == Block.OAK_LOG
            || blockType == Block.BIRCH_LOG
            || blockType == Block.OAK_LEAVES
            || blockType == Block.BIRCH_LEAVES;
    }

    /**
     * Checks if the given block is effectively dug by a shovel (grass, dirt & sand).
     */
    public static boolean isShovelEffective(byte blockType) {
        return blockType == Block.GRASS
            || blockType == Block.DIRT
            || blockType == Block.SAND;
    }

    /**
     * Checks if the given block is effectively mined by a pickaxe (stone, cobblestone & diamond ore).
     */
    public static boolean isPickaxeEffective(byte blockType) {
        return blockType == Block.STONE
            || blockType == Block.COBBLESTONE
            || blockType == Block.DIAMOND_ORE;
    }

    /**
     * Retrieves the tool speed multiplier.
     * Diamond Axe/Shovel/Pickaxe provides an 8.0x speed multiplier against effective blocks.
     */
    public static float getMiningSpeedMultiplier(byte toolId, byte blockType) {
        if (isAxe(toolId) && isAxeEffective(blockType)) {
            return 8.0f; // Diamond Axe tier multiplier
        }
        if (isShovel(toolId) && isShovelEffective(blockType)) {
            return 8.0f; // Diamond Shovel tier multiplier (0.10s dirt, 0.15s grass)
        }
        if (isPickaxe(toolId) && isPickaxeEffective(blockType)) {
            return 8.0f; // Diamond Pickaxe tier multiplier (0.30s stone, 0.40s cobble, 0.60s diamond ore)
        }
        return 1.0f;
    }

    /**
     * Determines if the player can harvest the block with the currently held tool.
     * Blocks requiring a pickaxe (Stone, Cobblestone, Diamond Ore) require an Iron Pickaxe.
     */
    public static boolean canHarvest(byte toolId, byte blockType) {
        if (blockType == Block.STONE || blockType == Block.COBBLESTONE || blockType == Block.DIAMOND_ORE) {
            return isPickaxe(toolId);
        }
        return true;
    }

    public static String getName(byte id) {
        if (id == IRON_AXE) return "Iron Axe";
        if (id == IRON_SHOVEL) return "Iron Shovel";
        if (id == IRON_PICKAXE) return "Iron Pickaxe";
        return Block.getName(id);
    }

    public static int getItemTile(byte id) {
        if (id == IRON_AXE) return 16;
        if (id == IRON_SHOVEL) return 17;
        if (id == IRON_PICKAXE) return 18;
        return 2;
    }
}
