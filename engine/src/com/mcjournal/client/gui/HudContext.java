package com.mcjournal.client.gui;

import com.mcjournal.client.*;
import com.mcjournal.world.biome.Biome;

/**
 * Read-only snapshot of current frame gameplay state required for HUD rendering.
 * Avoids per-frame allocations by allowing reusing/updating a single mutable context instance.
 */
public class HudContext {
    public Player player;
    public WorldSession worldSession;
    public AtmosphericTimeSystem timeSystem;
    public GameSettings settings;
    public Raycast.Hit targetedBlock;
    public float breakProgress;
    public String currentBiome = "Plains";
    public int atlasTextureId = 0;
    public int currentFps = 60;
    public int renderedChunks = 0;
    public int loadedChunks = 0;
    public float partialTick = 0.0f;
    public int screenWidth = 480;
    public int screenHeight = 270;

    public void update(Player player, WorldSession worldSession, AtmosphericTimeSystem timeSystem,
                       GameSettings settings, Raycast.Hit targetedBlock, float breakProgress,
                       String currentBiome, int atlasTextureId, int currentFps,
                       int renderedChunks, int loadedChunks, float partialTick,
                       int screenWidth, int screenHeight) {
        this.player = player;
        this.worldSession = worldSession;
        this.timeSystem = timeSystem;
        this.settings = settings;
        this.targetedBlock = targetedBlock;
        this.breakProgress = breakProgress;
        this.currentBiome = (currentBiome != null) ? currentBiome : "Plains";
        this.atlasTextureId = atlasTextureId;
        this.currentFps = currentFps;
        this.renderedChunks = renderedChunks;
        this.loadedChunks = loadedChunks;
        this.partialTick = partialTick;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public boolean hasPlayer() {
        return player != null;
    }

    public boolean hasTarget() {
        return targetedBlock != null;
    }
}
