package com.mcjournal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class WorldSaveManager {
    public static class SavedWorld {
        public String name;
        public String biome;
        public long seed;
        public String createdAt;

        public SavedWorld(String name, String biome, long seed, String createdAt) {
            this.name = name;
            this.biome = biome;
            this.seed = seed;
            this.createdAt = createdAt;
        }
    }

    private static final File SAVE_FILE = new File("saves/hardcore_world.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean hasWorld() {
        return SAVE_FILE.exists() && SAVE_FILE.length() > 0;
    }

    public static SavedWorld loadWorld() {
        if (!hasWorld()) return null;
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            return GSON.fromJson(reader, SavedWorld.class);
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Failed to load world: " + e.getMessage());
            return null;
        }
    }

    public static void saveWorld(String name, String biome, long seed) {
        try {
            SAVE_FILE.getParentFile().mkdirs();
            SavedWorld world = new SavedWorld(
                name,
                biome,
                seed,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
            try (FileWriter writer = new FileWriter(SAVE_FILE)) {
                GSON.toJson(world, writer);
            }
            System.out.println("[WorldSaveManager] Hardcore World saved successfully to " + SAVE_FILE.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Failed to save world: " + e.getMessage());
        }
    }

    public static void deleteWorld() {
        if (SAVE_FILE.exists()) {
            boolean deleted = SAVE_FILE.delete();
            System.out.println("[WorldSaveManager] Hardcore World deleted: " + deleted);
        }
    }
}
