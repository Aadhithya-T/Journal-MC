package com.mcjournal.world.structure;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable placement rules and environmental constraints for a structure definition.
 */
public final class PlacementConstraints {

    private final Set<String> allowedBiomes;
    private final Set<String> disallowedBiomes;
    private final int minElevation;
    private final int maxElevation;
    private final int maxSlope;
    private final int minBoundaryDistance;
    private final int minSpacing;
    private final boolean allowWater;
    private final int spawnExclusionRadius;
    private final int priority;

    private PlacementConstraints(Builder builder) {
        this.allowedBiomes = Set.copyOf(builder.allowedBiomes);
        this.disallowedBiomes = Set.copyOf(builder.disallowedBiomes);
        this.minElevation = builder.minElevation;
        this.maxElevation = builder.maxElevation;
        this.maxSlope = builder.maxSlope;
        this.minBoundaryDistance = Math.max(0, builder.minBoundaryDistance);
        this.minSpacing = Math.max(0, builder.minSpacing);
        this.allowWater = builder.allowWater;
        this.spawnExclusionRadius = Math.max(0, builder.spawnExclusionRadius);
        this.priority = builder.priority;
    }

    public Set<String> getAllowedBiomes() { return allowedBiomes; }
    public Set<String> getDisallowedBiomes() { return disallowedBiomes; }
    public int getMinElevation() { return minElevation; }
    public int getMaxElevation() { return maxElevation; }
    public int getMaxSlope() { return maxSlope; }
    public int getMinBoundaryDistance() { return minBoundaryDistance; }
    public int getMinSpacing() { return minSpacing; }
    public boolean isAllowWater() { return allowWater; }
    public int getSpawnExclusionRadius() { return spawnExclusionRadius; }
    public int getPriority() { return priority; }

    public static Builder builder() {
        return new Builder();
    }

    public static PlacementConstraints defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private final Set<String> allowedBiomes = new HashSet<>();
        private final Set<String> disallowedBiomes = new HashSet<>();
        private int minElevation = 50;
        private int maxElevation = 210;
        private int maxSlope = 2;
        private int minBoundaryDistance = 32;
        private int minSpacing = 64;
        private boolean allowWater = false;
        private int spawnExclusionRadius = 64;
        private int priority = 0;

        public Builder allowBiome(String biomeId) {
            if (biomeId != null) this.allowedBiomes.add(biomeId.toLowerCase().trim());
            return this;
        }

        public Builder disallowBiome(String biomeId) {
            if (biomeId != null) this.disallowedBiomes.add(biomeId.toLowerCase().trim());
            return this;
        }

        public Builder elevation(int minElevation, int maxElevation) {
            this.minElevation = minElevation;
            this.maxElevation = maxElevation;
            return this;
        }

        public Builder maxSlope(int slope) {
            this.maxSlope = slope;
            return this;
        }

        public Builder minBoundaryDistance(int dist) {
            this.minBoundaryDistance = dist;
            return this;
        }

        public Builder minSpacing(int spacing) {
            this.minSpacing = spacing;
            return this;
        }

        public Builder allowWater(boolean allow) {
            this.allowWater = allow;
            return this;
        }

        public Builder spawnExclusionRadius(int radius) {
            this.spawnExclusionRadius = radius;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public PlacementConstraints build() {
            return new PlacementConstraints(this);
        }
    }
}

