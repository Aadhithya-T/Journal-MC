package com.mcjournal.event;

/**
 * Fired when the player crosses a voxel block coordinate boundary in the world.
 * Avoids per-frame micro-float allocations by only publishing on discrete spatial transitions.
 */
public record PlayerMovedEvent(
    String worldId,
    double prevX,
    double prevY,
    double prevZ,
    double newX,
    double newY,
    double newZ
) {
    public int getBlockX() { return (int) Math.floor(newX); }
    public int getBlockY() { return (int) Math.floor(newY); }
    public int getBlockZ() { return (int) Math.floor(newZ); }
}

