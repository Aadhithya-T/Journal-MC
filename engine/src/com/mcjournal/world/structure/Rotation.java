package com.mcjournal.world.structure;

import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.property.Axis;

/**
 * 90-degree discrete rotations around the vertical Y axis for structure templates.
 */
public enum Rotation {
    NONE(0),
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    COUNTERCLOCKWISE_90(270);

    private final int degrees;

    Rotation(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }

    /**
     * Transforms local X coordinate from original dimensions (origW, origD).
     */
    public int transformX(int x, int z, int origW, int origD) {
        return switch (this) {
            case NONE -> x;
            case CLOCKWISE_90 -> origD - 1 - z;
            case CLOCKWISE_180 -> origW - 1 - x;
            case COUNTERCLOCKWISE_90 -> z;
        };
    }

    /**
     * Transforms local Z coordinate from original dimensions (origW, origD).
     */
    public int transformZ(int x, int z, int origW, int origD) {
        return switch (this) {
            case NONE -> z;
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> origD - 1 - z;
            case COUNTERCLOCKWISE_90 -> origW - 1 - x;
        };
    }

    /**
     * Returns transformed width (dimension along X).
     */
    public int transformedWidth(int origW, int origD) {
        return switch (this) {
            case NONE, CLOCKWISE_180 -> origW;
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> origD;
        };
    }

    /**
     * Returns transformed depth (dimension along Z).
     */
    public int transformedDepth(int origW, int origD) {
        return switch (this) {
            case NONE, CLOCKWISE_180 -> origD;
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> origW;
        };
    }

    /**
     * Rotates directional block state properties (e.g. AXIS property for logs).
     */
    public BlockState rotateState(BlockState state) {
        if (state == null) return null;
        if ((this == CLOCKWISE_90 || this == COUNTERCLOCKWISE_90) && state.contains(BlockProperties.AXIS)) {
            Axis axis = state.get(BlockProperties.AXIS);
            if (axis == Axis.X) {
                return state.with(BlockProperties.AXIS, Axis.Z);
            } else if (axis == Axis.Z) {
                return state.with(BlockProperties.AXIS, Axis.X);
            }
        }
        return state;
    }
}
