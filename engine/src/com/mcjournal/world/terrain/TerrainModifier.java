package com.mcjournal.world.terrain;

import com.mcjournal.world.biome.Biome;

/**
 * Deterministic modifier that can adjust procedural terrain height or biome assignment
 * for authored regions, valleys, special landmarks, or future POI terrain preparation.
 */
public interface TerrainModifier {

    /**
     * Unique identifier/name of this modifier for diagnostics and serialization.
     */
    String getName();

    /**
     * Modifies the calculated height at world coordinate (wx, wz).
     * @param wx World block X
     * @param wz World block Z
     * @param currentHeight The procedural height calculated prior to this stage
     * @return The modified height
     */
    double modifyHeight(int wx, int wz, double currentHeight);

    /**
     * Optionally overrides or adapts the biome at world coordinate (wx, wz).
     * @param wx World block X
     * @param wz World block Z
     * @param currentBiome The procedurally selected biome
     * @return The modified or preserved biome
     */
    default Biome modifyBiome(int wx, int wz, Biome currentBiome) {
        return currentBiome;
    }
}

