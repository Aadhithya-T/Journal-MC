package com.mcjournal.event;

/**
 * Fired when the player traverses a biome boundary into a new biome region.
 */
public record BiomeEnteredEvent(
    String worldId,
    String previousBiomeId,
    String newBiomeId,
    String newBiomeName
) {}

