package com.mcjournal.client;

import com.mcjournal.client.gui.FontRenderer;
import com.mcjournal.client.gui.GuiRenderer;
import com.mcjournal.client.gui.GuiScaleManager;
import com.mcjournal.client.gui.Screen;

/**
 * ScreenManager coordinates active GUI screen transitions, mouse cursor state,
 * input forwarding, virtual resolution scaling, and GUI rendering passes.
 */
public class ScreenManager {
    private Screen currentScreen;
    private final GuiScaleManager scaleManager = new GuiScaleManager();

    public GuiScaleManager getScaleManager() {
        return scaleManager;
    }

    public void setScreen(Screen screen, Window window, BlockBreakingManager breakingManager) {
        this.currentScreen = screen;
        if (screen != null) {
            if (window != null) {
                window.lockCursor(false);
                scaleManager.update(window.getWidth(), window.getHeight(), window.getWidth(), window.getHeight());
            }
            int initW = screen.usesLogicalCoordinates() ? scaleManager.getGuiWidth() : (window != null ? window.getWidth() : scaleManager.getFramebufferWidth());
            int initH = screen.usesLogicalCoordinates() ? scaleManager.getGuiHeight() : (window != null ? window.getHeight() : scaleManager.getFramebufferHeight());
            screen.init(initW, initH);
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
            if (currentScreen.usesLogicalCoordinates()) {
                double mx = scaleManager.windowToGuiX(input.getMouseX());
                double my = scaleManager.windowToGuiY(input.getMouseY());
                currentScreen.update(mx, my);
            } else {
                currentScreen.update(input.getMouseX(), input.getMouseY());
            }
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (currentScreen == null) return false;
        if (currentScreen.usesLogicalCoordinates()) {
            if (!scaleManager.isMouseInViewport(mouseX, mouseY)) {
                return false; // Prevent clicks outside GUI viewport from activating interface elements
            }
            double guiX = scaleManager.windowToGuiX(mouseX);
            double guiY = scaleManager.windowToGuiY(mouseY);
            return currentScreen.mouseClicked(guiX, guiY, button);
        } else {
            return currentScreen.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void onResize(int width, int height) {
        scaleManager.update(width, height, width, height);
        if (currentScreen != null) {
            if (currentScreen.usesLogicalCoordinates()) {
                currentScreen.init(scaleManager.getGuiWidth(), scaleManager.getGuiHeight());
            } else {
                currentScreen.init(width, height);
            }
        }
    }

    public void render(GuiRenderer guiRenderer, FontRenderer fontRenderer, InputHandler input, float deltaTime, int width, int height) {
        if (currentScreen != null && guiRenderer != null && fontRenderer != null && input != null) {
            scaleManager.update(width, height, width, height);
            if (currentScreen.usesLogicalCoordinates()) {
                guiRenderer.begin(scaleManager.getGuiWidth(), scaleManager.getGuiHeight(),
                        scaleManager.getViewportX(), scaleManager.getViewportY(),
                        scaleManager.getViewportWidth(), scaleManager.getViewportHeight());
                try {
                    double mx = scaleManager.windowToGuiX(input.getMouseX());
                    double my = scaleManager.windowToGuiY(input.getMouseY());
                    currentScreen.render(guiRenderer, fontRenderer, mx, my, deltaTime);
                } finally {
                    guiRenderer.end();
                }
            } else {
                guiRenderer.begin(width, height);
                try {
                    currentScreen.render(guiRenderer, fontRenderer, input.getMouseX(), input.getMouseY(), deltaTime);
                } finally {
                    guiRenderer.end();
                }
            }
        }
    }
}
