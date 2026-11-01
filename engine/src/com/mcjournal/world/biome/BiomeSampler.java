package com.mcjournal.world.biome;

import java.util.Collection;

/**
 * Samples biomes and computes smooth continuous parameter blending across climate boundaries.
 * Deterministic and independent of chunk loading order.
 */
public class BiomeSampler {

    private final BiomeRegistry registry;
    private final ClimateSampler climateSampler;

    public BiomeSampler(BiomeRegistry registry, ClimateSampler climateSampler) {
        this.registry = (registry != null) ? registry : BiomeRegistry.createDefaultRegistry();
        this.climateSampler = climateSampler;
    }

    public BiomeRegistry getRegistry() {
        return registry;
    }

    public ClimateSampler getClimateSampler() {
        return climateSampler;
    }

    /**
     * Determines the primary biome at world coordinates (wx, wz).
     */
    public Biome getBiome(int wx, int wz) {
        ClimateParameters climate = climateSampler.sample(wx, wz);
        Collection<Biome> biomes = registry.getAll();
        Biome best = registry.getDefaultBiome();
        double minDistance = Double.MAX_VALUE;

        for (Biome biome : biomes) {
            double distSq = climate.distanceSquared(
                    biome.getTargetTemperature(),
                    biome.getTargetMoisture(),
                    biome.getTargetContinentalness()
            );
            if (distSq < minDistance) {
                minDistance = distSq;
                best = biome;
            }
        }
        return best;
    }

    /**
     * Blended terrain height parameters for coordinate (wx, wz).
     */
    public record BlendedTerrainParams(double baseHeight, double heightVariation, double roughness, Biome primaryBiome) {}

    /**
     * Computes smoothly blended terrain parameters across biome transitions using
     * inverse-distance weighting in climate space.
     */
    public BlendedTerrainParams sampleBlendedParams(int wx, int wz) {
        ClimateParameters climate = climateSampler.sample(wx, wz);
        Collection<Biome> biomes = registry.getAll();

        double totalWeight = 0.0;
        double blendedBase = 0.0;
        double blendedVar = 0.0;
        double blendedRough = 0.0;

        Biome primaryBiome = registry.getDefaultBiome();
        double minDistance = Double.MAX_VALUE;

        final double EPSILON = 0.001;

        for (Biome biome : biomes) {
            double distSq = climate.distanceSquared(
                    biome.getTargetTemperature(),
                    biome.getTargetMoisture(),
                    biome.getTargetContinentalness()
            );

            if (distSq < minDistance) {
                minDistance = distSq;
                primaryBiome = biome;
            }

            // Inverse-distance squared weighting for sharp yet smooth transitions
            double weight = 1.0 / Math.pow(distSq + EPSILON, 2.0);
            totalWeight += weight;
            blendedBase += biome.getBaseHeight() * weight;
            blendedVar += biome.getHeightVariation() * weight;
            blendedRough += biome.getRoughness() * weight;
        }

        if (totalWeight > 0.0) {
            blendedBase /= totalWeight;
            blendedVar /= totalWeight;
            blendedRough /= totalWeight;
        } else {
            blendedBase = primaryBiome.getBaseHeight();
            blendedVar = primaryBiome.getHeightVariation();
            blendedRough = primaryBiome.getRoughness();
        }

        return new BlendedTerrainParams(blendedBase, blendedVar, blendedRough, primaryBiome);
    }
}
