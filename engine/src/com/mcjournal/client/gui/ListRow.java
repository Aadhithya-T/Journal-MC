package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

/**
 * Reusable row item widget for selection and scrollable list displays
 * (e.g., world selection and options lists).
 */
public class ListRow extends Widget {
    private final int index;
    private String primaryText;
    private String secondaryText;
    private boolean isSelected = false;
    private final Runnable onClick;

    public ListRow(int index, String primaryText, String secondaryText,
                   float x, float y, float width, float height, Runnable onClick) {
        super(x, y, width, height);
        this.index = index;
        this.primaryText = (primaryText != null) ? primaryText : "";
        this.secondaryText = (secondaryText != null) ? secondaryText : "";
        this.onClick = onClick;
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (!isVisible) return;

        // Selection / Hover Background
        if (isSelected) {
            gui.drawRect(x, y, width, height, 0.15f, 0.15f, 0.20f, 0.90f);
            gui.drawRect(x, y, width, 1, 1.0f, 1.0f, 1.0f, 0.70f);
            gui.drawRect(x, y + height - 1, width, 1, 1.0f, 1.0f, 1.0f, 0.70f);
        } else if (isHovered) {
            gui.drawRect(x, y, width, height, 0.10f, 0.10f, 0.10f, 0.60f);
        }

        // Focus indicator
        if (isFocused && isEnabled) {
            gui.drawRect(x + 1, y + 1, 2, height - 2, 1.0f, 1.0f, 1.0f, 0.90f);
        }

        // Text Rendering
        float primaryScale = 0.55f;
        float fontH = font.getFontHeight(primaryScale);
        float primaryY = (secondaryText.isEmpty()) ? y + (height - fontH) / 2.0f : y + 3.0f;

        int textColor = isSelected ? 0xFFFFA0 : (isHovered ? 0xFFFFFF : 0xDDDDDD);
        font.drawString(gui, primaryText, x + 8.0f, primaryY, primaryScale, textColor, true);

        if (!secondaryText.isEmpty()) {
            float secScale = 0.45f;
            font.drawString(gui, secondaryText, x + 8.0f, y + height - font.getFontHeight(secScale) - 3.0f,
                    secScale, 0x888888, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isEnabled && isHovered) {
            if (onClick != null) {
                onClick.run();
            }
            return true;
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

    public int getIndex() {
        return index;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = (primaryText != null) ? primaryText : "";
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = (secondaryText != null) ? secondaryText : "";
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}
