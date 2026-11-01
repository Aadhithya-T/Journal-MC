package com.mcjournal.world.terrain;

import com.mcjournal.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic pipeline of terrain modifiers applied in sequence during procedural generation.
 * Enables hybrid procedural/authored terrain (e.g. authored valleys, landmarks, POI clearing).
 */
public class TerrainModifierPipeline {

    private final List<TerrainModifier> modifiers = new ArrayList<>();

    public TerrainModifierPipeline() {}

    public TerrainModifierPipeline(List<TerrainModifier> initialModifiers) {
        if (initialModifiers != null) {
            for (TerrainModifier m : initialModifiers) {
                addModifier(m);
            }
        }
    }

    public synchronized void addModifier(TerrainModifier modifier) {
        modifiers.add(Objects.requireNonNull(modifier, "TerrainModifier cannot be null"));
    }

    public synchronized List<TerrainModifier> getModifiers() {
        return Collections.unmodifiableList(new ArrayList<>(modifiers));
    }

    public synchronized int size() {
        return modifiers.size();
    }

    /**
     * Executes all registered height modifiers in sequential order.
     */
    public synchronized double applyHeightModifiers(int wx, int wz, double proceduralHeight) {
        double height = proceduralHeight;
        for (TerrainModifier modifier : modifiers) {
            height = modifier.modifyHeight(wx, wz, height);
        }
        return height;
    }

    /**
     * Executes all registered biome modifiers in sequential order.
     */
    public synchronized Biome applyBiomeModifiers(int wx, int wz, Biome proceduralBiome) {
        Biome biome = proceduralBiome;
        for (TerrainModifier modifier : modifiers) {
            biome = modifier.modifyBiome(wx, wz, biome);
        }
        return biome;
    }

    public static TerrainModifierPipeline empty() {
        return new TerrainModifierPipeline();
    }
}

