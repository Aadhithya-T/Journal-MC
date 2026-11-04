package com.mcjournal.event;

/**
 * Fired when a world session is closed, paused, or unloaded.
 */
public record WorldUnloadedEvent(
    String worldId
) {}

