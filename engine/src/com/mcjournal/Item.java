package com.mcjournal;

import com.mcjournal.block.BlockStateRegistry;

/**
 * Item definitions and utility methods.
 * Tool gameplay systems, mining multipliers, and harvestability calculations have been removed.
 * Legacy tool IDs are retained strictly for safe save migration and backward-compatible load sanitization.
 */
public final class Item {
    // Obsolete tool IDs preserved only for legacy save file compatibility sanitization
    public static final byte LEGACY_IRON_AXE = 100;
    public static final byte LEGACY_IRON_SHOVEL = 101;
    public static final byte LEGACY_IRON_PICKAXE = 102;

    private Item() {}

    /**
     * Checks if the given byte ID corresponds to an obsolete legacy tool.
     * Used exclusively to safely sanitize legacy save files upon loading.
     */
    public static boolean isLegacyTool(byte id) {
        return id == LEGACY_IRON_AXE || id == LEGACY_IRON_SHOVEL || id == LEGACY_IRON_PICKAXE;
    }

    /**
     * Backward-compatible alias for legacy tool detection.
     */
    public static boolean isTool(byte id) {
        return isLegacyTool(id);
    }

    public static String getName(byte id) {
        if (id == LEGACY_IRON_AXE) return "Legacy Axe";
        if (id == LEGACY_IRON_SHOVEL) return "Legacy Shovel";
        if (id == LEGACY_IRON_PICKAXE) return "Legacy Pickaxe";
        return BlockStateRegistry.getBlockType(id).getName();
    }
}
