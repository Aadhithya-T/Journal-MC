package com.mcjournal.event;

/**
 * Fired when a landmark is discovered by the player.
 */
public record LandmarkDiscoveredEvent(
    String worldId,
    String landmarkId,
    String landmarkName,
    String landmarkType,
    double x,
    double y,
    double z
) {}

