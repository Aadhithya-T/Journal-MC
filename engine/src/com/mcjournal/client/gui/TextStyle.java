package com.mcjournal.client.gui;

/**
 * Standard typography styles for Journal-MC GUI screens.
 * Centralizes scale, default color, and shadow parameters, replacing ad-hoc fractional literals.
 */
public final class TextStyle {
    // Initial mapping matching current fractional scales:
    // TITLE: 0.80f
    // BODY: 0.55f
    // CAPTION: 0.48f
    public static final TextStyle TITLE = new TextStyle(0.80f, 0xFFFFFF, true);
    public static final TextStyle BODY = new TextStyle(0.55f, 0xFFFFFF, true);
    public static final TextStyle CAPTION = new TextStyle(0.48f, 0x888888, false);

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
