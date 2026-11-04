package com.mcjournal.event;

/**
 * Fired when a world session is successfully initialized and entered.
 */
public record WorldLoadedEvent(
    String worldId,
    String worldName,
    long seed,
    double worldTime
) {}

