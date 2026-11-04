package com.mcjournal.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Encapsulates an isolated on-disk storage directory for a specific world.
 * Enforces strict path traversal security, atomic file replacement, and
 * independent storage boundaries for world data, chunk regions, landmarks,
 * and future journal files.
 */
public class WorldStorageDirectory {

    private final String worldId;
    private final File rootDir;

    public WorldStorageDirectory(File baseSavesDir, String worldId) {
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("World ID cannot be null or blank");
        }
        this.worldId = worldId.trim();
        File savesDir = (baseSavesDir != null) ? baseSavesDir : new File("saves");
        this.rootDir = new File(savesDir, sanitizeFileName(this.worldId));
        this.rootDir.mkdirs();
    }

    public WorldStorageDirectory(File explicitWorldDir) {
        if (explicitWorldDir == null) {
            throw new IllegalArgumentException("World directory cannot be null");
        }
        this.rootDir = explicitWorldDir;
        this.worldId = explicitWorldDir.getName();
        this.rootDir.mkdirs();
    }

    public String getWorldId() {
        return worldId;
    }

    public File getRootDir() {
        return rootDir;
    }

    /**
     * Resolves a relative path within this world's directory.
     * Prevents path traversal vulnerabilities: throws SecurityException if the path attempts to escape rootDir.
     */
    public File resolvePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path cannot be null or blank");
        }

        Path basePath = rootDir.toPath().toAbsolutePath().normalize();
        Path resolved = basePath.resolve(relativePath).normalize();

        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }

        return resolved.toFile();
    }

    // Standard Files and Subdirectories
    public File getMetadataFile() {
        return resolvePath("world_metadata.json");
    }

    public File getBinarySaveFile() {
        return resolvePath("world.jmc");
    }

    public File getLandmarksFile() {
        return resolvePath("landmarks.json");
    }

    public File getRegionsDir() {
        File dir = resolvePath("regions");
        dir.mkdirs();
        return dir;
    }

    public File getJournalDir() {
        File dir = resolvePath("journal");
        dir.mkdirs();
        return dir;
    }

    public File getJournalEntriesDir() {
        File dir = resolvePath("journal/entries");
        dir.mkdirs();
        return dir;
    }

    public File getJournalAttachmentsDir() {
        File dir = resolvePath("journal/attachments");
        dir.mkdirs();
        return dir;
    }

    public File getBackupsDir() {
        File dir = resolvePath("backups");
        dir.mkdirs();
        return dir;
    }

    /**
     * Atomically writes string content to target file using a temporary file and atomic rename.
     */
    public static void writeStringAtomic(File targetFile, String content) throws IOException {
        if (targetFile == null || content == null) return;
        writeBytesAtomic(targetFile, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Atomically writes raw byte content to target file using a temporary file and atomic rename.
     */
    public static void writeBytesAtomic(File targetFile, byte[] data) throws IOException {
        if (targetFile == null || data == null) return;

        File parent = targetFile.getParentFile();
        if (parent != null) parent.mkdirs();

        File tempFile = new File(parent, targetFile.getName() + ".tmp");

        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(data);
                fos.flush();
                fos.getFD().sync();
            }

            try {
                Files.move(tempFile.toPath(), targetFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        }
    }

    /**
     * Reads entire file contents as a UTF-8 string safely.
     * Returns null if file does not exist or is empty.
     */
    public static String readStringSafe(File file) {
        if (file == null || !file.exists() || file.length() == 0) return null;
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[WorldStorageDirectory] Error reading " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

