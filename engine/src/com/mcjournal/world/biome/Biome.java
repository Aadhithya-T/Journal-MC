package com.mcjournal.world.biome;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;

import java.util.Objects;

/**
 * Immutable biome specification defining climate targets, height characteristics,
 * and block palettes. Independent of rendering.
 */
public final class Biome {

    private final String id;
    private final String displayName;
    private final double targetTemperature;
    private final double targetMoisture;
    private final double targetContinentalness;
    private final double baseHeight;
    private final double heightVariation;
    private final double roughness;
    private final BlockState surfaceBlock;
    private final BlockState subsurfaceBlock;
    private final BlockState deepBlock;
    private final BlockState underwaterBlock;
    private final int waterLevel;
    private final double treeDensity;
    private final double foliageDensity;

    private Biome(Builder builder) {
        if (builder.id == null || builder.id.isBlank()) {
            throw new IllegalArgumentException("Biome ID cannot be null or blank");
        }
        if (builder.displayName == null || builder.displayName.isBlank()) {
            throw new IllegalArgumentException("Biome displayName cannot be null or blank");
        }
        if (builder.surfaceBlock == null || builder.subsurfaceBlock == null ||
            builder.deepBlock == null || builder.underwaterBlock == null) {
            throw new IllegalArgumentException("Biome block palette cannot contain null states");
        }
        if (builder.baseHeight < 1.0 || builder.baseHeight > 250.0) {
            throw new IllegalArgumentException("baseHeight must be between 1.0 and 250.0, got: " + builder.baseHeight);
        }
        if (builder.waterLevel < 0 || builder.waterLevel > 255) {
            throw new IllegalArgumentException("waterLevel must be between 0 and 255, got: " + builder.waterLevel);
        }

        this.id = builder.id.toLowerCase().trim();
        this.displayName = builder.displayName;
        this.targetTemperature = Math.clamp(builder.targetTemperature, -1.0, 1.0);
        this.targetMoisture = Math.clamp(builder.targetMoisture, -1.0, 1.0);
        this.targetContinentalness = Math.clamp(builder.targetContinentalness, -1.0, 1.0);
        this.baseHeight = builder.baseHeight;
        this.heightVariation = Math.max(0.0, builder.heightVariation);
        this.roughness = Math.max(0.1, builder.roughness);
        this.surfaceBlock = builder.surfaceBlock;
        this.subsurfaceBlock = builder.subsurfaceBlock;
        this.deepBlock = builder.deepBlock;
        this.underwaterBlock = builder.underwaterBlock;
        this.waterLevel = builder.waterLevel;
        this.treeDensity = Math.clamp(builder.treeDensity, 0.0, 1.0);
        this.foliageDensity = Math.clamp(builder.foliageDensity, 0.0, 1.0);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getTargetTemperature() { return targetTemperature; }
    public double getTargetMoisture() { return targetMoisture; }
    public double getTargetContinentalness() { return targetContinentalness; }
    public double getBaseHeight() { return baseHeight; }
    public double getHeightVariation() { return heightVariation; }
    public double getRoughness() { return roughness; }
    public BlockState getSurfaceBlock() { return surfaceBlock; }
    public BlockState getSubsurfaceBlock() { return subsurfaceBlock; }
    public BlockState getDeepBlock() { return deepBlock; }
    public BlockState getUnderwaterBlock() { return underwaterBlock; }
    public int getWaterLevel() { return waterLevel; }
    public double getTreeDensity() { return treeDensity; }
    public double getFoliageDensity() { return foliageDensity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Biome other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Biome{" + id + " ('" + displayName + "')}";
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public static class Builder {
        private final String id;
        private final String displayName;
        private double targetTemperature = 0.0;
        private double targetMoisture = 0.0;
        private double targetContinentalness = 0.2;
        private double baseHeight = 64.0;
        private double heightVariation = 8.0;
        private double roughness = 1.0;
        private BlockState surfaceBlock = Blocks.GRASS.getDefaultState();
        private BlockState subsurfaceBlock = Blocks.DIRT.getDefaultState();
        private BlockState deepBlock = Blocks.STONE.getDefaultState();
        private BlockState underwaterBlock = Blocks.SAND.getDefaultState();
        private int waterLevel = 62;
        private double treeDensity = 0.15;
        private double foliageDensity = 0.25;

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder climate(double temp, double moist, double cont) {
            this.targetTemperature = temp;
            this.targetMoisture = moist;
            this.targetContinentalness = cont;
            return this;
        }

        public Builder terrain(double baseHeight, double variation, double roughness) {
            this.baseHeight = baseHeight;
            this.heightVariation = variation;
            this.roughness = roughness;
            return this;
        }

        public Builder surfaceBlock(BlockState state) {
            this.surfaceBlock = Objects.requireNonNull(state);
            return this;
        }

        public Builder subsurfaceBlock(BlockState state) {
            this.subsurfaceBlock = Objects.requireNonNull(state);
            return this;
        }

        public Builder deepBlock(BlockState state) {
            this.deepBlock = Objects.requireNonNull(state);
            return this;
        }

        public Builder underwaterBlock(BlockState state) {
            this.underwaterBlock = Objects.requireNonNull(state);
            return this;
        }

        public Builder waterLevel(int waterLevel) {
            this.waterLevel = waterLevel;
            return this;
        }

        public Builder treeDensity(double density) {
            this.treeDensity = density;
            return this;
        }

        public Builder foliageDensity(double density) {
            this.foliageDensity = density;
            return this;
        }

        public Biome build() {
            return new Biome(this);
        }
    }
}
