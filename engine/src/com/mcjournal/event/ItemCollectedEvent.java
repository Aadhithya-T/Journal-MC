package com.mcjournal.event;

/**
 * Fired when a player collects a floating item entity into their inventory.
 */
public record ItemCollectedEvent(
    String worldId,
    byte itemType,
    int count
) {}

