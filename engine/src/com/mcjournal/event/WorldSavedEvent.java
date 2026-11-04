package com.mcjournal.event;

/**
 * Fired when a world state snapshot is successfully saved to disk.
 */
public record WorldSavedEvent(
    String worldId,
    String worldName,
    String savePath
) {}

