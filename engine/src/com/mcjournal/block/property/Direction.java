package com.mcjournal.block.property;

public enum Direction {
    DOWN(0, -1, 0, "down", Axis.Y),
    UP(0, 1, 0, "up", Axis.Y),
    NORTH(0, 0, -1, "north", Axis.Z),
    SOUTH(0, 0, 1, "south", Axis.Z),
    WEST(-1, 0, 0, "west", Axis.X),
    EAST(1, 0, 0, "east", Axis.X);

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final String name;
    private final Axis axis;

    Direction(int offsetX, int offsetY, int offsetZ, String name, Axis axis) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.name = name;
        this.axis = axis;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public String getName() {
        return name;
    }

    public Axis getAxis() {
        return axis;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Direction fromNormal(int nx, int ny, int nz) {
        if (ny > 0) return UP;
        if (ny < 0) return DOWN;
        if (nz > 0) return SOUTH;
        if (nz < 0) return NORTH;
        if (nx > 0) return EAST;
        if (nx < 0) return WEST;
        return UP;
    }
}
