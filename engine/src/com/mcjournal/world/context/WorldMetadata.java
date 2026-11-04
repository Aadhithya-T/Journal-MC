package com.mcjournal.world.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcjournal.world.WorldConfiguration;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Authoritative, persistent world identity and metadata for Journal-MC.
 * Maintains a stable, immutable UUID (worldId) that remains constant across
 * world renames and storage migrations.
 */
public class WorldMetadata {

    public static final int CURRENT_METADATA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Stable World Identity
    private String worldId;
    private String worldName;
    private String createdAt;
    private String lastPlayedAt;
    private long seed;

    // World Dimensions (Finite 2500x2500x256)
    private int worldWidth = WorldConfiguration.DEFAULT_WIDTH;
    private int worldDepth = WorldConfiguration.DEFAULT_DEPTH;
    private int worldHeight = WorldConfiguration.DEFAULT_HEIGHT;

    // Generation Configuration
    private int generationVersion = WorldConfiguration.DEFAULT_GENERATION_VERSION;
    private String boundaryType = WorldConfiguration.BoundaryType.NATURAL.name();
    private int spawnX = WorldConfiguration.DEFAULT_SPAWN_X;
    private int spawnY = WorldConfiguration.DEFAULT_SPAWN_Y;
    private int spawnZ = WorldConfiguration.DEFAULT_SPAWN_Z;

    // In-game Temporal State
    private long currentGameDay = 1;
    private double currentGameTime = 6000.0; // Day 1 Midday baseline

    // Schema version
    private int metadataVersion = CURRENT_METADATA_VERSION;

    public WorldMetadata() {
        this.worldId = UUID.randomUUID().toString();
        this.worldName = "Journal World";
        this.createdAt = Instant.now().toString();
        this.lastPlayedAt = this.createdAt;
    }

    public static WorldMetadata createNew(String worldName, long seed, WorldConfiguration config) {
        WorldMetadata meta = new WorldMetadata();
        meta.worldId = UUID.randomUUID().toString();
        meta.worldName = (worldName != null && !worldName.isBlank()) ? worldName.trim() : "Journal World";
        meta.seed = seed;
        meta.createdAt = Instant.now().toString();
        meta.lastPlayedAt = meta.createdAt;

        if (config != null) {
            meta.worldWidth = config.width();
            meta.worldDepth = config.depth();
            meta.worldHeight = config.height();
            meta.generationVersion = config.generationVersion();
            meta.boundaryType = (config.boundaryType() != null) ? config.boundaryType().name() : "NATURAL";
            meta.spawnX = config.spawnX();
            meta.spawnY = config.spawnY();
            meta.spawnZ = config.spawnZ();
        }

        return meta;
    }

    /**
     * Creates a stable metadata instance for migrating legacy saves, deterministically
     * deriving worldId from seed and name if not previously assigned.
     */
    public static WorldMetadata fromLegacySave(String worldName, long seed, WorldConfiguration config, double worldTime) {
        WorldMetadata meta = new WorldMetadata();
        // Deterministic UUID for existing legacy world so it remains stable across launches
        meta.worldId = UUID.nameUUIDFromBytes((worldName + ":" + seed).getBytes()).toString();
        meta.worldName = (worldName != null && !worldName.isBlank()) ? worldName.trim() : "Journal World";
        meta.seed = seed;
        meta.currentGameTime = (worldTime >= 0.0) ? worldTime : 6000.0;
        meta.currentGameDay = (long) Math.floor(meta.currentGameTime / 24000.0) + 1;

        if (config != null) {
            meta.worldWidth = config.width();
            meta.worldDepth = config.depth();
            meta.worldHeight = config.height();
            meta.generationVersion = config.generationVersion();
            meta.boundaryType = config.boundaryType() != null ? config.boundaryType().name() : "NATURAL";
            meta.spawnX = config.spawnX();
            meta.spawnY = config.spawnY();
            meta.spawnZ = config.spawnZ();
        }

        return meta;
    }

    // Getters
    public String getWorldId() { return worldId != null ? worldId : "default-world-id"; }
    public String getWorldName() { return worldName != null ? worldName : "Journal World"; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getLastPlayedAt() { return lastPlayedAt != null ? lastPlayedAt : ""; }
    public long getSeed() { return seed; }
    public int getWorldWidth() { return worldWidth > 0 ? worldWidth : WorldConfiguration.DEFAULT_WIDTH; }
    public int getWorldDepth() { return worldDepth > 0 ? worldDepth : WorldConfiguration.DEFAULT_DEPTH; }
    public int getWorldHeight() { return worldHeight > 0 ? worldHeight : WorldConfiguration.DEFAULT_HEIGHT; }
    public int getGenerationVersion() { return generationVersion > 0 ? generationVersion : 1; }
    public String getBoundaryType() { return boundaryType != null ? boundaryType : "NATURAL"; }
    public int getSpawnX() { return spawnX; }
    public int getSpawnY() { return spawnY; }
    public int getSpawnZ() { return spawnZ; }
    public long getCurrentGameDay() { return currentGameDay > 0 ? currentGameDay : 1; }
    public double getCurrentGameTime() { return currentGameTime >= 0 ? currentGameTime : 6000.0; }
    public int getMetadataVersion() { return metadataVersion; }

    // Mutators (Guaranteed identity stability)
    public void setWorldName(String newName) {
        if (newName != null && !newName.isBlank()) {
            this.worldName = newName.trim();
        }
        updateLastPlayed();
    }

    public void updateLastPlayed() {
        this.lastPlayedAt = Instant.now().toString();
    }

    public void updateTime(long day, double timeTicks) {
        this.currentGameDay = Math.max(1, day);
        this.currentGameTime = Math.max(0.0, timeTicks);
        updateLastPlayed();
    }

    public WorldConfiguration toWorldConfiguration() {
        WorldConfiguration.BoundaryType bType = WorldConfiguration.BoundaryType.NATURAL;
        try {
            if (boundaryType != null) {
                bType = WorldConfiguration.BoundaryType.valueOf(boundaryType.toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {}

        return new WorldConfiguration(
            getWorldWidth(),
            getWorldDepth(),
            getWorldHeight(),
            seed,
            getGenerationVersion(),
            bType,
            spawnX,
            spawnY,
            spawnZ
        );
    }

    // JSON Serialization
    public String toJson() {
        return GSON.toJson(this);
    }

    public static WorldMetadata fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new WorldMetadata();
        }
        try {
            WorldMetadata meta = GSON.fromJson(json, WorldMetadata.class);
            if (meta == null) return new WorldMetadata();
            // Validate and sanitize essential fields
            if (meta.worldId == null || meta.worldId.isBlank()) {
                meta.worldId = UUID.randomUUID().toString();
            }
            if (meta.worldName == null || meta.worldName.isBlank()) {
                meta.worldName = "Journal World";
            }
            if (meta.createdAt == null || meta.createdAt.isBlank()) {
                meta.createdAt = Instant.now().toString();
            }
            return meta;
        } catch (Exception e) {
            System.err.println("[WorldMetadata] Failed to parse metadata JSON: " + e.getMessage() + ". Falling back to safe defaults.");
            return new WorldMetadata();
        }
    }
}

