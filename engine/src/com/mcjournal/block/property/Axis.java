package com.mcjournal.block.property;

public enum Axis {
    X("x"),
    Y("y"),
    Z("z");

    private final String name;

    Axis(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Axis fromString(String name) {
        for (Axis axis : values()) {
            if (axis.name.equalsIgnoreCase(name)) {
                return axis;
            }
        }
        return Y;
    }

    public static Axis fromNormal(int nx, int ny, int nz) {
        if (nx != 0) return X;
        if (nz != 0) return Z;
        return Y;
    }
}
