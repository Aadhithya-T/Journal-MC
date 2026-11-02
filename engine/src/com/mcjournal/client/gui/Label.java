package com.mcjournal.client.gui;

/**
 * Non-interactive text label widget with alignment options, drop shadows,
 * and measurement caching integration.
 */
public class Label extends Widget {
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    private String text;
    private int color;
    private boolean shadow;
    private float scale;
    private Alignment alignment;

    public Label(String text, float x, float y, float width, float height,
                 int color, boolean shadow, float scale, Alignment alignment) {
        super(x, y, width, height);
        this.text = (text != null) ? text : "";
        this.color = color;
        this.shadow = shadow;
        this.scale = (scale > 0) ? scale : 0.50f;
        this.alignment = (alignment != null) ? alignment : Alignment.LEFT;
        this.isFocusable = false;
    }

    public Label(String text, float x, float y, int color) {
        this(text, x, y, 200, 20, color, true, 0.50f, Alignment.LEFT);
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (!isVisible || text == null || text.isEmpty()) return;

        float textW = font.getStringWidth(text, scale);
        float fontH = font.getFontHeight(scale);

        float renderX = x;
        if (alignment == Alignment.CENTER) {
            renderX = x + (width - textW) / 2.0f;
        } else if (alignment == Alignment.RIGHT) {
            renderX = x + width - textW;
        }

        float renderY = y + (height - fontH) / 2.0f;
        font.drawString(gui, text, renderX, renderY, scale, color, shadow);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = (text != null) ? text : "";
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }
}
