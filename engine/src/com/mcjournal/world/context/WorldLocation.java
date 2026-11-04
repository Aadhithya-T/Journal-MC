package com.mcjournal.world.context;

/**
 * Immutable representation of a 3D location within a Journal-MC world.
 * Automatically computes discrete chunk coordinates with full negative-coordinate support.
 */
public record WorldLocation(
    String worldId,
    double x,
    double y,
    double z,
    int chunkX,
    int chunkZ,
    String biomeId,
    String biomeName
) {
    public static WorldLocation of(String worldId, double x, double y, double z, String biomeId, String biomeName) {
        int cx = Math.floorDiv((int) Math.floor(x), 16);
        int cz = Math.floorDiv((int) Math.floor(z), 16);
        return new WorldLocation(
            (worldId != null) ? worldId : "unknown",
            x,
            y,
            z,
            cx,
            cz,
            (biomeId != null) ? biomeId : "plains",
            (biomeName != null) ? biomeName : "Plains"
        );
    }

    public static WorldLocation empty() {
        return new WorldLocation("none", 0.0, 0.0, 0.0, 0, 0, "plains", "Plains");
    }

    public int getBlockX() { return (int) Math.floor(x); }
    public int getBlockY() { return (int) Math.floor(y); }
    public int getBlockZ() { return (int) Math.floor(z); }
}

