package com.mcjournal.world.time;

import com.mcjournal.event.DayChangedEvent;
import com.mcjournal.event.EventBus;

/**
 * Persistent logical in-game clock tracking continuous solar time and day progression.
 * Fully decoupled from rendering and sky lighting calculations.
 *
 * In-game solar kinematics:
 * - 24,000 ticks per solar day.
 * - 1,000 ticks per in-game hour.
 * - Tick 0 = 06:00 (Dawn / Sunrise)
 * - Tick 6,000 = 12:00 (Midday / High Noon)
 * - Tick 12,000 = 18:00 (Dusk / Sunset)
 * - Tick 18,000 = 00:00 (Midnight)
 */
public class WorldClock {

    public static final double TICKS_PER_DAY = 24000.0;
    public static final double TICKS_PER_HOUR = 1000.0;
    public static final double TICKS_PER_SECOND = 20.0; // 20 TPS

    private String worldId = "world";
    private double totalTicks = 6000.0; // Defaults to Day 1 Midday

    public WorldClock() {
        this(6000.0);
    }

    public WorldClock(double initialTotalTicks) {
        this.totalTicks = Math.max(0.0, initialTotalTicks);
    }

    public void setWorldId(String worldId) {
        if (worldId != null && !worldId.isBlank()) {
            this.worldId = worldId;
        }
    }

    public String getWorldId() {
        return worldId;
    }

    /**
     * Advances the clock by deltaTime seconds.
     * At 20 TPS, 1.0 second corresponds to 20 ticks.
     */
    public void advanceTime(double deltaTime) {
        if (deltaTime <= 0.0) return;
        advanceTicks(deltaTime * TICKS_PER_SECOND);
    }

    /**
     * Advances the clock by an explicit number of simulation ticks.
     * Fires a DayChangedEvent when crossing a day boundary.
     */
    public void advanceTicks(double ticks) {
        if (ticks <= 0.0) return;

        long prevDay = getDay();
        totalTicks += ticks;
        long newDay = getDay();

        if (newDay > prevDay) {
            EventBus.getInstance().post(new DayChangedEvent(worldId, prevDay, newDay));
        }
    }

    /**
     * Returns the 1-based current world day (Day 1 starts at 0 ticks).
     */
    public long getDay() {
        return (long) Math.floor(totalTicks / TICKS_PER_DAY) + 1;
    }

    /**
     * Returns the 0-based day count (0 for the first 24,000 ticks).
     */
    public long getZeroBasedDay() {
        return (long) Math.floor(totalTicks / TICKS_PER_DAY);
    }

    /**
     * Returns the current time of day in ticks [0.0 .. 24000.0).
     */
    public double getTimeOfDayTicks() {
        double tod = totalTicks % TICKS_PER_DAY;
        if (tod < 0.0) tod += TICKS_PER_DAY;
        return tod;
    }

    /**
     * Returns the current in-game 24-hour clock hour (0..23).
     * Tick 0 is 06:00 (dawn), Tick 6000 is 12:00 (noon).
     */
    public int getHour() {
        double solarOffset = (getTimeOfDayTicks() + 6000.0) % TICKS_PER_DAY;
        return (int) (solarOffset / TICKS_PER_HOUR);
    }

    /**
     * Returns the current in-game minute (0..59).
     */
    public int getMinute() {
        double solarOffset = (getTimeOfDayTicks() + 6000.0) % TICKS_PER_DAY;
        double hourFraction = (solarOffset % TICKS_PER_HOUR) / TICKS_PER_HOUR;
        return (int) Math.floor(hourFraction * 60.0);
    }

    /**
     * Formats the current in-game timestamp as: "Day X — HH:MM".
     * Example: "Day 12 — 08:42" or "Day 1 — 12:00".
     */
    public String getFormattedTime() {
        return String.format("Day %d — %02d:%02d", getDay(), getHour(), getMinute());
    }

    /**
     * Whether the solar cycle is currently in daytime [0..12000 ticks).
     */
    public boolean isDay() {
        double tod = getTimeOfDayTicks();
        return tod >= 0.0 && tod < 12000.0;
    }

    /**
     * Returns total monotonically accumulated simulation ticks since world genesis.
     */
    public double getTotalTicks() {
        return totalTicks;
    }

    /**
     * Explicitly sets total simulation ticks (e.g. when loading save data).
     */
    public void setTotalTicks(double ticks) {
        this.totalTicks = Math.max(0.0, ticks);
    }
}

