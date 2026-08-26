package com.mcjournal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Persisted user game & video settings.
 */
public class GameSettings {
    private static final File SETTINGS_FILE = new File("options.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 0: VSync, 30, 60, 90, 120, 144, 240, -1: Unlimited
    public int maxFps = 60;
    public boolean vsync = true;
    public int renderDistance = 12; // In chunks (4..24)
    public float fov = 70.0f;       // In degrees (60..110)
    public boolean showFps = true;
    public int fogMode = 0;         // 0: Fancy, 1: Fast, 2: Off

    public static final int[] FPS_PRESETS = {0, 30, 60, 90, 120, 144, 240, -1};
    public static final int[] RENDER_DISTANCE_PRESETS = {4, 6, 8, 10, 12, 16, 20, 24};

    public static GameSettings load() {
        if (SETTINGS_FILE.exists()) {
            try (FileReader reader = new FileReader(SETTINGS_FILE)) {
                GameSettings s = GSON.fromJson(reader, GameSettings.class);
                if (s != null) {
                    s.renderDistance = Math.clamp(s.renderDistance, 4, 24);
                    s.fov = Math.clamp(s.fov, 60.0f, 110.0f);
                    return s;
                }
            } catch (Exception e) {
                System.err.println("[GameSettings] Failed to load settings: " + e.getMessage());
            }
        }
        GameSettings defaults = new GameSettings();
        defaults.save();
        return defaults;
    }

    public void save() {
        try {
            SETTINGS_FILE.getParentFile();
            try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[GameSettings] Failed to save settings: " + e.getMessage());
        }
    }

    public String getFpsDisplayString() {
        if (vsync || maxFps == 0) return "VSync (60 FPS)";
        if (maxFps < 0) return "Unlimited";
        return maxFps + " FPS";
    }

    public void cycleFpsLimit() {
        int currentIndex = 0;
        for (int i = 0; i < FPS_PRESETS.length; i++) {
            if (FPS_PRESETS[i] == maxFps && (maxFps != 0 || vsync)) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % FPS_PRESETS.length;
        int nextVal = FPS_PRESETS[nextIndex];
        if (nextVal == 0) {
            vsync = true;
            maxFps = 0;
        } else {
            vsync = false;
            maxFps = nextVal;
        }
        save();
    }

    public void cycleRenderDistance() {
        int currentIndex = 4;
        for (int i = 0; i < RENDER_DISTANCE_PRESETS.length; i++) {
            if (RENDER_DISTANCE_PRESETS[i] == renderDistance) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % RENDER_DISTANCE_PRESETS.length;
        renderDistance = RENDER_DISTANCE_PRESETS[nextIndex];
        save();
    }

    public void cycleFov() {
        fov += 10.0f;
        if (fov > 110.0f) fov = 60.0f;
        save();
    }

    public void toggleShowFps() {
        showFps = !showFps;
        save();
    }

    public void cycleFogMode() {
        fogMode = (fogMode + 1) % 3;
        save();
    }

    public String getFogModeString() {
        return switch (fogMode) {
            case 0 -> "Fancy";
            case 1 -> "Fast";
            default -> "OFF";
        };
    }
}
