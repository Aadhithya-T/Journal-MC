package com.mcjournal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class WorldSaveManager {
    public static class SavedWorld {
        public String name;
        public String biome;
        public long seed;
        public String createdAt;

        // Player State
        public float playerX = 8.0f;
        public float playerY = 66.0f;
        public float playerZ = 8.0f;
        public float playerYaw = 0.0f;
        public float playerPitch = 0.0f;
        public int health = 20;
        public int hunger = 20;
        public int selectedSlot = 0;
        public byte[] hotbarBlocks = new byte[9];
        public int[] hotbarCounts = new int[9];

        // Continuous World Time (24,000 tick solar cycle: 6000 = Day Mid-Morning/Noon)
        public double worldTime = 6000.0;

        // Voxel Block State (Every single block broken, placed, or modified)
        public Map<String, Byte> modifiedBlocks = new HashMap<>();

        public SavedWorld() {}

        public SavedWorld(String name, String biome, long seed, String createdAt,
                          float px, float py, float pz, float yaw, float pitch,
                          int health, int hunger, int selectedSlot,
                          byte[] hotbarBlocks, int[] hotbarCounts,
                          double worldTime,
                          Map<String, Byte> modifiedBlocks) {
            this.name = name;
            this.biome = biome;
            this.seed = seed;
            this.createdAt = createdAt;
            this.playerX = px;
            this.playerY = py;
            this.playerZ = pz;
            this.playerYaw = yaw;
            this.playerPitch = pitch;
            this.health = health;
            this.hunger = hunger;
            this.selectedSlot = selectedSlot;
            this.hotbarBlocks = (hotbarBlocks != null) ? hotbarBlocks.clone() : new byte[9];
            this.hotbarCounts = (hotbarCounts != null) ? hotbarCounts.clone() : new int[9];
            this.worldTime = (worldTime >= 0.0) ? (worldTime % 24000.0) : 6000.0;
            this.modifiedBlocks = (modifiedBlocks != null) ? new HashMap<>(modifiedBlocks) : new HashMap<>();
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
            SavedWorld world = GSON.fromJson(reader, SavedWorld.class);
            if (world != null) {
                if (world.modifiedBlocks == null) {
                    world.modifiedBlocks = new HashMap<>();
                }
                if (world.hotbarBlocks == null) {
                    world.hotbarBlocks = new byte[9];
                }
                if (world.hotbarCounts == null) {
                    world.hotbarCounts = new int[9];
                }
                if (world.worldTime <= 0.0 && world.createdAt == null) {
                    world.worldTime = 6000.0;
                }
            }
            return world;
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Failed to load world: " + e.getMessage());
            return null;
        }
    }

    public static void saveWorld(String name, String biome, long seed,
                                 Player player, double worldTime, Map<String, Byte> modifiedBlocks) {
        try {
            SAVE_FILE.getParentFile().mkdirs();
            SavedWorld world = new SavedWorld(
                name,
                biome,
                seed,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                player != null ? player.pos.x : 8.0f,
                player != null ? player.pos.y : 66.0f,
                player != null ? player.pos.z : 8.0f,
                player != null ? player.yaw : 0.0f,
                player != null ? player.pitch : 0.0f,
                player != null ? player.health : 20,
                player != null ? player.hunger : 20,
                player != null ? player.selectedSlot : 0,
                player != null ? player.hotbarBlocks : new byte[9],
                player != null ? player.hotbarCounts : new int[9],
                worldTime,
                modifiedBlocks
            );

            try (FileWriter writer = new FileWriter(SAVE_FILE)) {
                GSON.toJson(world, writer);
            }
            int modCount = modifiedBlocks != null ? modifiedBlocks.size() : 0;
            System.out.println("[WorldSaveManager] Hardcore World saved (" + modCount + " block changes, time: " +
                    String.format("%.0f", world.worldTime) + " ticks, player at " +
                    String.format("%.1f, %.1f, %.1f", world.playerX, world.playerY, world.playerZ) + ") to " + SAVE_FILE.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Failed to save world: " + e.getMessage());
        }
    }

    public static void renameWorld(String newName) {
        SavedWorld world = loadWorld();
        if (world != null && newName != null && !newName.trim().isEmpty()) {
            world.name = newName.trim();
            try (FileWriter writer = new FileWriter(SAVE_FILE)) {
                GSON.toJson(world, writer);
                System.out.println("[WorldSaveManager] World renamed to: " + world.name);
            } catch (Exception e) {
                System.err.println("[WorldSaveManager] Failed to rename world: " + e.getMessage());
            }
        }
    }

    public static void deleteWorld() {
        if (SAVE_FILE.exists()) {
            boolean deleted = SAVE_FILE.delete();
            System.out.println("[WorldSaveManager] Hardcore World deleted: " + deleted);
        }
    }
}
