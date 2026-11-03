package com.mcjournal.client.gui;

/**
 * Standardized typography styles for Journal-MC GUI screens.
 * Uses integer scales (1x, 2x, etc.) on the native pixel-grid font.
 */
public final class TextStyle {

    // TITLE: 2x scale, white, with drop shadow
    public static final TextStyle TITLE = new TextStyle(2, 0xFFFFFF, true);

    // BODY: 1x scale, white, with drop shadow
    public static final TextStyle BODY = new TextStyle(1, 0xFFFFFF, true);

    // CAPTION: 1x scale, muted gray (0x888888), flat (no shadow)
    public static final TextStyle CAPTION = new TextStyle(1, 0x888888, false);

    private final int scale;
    private final int colorHex;
    private final boolean shadow;

    public TextStyle(int scale, int colorHex, boolean shadow) {
        if (scale < 1) {
            throw new IllegalArgumentException("TextStyle scale must be >= 1, was: " + scale);
        }
        this.scale = scale;
        this.colorHex = colorHex;
        this.shadow = shadow;
    }

    public int getScale() {
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

    public TextStyle withScale(int newScale) {
        return new TextStyle(newScale, this.colorHex, this.shadow);
    }
}
