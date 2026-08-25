package com.mcjournal.block;

import com.mcjournal.block.property.*;

public final class BlockProperties {
    private BlockProperties() {}

    /**
     * Orientation axis for logs, pillars, and directional blocks (X, Y, Z).
     */
    public static final EnumProperty<Axis> AXIS = EnumProperty.of("axis", Axis.class);

    /**
     * 6-Directional facing for pistons, dispensers, droppers, etc.
     */
    public static final EnumProperty<Direction> FACING = EnumProperty.of("facing", Direction.class);

    /**
     * Horizontal 4-direction facing for stairs, chests, furnaces, doors, etc.
     */
    public static final EnumProperty<Direction> HORIZONTAL_FACING = EnumProperty.of("facing", Direction.class,
            java.util.List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));

    /**
     * Fluid level (0 = full source block, 1..7 = flowing distance levels).
     */
    public static final IntegerProperty LEVEL = IntegerProperty.of("level", 0, 7);

    /**
     * Waterlogged state for stairs, slabs, fences, etc. submerged in water.
     */
    public static final BooleanProperty WATERLOGGED = BooleanProperty.of("waterlogged");

    /**
     * Snowy top for grass blocks beneath snow layers.
     */
    public static final BooleanProperty SNOWY = BooleanProperty.of("snowy");
}
