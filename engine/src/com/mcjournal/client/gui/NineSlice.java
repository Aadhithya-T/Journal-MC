package com.mcjournal.client.gui;

/**
 * Reusable Nine-Slice renderer for scaling GUI backgrounds and borders without
 * distorting corners or texture aspect ratios.
 *
 * Integrates directly with GuiRenderer to maintain dynamic batching and zero state switches.
 */
public class NineSlice {
    private final int textureId;
    private final int texWidth;
    private final int texHeight;
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;

    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    public NineSlice(int textureId, int texWidth, int texHeight,
                     float u0, float v0, float u1, float v1,
                     float left, float top, float right, float bottom) {
        this.textureId = textureId;
        this.texWidth = Math.max(1, texWidth);
        this.texHeight = Math.max(1, texHeight);
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * Renders a 9-slice box at (x, y, w, h). If textureId is 0, renders a procedural beveled box.
     */
    public void render(GuiRenderer gui, float x, float y, float w, float h, float r, float g, float b, float a) {
        if (textureId == 0) {
            renderProcedural(gui, x, y, w, h, r, g, b, a);
            return;
        }

        // Clamp insets if box is smaller than corner sum
        float actualLeft = (left + right > w) ? (w * (left / (left + right))) : left;
        float actualRight = (left + right > w) ? (w * (right / (left + right))) : right;
        float actualTop = (top + bottom > h) ? (h * (top / (top + bottom))) : top;
        float actualBottom = (top + bottom > h) ? (h * (bottom / (top + bottom))) : bottom;

        float centerW = Math.max(0, w - actualLeft - actualRight);
        float centerH = Math.max(0, h - actualTop - actualBottom);

        float du = (u1 - u0);
        float dv = (v1 - v0);

        float uLeft = u0 + (left / texWidth) * du;
        float uRight = u1 - (right / texWidth) * du;
        float vTop = v0 + (top / texHeight) * dv;
        float vBottom = v1 - (bottom / texHeight) * dv;

        // 1. Four Corners (Never Stretched)
        // Top-Left
        gui.drawTexturedQuad(textureId, x, y, actualLeft, actualTop, u0, v0, uLeft, vTop, r, g, b, a);
        // Top-Right
        gui.drawTexturedQuad(textureId, x + w - actualRight, y, actualRight, actualTop, uRight, v0, u1, vTop, r, g, b, a);
        // Bottom-Left
        gui.drawTexturedQuad(textureId, x, y + h - actualBottom, actualLeft, actualBottom, u0, vBottom, uLeft, v1, r, g, b, a);
        // Bottom-Right
        gui.drawTexturedQuad(textureId, x + w - actualRight, y + h - actualBottom, actualRight, actualBottom, uRight, vBottom, u1, v1, r, g, b, a);

        // 2. Four Edges (Stretched in one dimension)
        if (centerW > 0) {
            // Top Edge
            gui.drawTexturedQuad(textureId, x + actualLeft, y, centerW, actualTop, uLeft, v0, uRight, vTop, r, g, b, a);
            // Bottom Edge
            gui.drawTexturedQuad(textureId, x + actualLeft, y + h - actualBottom, centerW, actualBottom, uLeft, vBottom, uRight, v1, r, g, b, a);
        }

        if (centerH > 0) {
            // Left Edge
            gui.drawTexturedQuad(textureId, x, y + actualTop, actualLeft, centerH, u0, vTop, uLeft, vBottom, r, g, b, a);
            // Right Edge
            gui.drawTexturedQuad(textureId, x + w - actualRight, y + actualTop, actualRight, centerH, uRight, vTop, u1, vBottom, r, g, b, a);
        }

        // 3. Center (Stretched in both dimensions)
        if (centerW > 0 && centerH > 0) {
            gui.drawTexturedQuad(textureId, x + actualLeft, y + actualTop, centerW, centerH, uLeft, vTop, uRight, vBottom, r, g, b, a);
        }
    }

    private void renderProcedural(GuiRenderer gui, float x, float y, float w, float h, float r, float g, float b, float a) {
        float border = Math.min(left, Math.min(w / 2.0f, h / 2.0f));

        // Center Fill
        gui.drawRect(x + border, y + border, w - border * 2, h - border * 2, r * 0.75f, g * 0.75f, b * 0.75f, a);

        // Top & Left Highlight Borders
        gui.drawRect(x, y, w, border, r * 1.2f, g * 1.2f, b * 1.2f, a);
        gui.drawRect(x, y + border, border, h - border * 2, r * 1.2f, g * 1.2f, b * 1.2f, a);

        // Bottom & Right Shadow Borders
        gui.drawRect(x, y + h - border, w, border, r * 0.4f, g * 0.4f, b * 0.4f, a);
        gui.drawRect(x + w - border, y + border, border, h - border * 2, r * 0.4f, g * 0.4f, b * 0.4f, a);
    }

    public int getTextureId() {
        return textureId;
    }

    public float getLeft() {
        return left;
    }

    public float getTop() {
        return top;
    }

    public float getRight() {
        return right;
    }

    public float getBottom() {
        return bottom;
    }
}
