package com.mcjournal.world.context;

import com.mcjournal.world.landmark.WorldLandmark;

import java.util.Optional;

/**
 * Reusable, engine-level contextual snapshot answering:
 * "Where is the player right now, and what is the surrounding world context?"
 *
 * Provides safe fallbacks when the world is loading, unloading, or paused.
 */
public record WorldContext(
    String worldId,
    String worldName,
    WorldLocation playerLocation,
    long currentGameDay,
    double currentGameTime,
    String formattedTime,
    String currentBiome,
    WorldLandmark nearbyLandmark,
    boolean available
) {
    public static final WorldContext UNAVAILABLE = new WorldContext(
        "none",
        "No World Active",
        WorldLocation.empty(),
        1,
        6000.0,
        "Day 1 — 12:00",
        "Plains",
        null,
        false
    );

    public Optional<WorldLandmark> getNearbyLandmark() {
        return Optional.ofNullable(nearbyLandmark);
    }
}

