package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

/**
 * Standard interactive button widget with 9-slice rendering, keyboard activation,
 * focus highlighting, and duplicate activation prevention.
 */
public class Button extends Widget {
    protected final int id;
    protected String text;
    protected final Runnable onClick;

    private boolean isPressed = false;
    private boolean activatedOnPress = false;

    public Button(int id, String text, float x, float y, float width, float height, Runnable onClick) {
        super(x, y, width, height);
        this.id = id;
        this.text = text;
        this.onClick = onClick;
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        render(gui, font);
    }

    /**
     * Backward-compatible 2-argument render method.
     */
    public void render(GuiRenderer gui, FontRenderer font) {
        if (!isVisible) return;

        // Draw Authentic Vanilla Minecraft 9-Slice Stone Button
        gui.drawMinecraftButton(x, y, width, height, isHovered, isEnabled);

        // Focus Highlight Border (1px crisp outline)
        if (isFocused && isEnabled) {
            gui.drawRect(x - 1, y - 1, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y + height, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x + width, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
        }

        float maxAllowedW = width - 14.0f;
        float desiredScale = height >= 38 ? 0.88f : (height >= 28 ? 0.65f : 0.50f);
        float textW = font.getStringWidth(text, desiredScale);
        float scale = desiredScale;
        if (textW > maxAllowedW && textW > 0) {
            scale = desiredScale * (maxAllowedW / textW);
            textW = font.getStringWidth(text, scale);
        }

        float textX = x + (width - textW) / 2.0f;
        float fontH = font.getFontHeight(scale);
        float textY = y + (height - fontH) / 2.0f;

        int textColor;
        if (!isEnabled) {
            textColor = 0xa0a0a0;
        } else if (isHovered) {
            textColor = 0xffffa0; // Minecraft yellow hover
        } else {
            textColor = 0xe0e0e0; // Minecraft crisp light gray
        }

        font.drawString(gui, text, textX, textY, scale, textColor, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isEnabled && isHovered) {
            isPressed = true;
            activatedOnPress = true;
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            boolean wasPressed = isPressed;
            isPressed = false;
            // Prevent duplicate activation if already executed on press
            if (activatedOnPress) {
                activatedOnPress = false;
                return wasPressed;
            }
            if (wasPressed && isEnabled && isMouseOver(mouseX, mouseY)) {
                if (onClick != null) {
                    onClick.run();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (isFocused && isEnabled && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                if (onClick != null) {
                    onClick.run();
                }
                return true;
            }
        }
        return false;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
