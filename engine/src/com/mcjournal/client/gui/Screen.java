package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all GUI screens.
 * Coordinates widget collections, focus cycling (Tab / Shift+Tab),
 * input routing, tooltip rendering, and stack lifecycle events.
 */
public abstract class Screen {
    protected final MCJournalApp app;
    protected int width;
    protected int height;

    protected final List<Widget> widgets = new ArrayList<>();
    // Backward compatibility for existing screens accessing 'buttons' directly
    protected final List<Button> buttons = new ArrayList<>();
    protected final FocusManager focusManager = new FocusManager();
    protected FontRenderer font;

    public Screen(MCJournalApp app) {
        this.app = app;
    }

    public void setFont(FontRenderer font) {
        this.font = font;
    }

    public FontRenderer getFont() {
        if (this.font == null) {
            this.font = new FontRenderer();
        }
        return this.font;
    }

    public boolean usesLogicalCoordinates() {
        return false;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public FocusManager getFocusManager() {
        return focusManager;
    }

    public Widget getFocusedWidget() {
        return focusManager.getFocusedWidget();
    }

    public void addWidget(Widget widget) {
        if (widget != null && !widgets.contains(widget)) {
            widgets.add(widget);
            if (widget instanceof Button btn && !buttons.contains(btn)) {
                buttons.add(btn);
            }
        }
    }

    public void removeWidget(Widget widget) {
        if (widget != null) {
            widgets.remove(widget);
            buttons.remove(widget);
            focusManager.onWidgetRemoved(widget);
        }
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        this.widgets.clear();
        this.buttons.clear();
        this.focusManager.clearFocus();
    }

    public void init(int width, int height, FontRenderer font) {
        if (font != null) {
            this.font = font;
        }
        init(width, height);
    }

    /**
     * Synchronizes any buttons directly added to the legacy 'buttons' list into 'widgets'.
     */
    protected void syncLegacyButtons() {
        for (Button btn : buttons) {
            if (!widgets.contains(btn)) {
                widgets.add(btn);
            }
        }
    }

    public void update(double mouseX, double mouseY) {
        syncLegacyButtons();
        for (Widget widget : widgets) {
            widget.update(mouseX, mouseY);
        }
    }

    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (font != null) { this.font = font; }
        syncLegacyButtons();

        // 1. Render all visible widgets
        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.render(gui, font, mouseX, mouseY, deltaTime);
            }
        }

        // 2. Render tooltips on top of all widgets
        for (Widget widget : widgets) {
            if (widget.isVisible() && widget.isHovered()) {
                widget.renderTooltip(gui, font, mouseX, mouseY, width, height);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncLegacyButtons();

        // Focus handling
        focusManager.handleClick(widgets, mouseX, mouseY);

        // Deliver click in reverse painter's order (topmost widget first)
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(mouseX, mouseY)) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        syncLegacyButtons();
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible()) {
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        syncLegacyButtons();
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible()) {
                if (widget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        syncLegacyButtons();
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget widget = widgets.get(i);
            if (widget.isVisible() && widget.isMouseOver(mouseX, mouseY)) {
                if (widget.mouseScrolled(mouseX, mouseY, scrollDelta)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        syncLegacyButtons();

        // Tab / Shift+Tab keyboard focus navigation
        if (keyCode == GLFW.GLFW_KEY_TAB && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
            boolean reverse = (mods & GLFW.GLFW_MOD_SHIFT) != 0;
            if (focusManager.cycleFocus(widgets, reverse)) {
                return;
            }
        }

        // Deliver to focused widget first
        Widget focused = focusManager.getFocusedWidget();
        if (focused != null && focused.isVisible() && focused.isEnabled()) {
            if (focused.keyPressed(keyCode, scanCode, action, mods)) {
                return;
            }
        }
    }

    public void keyReleased(int keyCode, int scanCode, int action, int mods) {
        syncLegacyButtons();
        Widget focused = focusManager.getFocusedWidget();
        if (focused != null && focused.isVisible() && focused.isEnabled()) {
            focused.keyReleased(keyCode, scanCode, action, mods);
        }
    }

    public void charTyped(char character) {
        syncLegacyButtons();
        Widget focused = focusManager.getFocusedWidget();
        if (focused != null && focused.isVisible() && focused.isEnabled()) {
            focused.charTyped(character);
        }
    }

    // --- Screen Lifecycle Events ---

    public void onOpened() {
    }

    public void onClosed() {
        focusManager.clearFocus();
    }

    public void onDeactivated() {
        // Called when a child screen is pushed on top of this screen
    }

    public void onReactivated() {
        // Called when child screen is closed and this screen is resumed
    }

    public void dispose() {
        widgets.clear();
        buttons.clear();
        focusManager.clearFocus();
    }
}
