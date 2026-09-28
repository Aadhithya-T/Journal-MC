package com.mcjournal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcjournal.ChunkManager;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Unified binary world persistence manager (.jmc) with asynchronous save support (P9.1 - P9.4).
 * Stores world metadata and player state in versioned binary format (formatVersion = 3),
 * relying on RegionManager (.jmc) for all voxel block persistence.
 */
public class WorldSaveManager {
    public static final int MAGIC = 0x4A4D4357; // "JMCW" (Journal-MC World)
    public static final int FORMAT_VERSION = 3;

    private static final File SAVE_FILE_JMC = new File("saves/world.jmc");
    private static final File SAVE_FILE_LEGACY_JSON = new File("saves/hardcore_world.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WorldSave-Worker");
        t.setDaemon(true);
        return t;
    });

    public static class SavedWorld {
        public int formatVersion = FORMAT_VERSION;
        public String name = "Hardcore World";
        public String biome = "Plains";
        public long seed = 12345L;
        public String createdAt = "";
        public String savedAt = "";

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

        // Legacy delta map for migrating pre-v3 worlds (populated only during migration)
        public Map<String, String> modifiedBlockStates = new HashMap<>();

        public SavedWorld() {}

        public Map<String, BlockState> getBlockStateDeltas() {
            Map<String, BlockState> result = new HashMap<>();
            if (modifiedBlockStates != null && !modifiedBlockStates.isEmpty()) {
                for (Map.Entry<String, String> entry : modifiedBlockStates.entrySet()) {
                    result.put(entry.getKey(), BlockStateRegistry.parse(entry.getValue()));
                }
            }
            return result;
        }
    }

    public static boolean hasWorld() {
        return (SAVE_FILE_JMC.exists() && SAVE_FILE_JMC.length() > 0)
            || (SAVE_FILE_LEGACY_JSON.exists() && SAVE_FILE_LEGACY_JSON.length() > 0);
    }

    public static SavedWorld loadWorld() {
        if (!hasWorld()) return null;

        // 1. Try loading modern versioned binary save (.jmc)
        if (SAVE_FILE_JMC.exists() && SAVE_FILE_JMC.length() > 0) {
            try {
                return loadBinarySave(SAVE_FILE_JMC);
            } catch (Exception e) {
                System.err.println("[WorldSaveManager] Failed to load binary world.jmc: " + e.getMessage() + ", checking legacy fallback...");
            }
        }

        // 2. Fallback to legacy JSON format and automatically migrate to v3 .jmc
        if (SAVE_FILE_LEGACY_JSON.exists() && SAVE_FILE_LEGACY_JSON.length() > 0) {
            SavedWorld legacy = loadLegacyJsonSave(SAVE_FILE_LEGACY_JSON);
            if (legacy != null) {
                migrateLegacyToBinary(legacy);
                return legacy;
            }
        }

        return null;
    }

    private static SavedWorld loadBinarySave(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int magic = dis.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid world.jmc magic header: " + Integer.toHexString(magic));
            }

            int version = dis.readInt();
            if (version > FORMAT_VERSION) {
                throw new IOException("Unsupported world save version: " + version + " (max supported: " + FORMAT_VERSION + ")");
            }

            SavedWorld world = new SavedWorld();
            world.formatVersion = version;
            world.name = dis.readUTF();
            world.biome = dis.readUTF();
            world.seed = dis.readLong();
            world.worldTime = dis.readDouble();

            world.playerX = dis.readFloat();
            world.playerY = dis.readFloat();
            world.playerZ = dis.readFloat();
            world.playerYaw = dis.readFloat();
            world.playerPitch = dis.readFloat();

            world.health = dis.readInt();
            world.hunger = dis.readInt();
            world.selectedSlot = dis.readInt();

            world.hotbarBlocks = new byte[9];
            for (int i = 0; i < 9; i++) {
                world.hotbarBlocks[i] = dis.readByte();
            }

            world.hotbarCounts = new int[9];
            for (int i = 0; i < 9; i++) {
                world.hotbarCounts[i] = dis.readInt();
            }

            world.createdAt = dis.readUTF();
            world.savedAt = dis.readUTF();

            return world;
        }
    }

    private static SavedWorld loadLegacyJsonSave(File file) {
        try (FileReader reader = new FileReader(file)) {
            SavedWorld world = GSON.fromJson(reader, SavedWorld.class);
            if (world != null) {
                if (world.modifiedBlockStates == null) world.modifiedBlockStates = new HashMap<>();
                if (world.hotbarBlocks == null) world.hotbarBlocks = new byte[9];
                if (world.hotbarCounts == null) world.hotbarCounts = new int[9];
                if (world.worldTime <= 0.0 && world.createdAt == null) world.worldTime = 6000.0;
            }
            return world;
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Failed to read legacy json save: " + e.getMessage());
            return null;
        }
    }

    private static void migrateLegacyToBinary(SavedWorld legacy) {
        try {
            System.out.println("[WorldSaveManager] Migrating legacy JSON world save to version 3 binary .jmc format...");
            saveBinaryDirect(legacy, SAVE_FILE_JMC);

            File backup = new File(SAVE_FILE_LEGACY_JSON.getParentFile(), "hardcore_world.json.migrated");
            Files.move(SAVE_FILE_LEGACY_JSON.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[WorldSaveManager] Migration complete! Legacy JSON archived to " + backup.getName());
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Migration failed: " + e.getMessage());
        }
    }

    private static void saveBinaryDirect(SavedWorld world, File targetFile) throws IOException {
        targetFile.getParentFile().mkdirs();
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            dos.writeInt(MAGIC);
            dos.writeInt(FORMAT_VERSION);

            dos.writeUTF(world.name != null ? world.name : "Hardcore World");
            dos.writeUTF(world.biome != null ? world.biome : "Plains");
            dos.writeLong(world.seed);
            dos.writeDouble(world.worldTime);

            dos.writeFloat(world.playerX);
            dos.writeFloat(world.playerY);
            dos.writeFloat(world.playerZ);
            dos.writeFloat(world.playerYaw);
            dos.writeFloat(world.playerPitch);

            dos.writeInt(world.health);
            dos.writeInt(world.hunger);
            dos.writeInt(world.selectedSlot);

            for (int i = 0; i < 9; i++) {
                dos.writeByte(world.hotbarBlocks != null && i < world.hotbarBlocks.length ? world.hotbarBlocks[i] : 0);
            }
            for (int i = 0; i < 9; i++) {
                dos.writeInt(world.hotbarCounts != null && i < world.hotbarCounts.length ? world.hotbarCounts[i] : 0);
            }

            dos.writeUTF(world.createdAt != null ? world.createdAt : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            dos.writeUTF(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            dos.flush();
        }

        Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Asynchronous save: snapshot on main thread, flush chunks and metadata on background thread (P9.4).
     */
    public static CompletableFuture<Void> saveWorldAsync(
            String name, String biome, long seed, Player player, double worldTime, ChunkManager chunkManager) {

        // 1. Snapshot metadata and player state immediately on calling thread
        SavedWorld snapshot = new SavedWorld();
        snapshot.name = name;
        snapshot.biome = biome;
        snapshot.seed = seed;
        snapshot.worldTime = worldTime;
        if (player != null) {
            snapshot.playerX = player.pos.x;
            snapshot.playerY = player.pos.y;
            snapshot.playerZ = player.pos.z;
            snapshot.playerYaw = player.yaw;
            snapshot.playerPitch = player.pitch;
            snapshot.health = player.health;
            snapshot.hunger = player.hunger;
            snapshot.selectedSlot = player.selectedSlot;
            snapshot.hotbarBlocks = player.hotbarBlocks.clone();
            snapshot.hotbarCounts = player.hotbarCounts.clone();
        }

        // 2. Snapshot and trigger asynchronous chunk save
        CompletableFuture<Void> chunkSaveFuture = (chunkManager != null)
                ? chunkManager.saveAllModifiedChunksAsync()
                : CompletableFuture.completedFuture(null);

        // 3. Write binary metadata file asynchronously
        CompletableFuture<Void> metaSaveFuture = CompletableFuture.runAsync(() -> {
            try {
                saveBinaryDirect(snapshot, SAVE_FILE_JMC);
                System.out.println("[WorldSaveManager] Saved binary world metadata to " + SAVE_FILE_JMC.getName() + " (v" + FORMAT_VERSION + ")");
            } catch (IOException e) {
                System.err.println("[WorldSaveManager] Failed to write binary world.jmc: " + e.getMessage());
            }
        }, SAVE_EXECUTOR);

        return CompletableFuture.allOf(chunkSaveFuture, metaSaveFuture);
    }

    public static void saveWorld(String name, String biome, long seed, Player player, double worldTime, ChunkManager chunkManager) {
        try {
            saveWorldAsync(name, biome, seed, player, worldTime, chunkManager).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[WorldSaveManager] Synchronous world save failed: " + e.getMessage());
        }
    }

    @Deprecated
    public static void saveWorld(String name, String biome, long seed, Player player, double worldTime, Map<String, BlockState> unused) {
        saveWorld(name, biome, seed, player, worldTime, (ChunkManager) null);
    }

    public static void renameWorld(String newName) {
        SavedWorld world = loadWorld();
        if (world != null && newName != null && !newName.trim().isEmpty()) {
            world.name = newName.trim();
            try {
                saveBinaryDirect(world, SAVE_FILE_JMC);
                System.out.println("[WorldSaveManager] World renamed to: " + world.name);
            } catch (Exception e) {
                System.err.println("[WorldSaveManager] Failed to rename world: " + e.getMessage());
            }
        }
    }

    public static void deleteWorld() {
        if (SAVE_FILE_JMC.exists()) {
            SAVE_FILE_JMC.delete();
        }
        if (SAVE_FILE_LEGACY_JSON.exists()) {
            SAVE_FILE_LEGACY_JSON.delete();
        }
        File legacyMigrated = new File("saves/hardcore_world.json.migrated");
        if (legacyMigrated.exists()) {
            legacyMigrated.delete();
        }

        // Recursively clean region files in saves/
        File savesDir = new File("saves");
        if (savesDir.exists()) {
            deleteDirectoryContents(new File(savesDir, "regions"));
            File[] subdirs = savesDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File dir : subdirs) {
                    deleteDirectoryContents(dir);
                    dir.delete();
                }
            }
        }
        System.out.println("[WorldSaveManager] World save and chunk regions permanently deleted.");
    }

    private static void deleteDirectoryContents(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectoryContents(f);
                f.delete();
            }
        }
        dir.delete();
    }
}
