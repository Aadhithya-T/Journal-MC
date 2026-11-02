package com.mcjournal.client.gui;

/**
 * Standardized typography styles for Journal-MC GUI screens.
 * Uses integer scales (1x, 2x) on the native pixel-grid font.
 */
public final class TextStyle {

    // TITLE: 2x scale, white, with drop shadow
    public static final TextStyle TITLE = new TextStyle(2.0f, 0xFFFFFF, true);

    // BODY: 1x scale, white, with drop shadow
    public static final TextStyle BODY = new TextStyle(1.0f, 0xFFFFFF, true);

    // CAPTION: 1x scale, muted gray (0x888888), flat (no shadow)
    public static final TextStyle CAPTION = new TextStyle(1.0f, 0x888888, false);

    private final float scale;
    private final int colorHex;
    private final boolean shadow;

    public TextStyle(float scale, int colorHex, boolean shadow) {
        this.scale = scale;
        this.colorHex = colorHex;
        this.shadow = shadow;
    }

    public float getScale() {
        return scale;
    }

    public int getColorHex() {
        return colorHex;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public TextStyle withColor(int newColorHex) {
        return new TextStyle(this.scale, newColorHex, this.shadow);
    }

    public TextStyle withShadow(boolean newShadow) {
        return new TextStyle(this.scale, this.colorHex, newShadow);
    }

    public TextStyle withScale(float newScale) {
        return new TextStyle(newScale, this.colorHex, this.shadow);
    }
}
