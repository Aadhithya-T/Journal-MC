package com.mcjournal.world.biome;

/**
 * Encapsulates continuous climate signals used for biome selection and blending.
 * All parameters are normalized to [-1.0, 1.0].
 */
public record ClimateParameters(double temperature, double moisture, double continentalness) {

    public ClimateParameters {
        temperature = Math.clamp(temperature, -1.0, 1.0);
        moisture = Math.clamp(moisture, -1.0, 1.0);
        continentalness = Math.clamp(continentalness, -1.0, 1.0);
    }

    /**
     * Calculates the squared weighted Euclidean distance between this climate and target climate preferences.
     */
    public double distanceSquared(double targetTemp, double targetMoist, double targetCont) {
        double dt = this.temperature - targetTemp;
        double dm = this.moisture - targetMoist;
        double dc = this.continentalness - targetCont;
        // Weight continentalness slightly higher as it defines major landmasses
        return dt * dt * 1.0 + dm * dm * 1.0 + dc * dc * 1.5;
    }
}
