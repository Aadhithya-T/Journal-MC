package com.mcjournal.world.biome;

import com.mcjournal.block.Blocks;

import java.util.*;

/**
 * Thread-safe biome registry enforcing a non-negotiable maximum of 10 registered biomes.
 * Rejects duplicate IDs and invalid biome definitions, and supplies an authoritative default biome.
 */
public class BiomeRegistry {

    public static final int MAX_BIOMES = 10;

    private final Map<String, Biome> biomes = new LinkedHashMap<>();
    private final Biome defaultBiome;

    /**
     * Constructs a registry with the specified fallback default biome.
     */
    public BiomeRegistry(Biome defaultBiome) {
        if (defaultBiome == null) {
            throw new IllegalArgumentException("Default biome cannot be null");
        }
        this.defaultBiome = defaultBiome;
        register(defaultBiome);
    }

    /**
     * Registers a new biome.
     * Enforces the MAX_BIOMES limit (10) and uniqueness of biome IDs.
     */
    public synchronized void register(Biome biome) {
        if (biome == null) {
            throw new IllegalArgumentException("Cannot register null biome");
        }
        if (biomes.containsKey(biome.getId())) {
            throw new IllegalArgumentException("Duplicate biome ID already registered: " + biome.getId());
        }
        if (biomes.size() >= MAX_BIOMES) {
            throw new IllegalStateException("Biome registry maximum limit (" + MAX_BIOMES + ") reached. Cannot register: " + biome.getId());
        }
        biomes.put(biome.getId(), biome);
    }

    public synchronized Biome get(String id) {
        if (id == null) return defaultBiome;
        return biomes.getOrDefault(id.toLowerCase().trim(), defaultBiome);
    }

    public synchronized boolean hasBiome(String id) {
        if (id == null) return false;
        return biomes.containsKey(id.toLowerCase().trim());
    }

    public Biome getDefaultBiome() {
        return defaultBiome;
    }

    public synchronized int size() {
        return biomes.size();
    }

    public synchronized Collection<Biome> getAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(biomes.values()));
    }

    /**
     * Factory method creating a standard registry populated with initial exploration biomes:
     * Plains (default), Forest, Mountains, Desert, and Ocean (5 out of 10 maximum).
     */
    public static BiomeRegistry createDefaultRegistry() {
        Biome plains = Biome.builder("plains", "Lush Plains")
                .climate(0.2, 0.1, 0.2)
                .terrain(64.0, 6.0, 1.0)
                .surfaceBlock(Blocks.GRASS.getDefaultState())
                .subsurfaceBlock(Blocks.DIRT.getDefaultState())
                .deepBlock(Blocks.STONE.getDefaultState())
                .underwaterBlock(Blocks.SAND.getDefaultState())
                .treeDensity(0.12)
                .foliageDensity(0.35)
                .build();

        BiomeRegistry registry = new BiomeRegistry(plains);

        Biome forest = Biome.builder("forest", "Oak & Birch Forest")
                .climate(0.1, 0.6, 0.3)
                .terrain(66.0, 9.0, 1.1)
                .surfaceBlock(Blocks.GRASS.getDefaultState())
                .subsurfaceBlock(Blocks.DIRT.getDefaultState())
                .deepBlock(Blocks.STONE.getDefaultState())
                .underwaterBlock(Blocks.DIRT.getDefaultState())
                .treeDensity(0.65)
                .foliageDensity(0.40)
                .build();

        Biome mountains = Biome.builder("mountains", "Craggy Mountains")
                .climate(-0.3, -0.1, 0.7)
                .terrain(82.0, 36.0, 2.2)
                .surfaceBlock(Blocks.STONE.getDefaultState())
                .subsurfaceBlock(Blocks.STONE.getDefaultState())
                .deepBlock(Blocks.STONE.getDefaultState())
                .underwaterBlock(Blocks.COBBLESTONE.getDefaultState())
                .treeDensity(0.04)
                .foliageDensity(0.08)
                .build();

        Biome desert = Biome.builder("desert", "Arid Dunes")
                .climate(0.8, -0.8, 0.3)
                .terrain(63.0, 5.0, 0.9)
                .surfaceBlock(Blocks.SAND.getDefaultState())
                .subsurfaceBlock(Blocks.SAND.getDefaultState())
                .deepBlock(Blocks.STONE.getDefaultState())
                .underwaterBlock(Blocks.SAND.getDefaultState())
                .treeDensity(0.0)
                .foliageDensity(0.0)
                .build();

        Biome ocean = Biome.builder("ocean", "Coastal Ocean")
                .climate(0.0, 0.4, -0.6)
                .terrain(50.0, 7.0, 0.8)
                .surfaceBlock(Blocks.SAND.getDefaultState())
                .subsurfaceBlock(Blocks.DIRT.getDefaultState())
                .deepBlock(Blocks.STONE.getDefaultState())
                .underwaterBlock(Blocks.SAND.getDefaultState())
                .treeDensity(0.0)
                .foliageDensity(0.0)
                .build();

        registry.register(forest);
        registry.register(mountains);
        registry.register(desert);
        registry.register(ocean);

        return registry;
    }
}

