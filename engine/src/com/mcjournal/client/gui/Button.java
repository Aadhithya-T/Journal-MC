package com.mcjournal.client.gui;

public class Button {
    private final int id;
    private String text;
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean isHovered;
    private boolean isEnabled = true;
    private final Runnable onClick;

    public Button(int id, String text, float x, float y, float width, float height, Runnable onClick) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }

    public void update(double mouseX, double mouseY) {
        if (!isEnabled) {
            isHovered = false;
            return;
        }
        this.isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void render(GuiRenderer gui, FontRenderer font) {
        // Draw Authentic Vanilla Minecraft 9-Slice Stone Button
        gui.drawMinecraftButton(x, y, width, height, isHovered, isEnabled);

        float scale = height > 36 ? 1.05f : 0.92f;
        float textW = font.getStringWidth(text, scale);
        float textX = x + (width - textW) / 2.0f;
        float textY = y + (height - (18 * scale)) / 2.0f + 1;

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

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isEnabled && isHovered) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
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
