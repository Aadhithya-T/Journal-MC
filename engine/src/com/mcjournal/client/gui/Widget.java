package com.mcjournal.client.gui;

/**
 * Abstract base class for all interactive GUI widgets in Journal-MC.
 *
 * Coordinates are defined in logical GUI units (independent of physical display resolution).
 * Handles visibility, enabled state, hover detection, focus management, input routing,
 * and tooltip rendering.
 */
public abstract class Widget {
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    protected boolean isVisible = true;
    protected boolean isEnabled = true;
    protected boolean isHovered = false;
    protected boolean isFocused = false;
    protected boolean isFocusable = true;

    protected String tooltipText = null;

    public Widget(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Checks if the logical mouse position lies within the widget's bounds.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isVisible && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Updates hover state and per-frame widget logic.
     */
    public void update(double mouseX, double mouseY) {
        if (!isVisible || !isEnabled) {
            isHovered = false;
            return;
        }
        isHovered = isMouseOver(mouseX, mouseY);
    }

    /**
     * Renders the widget. Must not draw if isVisible is false.
     */
    public abstract void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime);

    // --- Input Callbacks ---

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int action, int mods) {
        return false;
    }

    public boolean keyReleased(int keyCode, int scanCode, int action, int mods) {
        return false;
    }

    public boolean charTyped(char codePoint) {
        return false;
    }

    // --- Focus Management ---

    public void onFocusGained() {
        this.isFocused = true;
    }

    public void onFocusLost() {
        this.isFocused = false;
    }

    // --- Tooltip Rendering ---

    public void renderTooltip(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!isVisible || !isHovered || tooltipText == null || tooltipText.isEmpty()) {
            return;
        }

        float textW = font.getStringWidth(tooltipText, 0.50f);
        float boxW = textW + 10;
        float boxH = 15;

        float tipX = (float) mouseX + 8;
        float tipY = (float) mouseY - 14;

        if (tipX + boxW > screenWidth) {
            tipX = Math.max(2, screenWidth - boxW - 2);
        }
        if (tipY < 2) {
            tipY = (float) mouseY + 14;
        }

        // Minecraft style purple bevel tooltip box
        gui.drawBevelBox(tipX, tipY, boxW, boxH, 0x100010, 0x5000ff, 0x28007f);
        font.drawString(gui, tooltipText, tipX + 5, tipY + 3, 0.50f, 0xFFFFFF, true);
    }

    // --- Getters & Setters ---

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
        if (!visible) {
            isHovered = false;
            isFocused = false;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled) {
            isHovered = false;
            isFocused = false;
        }
    }

    public boolean isHovered() {
        return isHovered;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        if (this.isFocused != focused) {
            if (focused) onFocusGained(); else onFocusLost();
        }
    }

    public boolean isFocusable() {
        return isFocusable && isVisible && isEnabled;
    }

    public void setFocusable(boolean focusable) {
        this.isFocusable = focusable;
        if (!focusable) isFocused = false;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    public void setTooltip(String tooltipText) { setTooltipText(tooltipText); }

    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }
}
