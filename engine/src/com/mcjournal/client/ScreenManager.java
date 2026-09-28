package com.mcjournal.client;

import com.mcjournal.client.gui.FontRenderer;
import com.mcjournal.client.gui.GuiRenderer;
import com.mcjournal.client.gui.Screen;

/**
 * ScreenManager coordinates active GUI screen transitions, mouse cursor state,
 * input forwarding, and GUI rendering passes.
 */
public class ScreenManager {
    private Screen currentScreen;

    public void setScreen(Screen screen, Window window, BlockBreakingManager breakingManager) {
        this.currentScreen = screen;
        if (screen != null) {
            if (window != null) window.lockCursor(false);
            if (window != null) screen.init(window.getWidth(), window.getHeight());
            if (breakingManager != null) breakingManager.resetBreak();
        } else {
            if (window != null) window.lockCursor(true);
        }
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public boolean hasScreen() {
        return currentScreen != null;
    }

    public void update(InputHandler input) {
        if (currentScreen != null && input != null) {
            currentScreen.update(input.getMouseX(), input.getMouseY());
        }
    }

    public void onResize(int width, int height) {
        if (currentScreen != null) {
            currentScreen.init(width, height);
        }
    }

    public void render(GuiRenderer guiRenderer, FontRenderer fontRenderer, InputHandler input, float deltaTime, int width, int height) {
        if (currentScreen != null && guiRenderer != null && fontRenderer != null && input != null) {
            guiRenderer.begin(width, height);
            currentScreen.render(guiRenderer, fontRenderer, input.getMouseX(), input.getMouseY(), deltaTime);
            guiRenderer.end();
        }
    }
}
