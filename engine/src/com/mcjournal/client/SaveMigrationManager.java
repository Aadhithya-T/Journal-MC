package com.mcjournal.client;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkPos;
import com.mcjournal.ChunkSerializer;
import com.mcjournal.RegionFile;
import com.mcjournal.RegionManager;
import com.mcjournal.RegionPos;
import com.mcjournal.block.Blocks;
import com.mcjournal.world.WorldBounds;
import com.mcjournal.world.WorldConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration pipeline for converting legacy unbounded worlds (v3 binary or JSON)
 * into finite-world v4 saves without modifying or corrupting the original source save.
 */
public class SaveMigrationManager {

    public record MigrationReport(
        int sourceVersion,
        int destinationVersion,
        long seed,
        int importedChunkCount,
        int skippedChunkCount,
        List<ChunkPos> skippedChunks,
        boolean playerPositionCompatible,
        boolean playerRelocated,
        List<String> warnings,
        boolean success
    ) {
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== MIGRATION REPORT [v").append(sourceVersion).append(" -> v").append(destinationVersion).append("] ===\n");
            sb.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");
            sb.append("Seed: ").append(seed).append("\n");
            sb.append("Imported Chunks: ").append(importedChunkCount).append("\n");
            sb.append("Skipped Chunks: ").append(skippedChunkCount).append("\n");
            if (!skippedChunks.isEmpty()) {
                int displayLimit = Math.min(10, skippedChunks.size());
                sb.append("  Skipped Coordinates (showing ").append(displayLimit).append(" of ").append(skippedChunks.size()).append("): ");
                for (int i = 0; i < displayLimit; i++) {
                    sb.append(skippedChunks.get(i)).append(i + 1 < displayLimit ? ", " : "");
                }
                sb.append("\n");
            }
            sb.append("Player Position: ").append(playerPositionCompatible ? "Compatible (Preserved)" : "Out-of-bounds (Relocated to spawn)").append("\n");
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                for (String w : warnings) {
                    sb.append("  * ").append(w).append("\n");
                }
            }
            return sb.toString();
        }
    }

    /**
     * Explicitly migrates an existing world save to a separate target finite world save.
     * The source save is NEVER modified.
     *
     * @param sourceDir Directory containing source save (world.jmc or hardcore_world.json, plus regions/)
     * @param destDir Target directory for migrated finite save
     * @param targetConfig Target finite world configuration
     * @return MigrationReport summarizing imported chunks, skipped chunks, and player status
     */
    public static MigrationReport migrateToFiniteWorld(File sourceDir, File destDir, WorldConfiguration targetConfig) {
        List<String> warnings = new ArrayList<>();
        List<ChunkPos> skippedChunks = new ArrayList<>();
        int importedChunks = 0;
        int skippedCount = 0;

        if (sourceDir == null || !sourceDir.exists()) {
            warnings.add("Source world directory does not exist: " + sourceDir);
            return new MigrationReport(0, WorldSaveManager.FORMAT_VERSION, 0L, 0, 0, skippedChunks, false, false, warnings, false);
        }

        if (destDir == null) {
            warnings.add("Destination directory cannot be null");
            return new MigrationReport(0, WorldSaveManager.FORMAT_VERSION, 0L, 0, 0, skippedChunks, false, false, warnings, false);
        }

        // Rule 1: Never overwrite the original legacy save directory
        try {
            if (sourceDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
                warnings.add("Destination directory cannot be identical to source directory (source preservation rule)");
                return new MigrationReport(0, WorldSaveManager.FORMAT_VERSION, 0L, 0, 0, skippedChunks, false, false, warnings, false);
            }
        } catch (IOException e) {
            warnings.add("Failed to resolve directory canonical paths: " + e.getMessage());
            return new MigrationReport(0, WorldSaveManager.FORMAT_VERSION, 0L, 0, 0, skippedChunks, false, false, warnings, false);
        }

        if (targetConfig == null) {
            targetConfig = WorldConfiguration.createDefault(0L);
        }
        WorldBounds targetBounds = new WorldBounds(targetConfig);

        // 1. Locate and read source metadata
        File sourceMetaFile = new File(sourceDir, "world.jmc");
        File sourceLegacyJson = new File(sourceDir, "hardcore_world.json");
        WorldSaveManager.SavedWorld sourceSave = null;

        if (sourceMetaFile.exists() && sourceMetaFile.length() > 0) {
            sourceSave = WorldSaveManager.loadWorld(sourceMetaFile);
        }
        if (sourceSave == null && sourceLegacyJson.exists() && sourceLegacyJson.length() > 0) {
            sourceSave = WorldSaveManager.loadWorld(sourceLegacyJson);
        }

        if (sourceSave == null) {
            warnings.add("Could not find or parse valid world metadata in source directory");
            return new MigrationReport(0, WorldSaveManager.FORMAT_VERSION, 0L, 0, 0, skippedChunks, false, false, warnings, false);
        }

        int sourceVersion = sourceSave.formatVersion;
        long worldSeed = sourceSave.seed;

        // 2. Prepare destination directory and RegionManager
        destDir.mkdirs();
        File destRegionsDir = new File(destDir, "regions");
        destRegionsDir.mkdirs();

        try (RegionManager destRegionManager = new RegionManager(destDir, targetBounds)) {
            // 3. Scan and copy valid chunks from source regions
            File sourceRegionsDir = new File(sourceDir, "regions");
            if (sourceRegionsDir.exists() && sourceRegionsDir.isDirectory()) {
                File[] regionFiles = sourceRegionsDir.listFiles((dir, name) -> name.startsWith("r.") && name.endsWith(".jmc"));
                if (regionFiles != null) {
                    for (File rFile : regionFiles) {
                        try (RegionFile sourceRegion = new RegionFile(rFile)) {
                            // Extract rx and rz from filename "r.rx.rz.jmc"
                            String[] parts = rFile.getName().split("\\.");
                            if (parts.length >= 4) {
                                int rx = Integer.parseInt(parts[1]);
                                int rz = Integer.parseInt(parts[2]);

                                for (int localCz = 0; localCz < 32; localCz++) {
                                    for (int localCx = 0; localCx < 32; localCx++) {
                                        if (sourceRegion.hasChunk(localCx, localCz)) {
                                            int globalCx = rx * 32 + localCx;
                                            int globalCz = rz * 32 + localCz;

                                            if (targetBounds.isValidChunk(globalCx, globalCz)) {
                                                byte[] chunkData = sourceRegion.readChunkData(localCx, localCz);
                                                if (chunkData != null) {
                                                    Chunk chunk = ChunkSerializer.deserialize(chunkData);
                                                    if (chunk != null) {
                                                        // Sanitize partial edge chunks
                                                        if (targetBounds.isPartialChunk(globalCx, globalCz)) {
                                                            int maxLx = targetBounds.getValidLocalBlockCountX(globalCx);
                                                            int maxLz = targetBounds.getValidLocalBlockCountZ(globalCz);
                                                            for (int y = 0; y < Chunk.HEIGHT; y++) {
                                                                for (int z = 0; z < Chunk.SIZE; z++) {
                                                                    for (int x = 0; x < Chunk.SIZE; x++) {
                                                                        if (x >= maxLx || z >= maxLz) {
                                                                            chunk.setBlockState(x, y, z, Blocks.AIR.getDefaultState());
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        destRegionManager.saveChunk(chunk);
                                                        importedChunks++;
                                                    }
                                                }
                                            } else {
                                                skippedCount++;
                                                ChunkPos outPos = new ChunkPos(globalCx, globalCz);
                                                skippedChunks.add(outPos);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            warnings.add("Failed processing source region file " + rFile.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }

            // 4. Validate and relocate player position if necessary
            boolean playerCompatible = true;
            boolean playerRelocated = false;
            float px = sourceSave.playerX;
            float py = sourceSave.playerY;
            float pz = sourceSave.playerZ;

            if (!targetBounds.isPlayerAABBInsideWorld(px, py, pz)) {
                playerCompatible = false;
                playerRelocated = true;
                px = targetConfig.spawnX();
                py = (float) targetConfig.spawnY();
                pz = targetConfig.spawnZ();
                warnings.add("Source player AABB at (" + sourceSave.playerX + ", " + sourceSave.playerY + ", " + sourceSave.playerZ + ") extends outside target bounds; relocated to spawn (" + px + ", " + py + ", " + pz + ")");
            }

            // 5. Construct and write destination V4 metadata
            WorldSaveManager.SavedWorld destSave = new WorldSaveManager.SavedWorld();
            destSave.formatVersion = WorldSaveManager.FORMAT_VERSION;
            destSave.name = sourceSave.name != null ? sourceSave.name : "Migrated World";
            destSave.biome = sourceSave.biome != null ? sourceSave.biome : "Plains";
            destSave.seed = sourceSave.seed;
            destSave.worldTime = sourceSave.worldTime;

            destSave.playerX = px;
            destSave.playerY = py;
            destSave.playerZ = pz;
            destSave.playerYaw = sourceSave.playerYaw;
            destSave.playerPitch = sourceSave.playerPitch;
            destSave.selectedSlot = sourceSave.selectedSlot;
            destSave.hotbarBlocks = sourceSave.hotbarBlocks != null ? sourceSave.hotbarBlocks.clone() : new byte[9];
            destSave.hotbarCounts = sourceSave.hotbarCounts != null ? sourceSave.hotbarCounts.clone() : new int[9];

            destSave.worldWidth = targetConfig.width();
            destSave.worldDepth = targetConfig.depth();
            destSave.worldHeight = targetConfig.height();
            destSave.generationVersion = targetConfig.generationVersion();
            destSave.boundaryType = targetConfig.boundaryType().name();
            destSave.spawnX = targetConfig.spawnX();
            destSave.spawnY = targetConfig.spawnY();
            destSave.spawnZ = targetConfig.spawnZ();
            destSave.createdAt = sourceSave.createdAt;
            destSave.isLegacy = false;

            File destMetaFile = new File(destDir, "world.jmc");
            WorldSaveManager.saveBinaryDirect(destSave, destMetaFile);

            return new MigrationReport(
                sourceVersion,
                WorldSaveManager.FORMAT_VERSION,
                worldSeed,
                importedChunks,
                skippedCount,
                skippedChunks,
                playerCompatible,
                playerRelocated,
                warnings,
                true
            );
        } catch (Exception e) {
            warnings.add("Migration failed with exception: " + e.getMessage());
            return new MigrationReport(
                sourceVersion,
                WorldSaveManager.FORMAT_VERSION,
                worldSeed,
                importedChunks,
                skippedCount,
                skippedChunks,
                false,
                false,
                warnings,
                false
            );
        }
    }
}

