package com.mcjournal.event;

/**
 * Fired when a player successfully places a block into the world.
 */
public record BlockPlacedEvent(
    String worldId,
    byte blockType,
    int x,
    int y,
    int z
) {}

