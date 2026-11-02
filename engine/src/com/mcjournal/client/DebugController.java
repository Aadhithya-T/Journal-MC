package com.mcjournal.client;

import static org.lwjgl.glfw.GLFW.*;

/**
 * DebugController handles developer debugging hotkeys (F1-F12) to inspect rendering passes.
 */
public class DebugController {
    private boolean wasF1Down = false;
    private boolean wasF2Down = false;
    private boolean wasF3Down = false;

    public void update(InputHandler input) {
        update(input, null);
    }

    public void update(InputHandler input, MCJournalApp app) {
        if (input == null) return;

        // F1: Toggle HUD Visibility
        boolean f1Down = input.isKeyDown(GLFW_KEY_F1);
        if (f1Down && !wasF1Down) {
            if (app != null && app.getSettings() != null) {
                app.getSettings().toggleShowHud();
            }
        }
        wasF1Down = f1Down;

        // F2: Capture High-Resolution Screenshot
        boolean f2Down = input.isKeyDown(GLFW_KEY_F2);
        if (f2Down && !wasF2Down) {
            if (app != null) {
                app.takeScreenshot();
            }
        }
        wasF2Down = f2Down;

        // F3: Toggle Engine Debug Overlay
        boolean f3Down = input.isKeyDown(GLFW_KEY_F3);
        if (f3Down && !wasF3Down) {
            EngineMetrics.getInstance().toggleDebugOverlay();
        }
        wasF3Down = f3Down;

        if (input.isKeyDown(GLFW_KEY_F4)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_DIRECT_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F5)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_AMBIENT_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F6)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_AO;
        if (input.isKeyDown(GLFW_KEY_F7)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_TOTAL_LIGHT;
        if (input.isKeyDown(GLFW_KEY_F8)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_PRE_TONEMAP;
        if (input.isKeyDown(GLFW_KEY_F9)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_FOG;
        if (input.isKeyDown(GLFW_KEY_F10)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_ALBEDO;
        if (input.isKeyDown(GLFW_KEY_F11)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_DEPTH;
        if (input.isKeyDown(GLFW_KEY_F12)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_WATER_TRANSMISSION;
    }
}
