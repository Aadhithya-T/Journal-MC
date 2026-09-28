package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;

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
        return isAxeEffective(BlockStateRegistry.getBlockType(blockType));
    }

    public static boolean isAxeEffective(BlockType block) {
        return block == Blocks.OAK_LOG
            || block == Blocks.BIRCH_LOG
            || block == Blocks.OAK_LEAVES
            || block == Blocks.BIRCH_LEAVES;
    }

    public static boolean isAxeEffective(BlockState state) {
        return state != null && isAxeEffective(state.getBlock());
    }

    /**
     * Checks if the given block is effectively dug by a shovel (grass, dirt & sand).
     */
    public static boolean isShovelEffective(byte blockType) {
        return isShovelEffective(BlockStateRegistry.getBlockType(blockType));
    }

    public static boolean isShovelEffective(BlockType block) {
        return block == Blocks.GRASS
            || block == Blocks.DIRT
            || block == Blocks.SAND;
    }

    public static boolean isShovelEffective(BlockState state) {
        return state != null && isShovelEffective(state.getBlock());
    }

    /**
     * Checks if the given block is effectively mined by a pickaxe (stone, cobblestone & diamond ore).
     */
    public static boolean isPickaxeEffective(byte blockType) {
        return isPickaxeEffective(BlockStateRegistry.getBlockType(blockType));
    }

    public static boolean isPickaxeEffective(BlockType block) {
        return block == Blocks.STONE
            || block == Blocks.COBBLESTONE
            || block == Blocks.DIAMOND_ORE;
    }

    public static boolean isPickaxeEffective(BlockState state) {
        return state != null && isPickaxeEffective(state.getBlock());
    }

    /**
     * Retrieves the tool speed multiplier.
     * Diamond Axe/Shovel/Pickaxe provides an 8.0x speed multiplier against effective blocks.
     */
    public static float getMiningSpeedMultiplier(byte toolId, byte blockType) {
        return getMiningSpeedMultiplier(toolId, BlockStateRegistry.getBlockType(blockType));
    }

    public static float getMiningSpeedMultiplier(byte toolId, BlockType block) {
        if (isAxe(toolId) && isAxeEffective(block)) {
            return 8.0f; // Diamond Axe tier multiplier
        }
        if (isShovel(toolId) && isShovelEffective(block)) {
            return 8.0f; // Diamond Shovel tier multiplier (0.10s dirt, 0.15s grass)
        }
        if (isPickaxe(toolId) && isPickaxeEffective(block)) {
            return 8.0f; // Diamond Pickaxe tier multiplier (0.30s stone, 0.40s cobble, 0.60s diamond ore)
        }
        return 1.0f;
    }

    public static float getMiningSpeedMultiplier(byte toolId, BlockState state) {
        return state != null ? getMiningSpeedMultiplier(toolId, state.getBlock()) : 1.0f;
    }

    /**
     * Determines if the player can harvest the block with the currently held tool.
     * Blocks requiring a pickaxe (Stone, Cobblestone, Diamond Ore) require an Iron Pickaxe.
     */
    public static boolean canHarvest(byte toolId, byte blockType) {
        return canHarvest(toolId, BlockStateRegistry.getBlockType(blockType));
    }

    public static boolean canHarvest(byte toolId, BlockType block) {
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.DIAMOND_ORE) {
            return isPickaxe(toolId);
        }
        return true;
    }

    public static boolean canHarvest(byte toolId, BlockState state) {
        return state != null && canHarvest(toolId, state.getBlock());
    }

    public static String getName(byte id) {
        if (id == IRON_AXE) return "Iron Axe";
        if (id == IRON_SHOVEL) return "Iron Shovel";
        if (id == IRON_PICKAXE) return "Iron Pickaxe";
        return BlockStateRegistry.getBlockType(id).getName();
    }

    public static int getItemTile(byte id) {
        if (id == IRON_AXE) return 16;
        if (id == IRON_SHOVEL) return 17;
        if (id == IRON_PICKAXE) return 18;
        return 2;
    }
}
