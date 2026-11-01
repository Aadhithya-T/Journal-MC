package com.mcjournal.world.biome;

import com.mcjournal.SimplexNoise;

/**
 * Thread-safe, deterministic climate sampler.
 * Computes temperature, moisture, and continentalness at world coordinates (wx, wz)
 * using octave-based Simplex noise derived from the world seed.
 */
public class ClimateSampler {

    private final SimplexNoise tempBaseNoise;
    private final SimplexNoise tempDetailNoise;
    private final SimplexNoise moistBaseNoise;
    private final SimplexNoise moistDetailNoise;
    private final SimplexNoise contBaseNoise;
    private final SimplexNoise contDetailNoise;

    public ClimateSampler(long seed) {
        this.tempBaseNoise = new SimplexNoise(seed + 101L);
        this.tempDetailNoise = new SimplexNoise(seed + 102L);
        this.moistBaseNoise = new SimplexNoise(seed + 201L);
        this.moistDetailNoise = new SimplexNoise(seed + 202L);
        this.contBaseNoise = new SimplexNoise(seed + 301L);
        this.contDetailNoise = new SimplexNoise(seed + 302L);
    }

    /**
     * Samples climate parameters at world coordinate (wx, wz).
     * Output values are strictly within [-1.0, 1.0].
     */
    public ClimateParameters sample(int wx, int wz) {
        // Temperature: large climate zones (~650 blocks) + detail variation
        double tempBase = tempBaseNoise.noise2D(wx * 0.0015, wz * 0.0015);
        double tempDetail = tempDetailNoise.noise2D(wx * 0.0050, wz * 0.0050);
        double temp = tempBase * 0.80 + tempDetail * 0.20;

        // Moisture: humidity distribution (~550 blocks) + detail variation
        double moistBase = moistBaseNoise.noise2D(wx * 0.0018, wz * 0.0018);
        double moistDetail = moistDetailNoise.noise2D(wx * 0.0060, wz * 0.0060);
        double moist = moistBase * 0.80 + moistDetail * 0.20;

        // Continentalness: ocean vs inland landmass (~800 blocks) + coastal detail
        double contBase = contBaseNoise.noise2D(wx * 0.0012, wz * 0.0012);
        double contDetail = contDetailNoise.noise2D(wx * 0.0040, wz * 0.0040);
        double cont = contBase * 0.75 + contDetail * 0.25;

        return new ClimateParameters(temp, moist, cont);
    }
}
