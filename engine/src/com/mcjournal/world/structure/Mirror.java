package com.mcjournal.world.structure;

/**
 * Horizontal reflection / mirroring options for structure templates.
 */
public enum Mirror {
    NONE,
    LEFT_RIGHT,  // Mirror along X axis (reverses X)
    FRONT_BACK;  // Mirror along Z axis (reverses Z)

    public int transformX(int x, int width) {
        return switch (this) {
            case NONE, FRONT_BACK -> x;
            case LEFT_RIGHT -> width - 1 - x;
        };
    }

    public int transformZ(int z, int depth) {
        return switch (this) {
            case NONE, LEFT_RIGHT -> z;
            case FRONT_BACK -> depth - 1 - z;
        };
    }
}

