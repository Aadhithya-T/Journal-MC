package com.mcjournal.world;

/**
 * Immutable configuration defining finite world spatial dimensions, seed,
 * generation version, boundary type, and initial spawn point.
 */
public record WorldConfiguration(
    int width,
    int depth,
    int height,
    long seed,
    int generationVersion,
    BoundaryType boundaryType,
    int spawnX,
    int spawnY,
    int spawnZ
) {

    public enum BoundaryType {
        NATURAL,
        BARRIER,
        VOID
    }

    public static final int DEFAULT_WIDTH = 2500;
    public static final int DEFAULT_DEPTH = 2500;
    public static final int DEFAULT_HEIGHT = 256;
    public static final int DEFAULT_GENERATION_VERSION = 1;
    public static final int DEFAULT_SPAWN_X = 1250;
    public static final int DEFAULT_SPAWN_Y = 66;
    public static final int DEFAULT_SPAWN_Z = 1250;

    public WorldConfiguration {
        if (width <= 0 || depth <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "World dimensions must be strictly positive: width=" + width + ", depth=" + depth + ", height=" + height
            );
        }
        if (boundaryType == null) {
            boundaryType = BoundaryType.NATURAL;
        }
    }

    public static WorldConfiguration createDefault(long seed) {
        return new WorldConfiguration(
            DEFAULT_WIDTH,
            DEFAULT_DEPTH,
            DEFAULT_HEIGHT,
            seed,
            DEFAULT_GENERATION_VERSION,
            BoundaryType.NATURAL,
            DEFAULT_SPAWN_X,
            DEFAULT_SPAWN_Y,
            DEFAULT_SPAWN_Z
        );
    }

    public static WorldConfiguration of(int width, int depth, int height, long seed) {
        return new WorldConfiguration(
            width,
            depth,
            height,
            seed,
            DEFAULT_GENERATION_VERSION,
            BoundaryType.NATURAL,
            width / 2,
            DEFAULT_SPAWN_Y,
            depth / 2
        );
    }
}
