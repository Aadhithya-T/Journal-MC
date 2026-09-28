package com.mcjournal.client;

import static org.lwjgl.glfw.GLFW.*;

/**
 * DebugController handles developer debugging hotkeys (F1-F12) to inspect rendering passes.
 */
public class DebugController {

    public void update(InputHandler input) {
        if (input == null) return;

        if (input.isKeyDown(GLFW_KEY_F1)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_NORMAL;
        if (input.isKeyDown(GLFW_KEY_F2)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_ALBEDO;
        if (input.isKeyDown(GLFW_KEY_F3)) RenderingConfig.currentDebugMode = RenderingConfig.DEBUG_MODE_NORMALS;
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
