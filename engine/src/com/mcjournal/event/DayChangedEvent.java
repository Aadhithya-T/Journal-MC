package com.mcjournal.event;

/**
 * Fired when the logical world clock transitions across a solar day boundary.
 */
public record DayChangedEvent(
    String worldId,
    long previousDay,
    long newDay
) {}

