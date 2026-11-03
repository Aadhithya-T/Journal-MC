package com.mcjournal.client;

import com.mcjournal.client.gui.FontRenderer;
import com.mcjournal.client.gui.GuiRenderer;
import com.mcjournal.client.gui.GuiScaleManager;
import com.mcjournal.client.gui.Screen;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ScreenManager coordinates GUI screen transitions, screen stack navigation (push/pop),
 * mouse cursor locking, input forwarding, virtual resolution scaling, and GUI rendering.
 */
public class ScreenManager {
    private final Deque<Screen> screenStack = new ArrayDeque<>();
    private Screen currentScreen = null;
    private FontRenderer lastFontRenderer = null;
    private final GuiScaleManager scaleManager = new GuiScaleManager();
    private Window window;

    public void setFontRenderer(FontRenderer fontRenderer) {
        this.lastFontRenderer = fontRenderer;
    }

    public FontRenderer getLastFontRenderer() {
        return lastFontRenderer;
    }

    public GuiScaleManager getScaleManager() {
        return scaleManager;
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public boolean hasScreen() {
        return currentScreen != null;
    }

    public int getStackDepth() {
        return screenStack.size();
    }

    public boolean isInputConsumed() {
        return currentScreen != null && currentScreen.getFocusedWidget() != null;
    }

    /**
     * Sets or replaces the current root screen, closing all previous screens on the stack.
     */
    public void setScreen(Screen screen, Window window) {
        this.window = window;

        // Cleanly close previous screen stack
        while (!screenStack.isEmpty()) {
            Screen s = screenStack.pop();
            s.onClosed();
            s.dispose();
        }

        this.currentScreen = screen;
        if (screen != null) {
            screenStack.push(screen);
            if (window != null) {
                window.lockCursor(false);
                scaleManager.update(window.getWidth(), window.getHeight(), window.getWidth(), window.getHeight());
            }
            int initW = screen.usesLogicalCoordinates() ? scaleManager.getGuiWidth() : (window != null ? window.getWidth() : scaleManager.getFramebufferWidth());
            int initH = screen.usesLogicalCoordinates() ? scaleManager.getGuiHeight() : (window != null ? window.getHeight() : scaleManager.getFramebufferHeight());
            screen.init(initW, initH, lastFontRenderer);
            screen.onOpened();
        } else {
            if (window != null) window.lockCursor(true);
        }
    }

    @Deprecated
    public void setScreen(Screen screen, Window window, Object unused) {
        setScreen(screen, window);
    }

    /**
     * Pushes a child screen onto the navigation stack, preserving the parent screen's state.
     */
    public void pushScreen(Screen child) {
        if (child == null) return;

        if (currentScreen != null) {
            currentScreen.onDeactivated();
        }

        screenStack.push(child);
        this.currentScreen = child;

        if (window != null) {
            window.lockCursor(false);
            scaleManager.update(window.getWidth(), window.getHeight(), window.getWidth(), window.getHeight());
        }

        int initW = child.usesLogicalCoordinates() ? scaleManager.getGuiWidth() : (window != null ? window.getWidth() : scaleManager.getFramebufferWidth());
        int initH = child.usesLogicalCoordinates() ? scaleManager.getGuiHeight() : (window != null ? window.getHeight() : scaleManager.getFramebufferHeight());
        child.init(initW, initH, lastFontRenderer);
        child.onOpened();
    }

    /**
     * Pops the current screen from the stack, restoring the previous parent screen.
     */
    public Screen popScreen() {
        if (screenStack.isEmpty()) {
            currentScreen = null;
            if (window != null) window.lockCursor(true);
            return null;
        }

        Screen closing = screenStack.pop();
        closing.onClosed();
        closing.dispose();

        if (!screenStack.isEmpty()) {
            currentScreen = screenStack.peek();
            currentScreen.onReactivated();
            if (window != null) {
                window.lockCursor(false);
            }
        } else {
            currentScreen = null;
            if (window != null) {
                window.lockCursor(true);
            }
        }
        return currentScreen;
    }

    public void closeAllScreens() {
        while (!screenStack.isEmpty()) {
            Screen s = screenStack.pop();
            s.onClosed();
            s.dispose();
        }
        currentScreen = null;
        if (window != null) {
            window.lockCursor(true);
        }
    }

    // --- Input Forwarding ---

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
                return false;
            }
            double guiX = scaleManager.windowToGuiX(mouseX);
            double guiY = scaleManager.windowToGuiY(mouseY);
            return currentScreen.mouseClicked(guiX, guiY, button);
        } else {
            return currentScreen.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (currentScreen == null) return false;
        if (currentScreen.usesLogicalCoordinates()) {
            double guiX = scaleManager.windowToGuiX(mouseX);
            double guiY = scaleManager.windowToGuiY(mouseY);
            return currentScreen.mouseReleased(guiX, guiY, button);
        } else {
            return currentScreen.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (currentScreen == null) return false;
        if (currentScreen.usesLogicalCoordinates()) {
            double guiX = scaleManager.windowToGuiX(mouseX);
            double guiY = scaleManager.windowToGuiY(mouseY);
            return currentScreen.mouseScrolled(guiX, guiY, scrollDelta);
        } else {
            return currentScreen.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
    }

    public void onResize(int width, int height) {
        scaleManager.update(width, height, width, height);
        for (Screen screen : screenStack) {
            if (screen.usesLogicalCoordinates()) {
                screen.init(scaleManager.getGuiWidth(), scaleManager.getGuiHeight(), lastFontRenderer);
            } else {
                screen.init(width, height, lastFontRenderer);
            }
        }
    }

    public void render(GuiRenderer guiRenderer, FontRenderer fontRenderer, InputHandler input, float deltaTime, int width, int height) {
        this.lastFontRenderer = fontRenderer;
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
