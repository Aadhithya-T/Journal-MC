package com.mcjournal.client.gui;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class FontRenderer {
    private static final int FONT_TEX_SIZE = 512;
    private static final int GLYPH_COUNT = 256;

    private int textureId;
    private final float[] glyphU0 = new float[GLYPH_COUNT];
    private final float[] glyphV0 = new float[GLYPH_COUNT];
    private final float[] glyphU1 = new float[GLYPH_COUNT];
    private final float[] glyphV1 = new float[GLYPH_COUNT];
    private final int[] glyphWidth = new int[GLYPH_COUNT];
    private final int[] glyphHeight = new int[GLYPH_COUNT];
    private int baseFontHeight = 20;

    public void init() {
        BufferedImage image = new BufferedImage(FONT_TEX_SIZE, FONT_TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Crisp pixel rendering without blurry subpixel antialiasing
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        Font font = null;
        try (InputStream is = FontRenderer.class.getResourceAsStream("/fonts/minecraft.ttf")) {
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                font = baseFont.deriveFont(Font.PLAIN, 20.0f);
                System.out.println("[FontRenderer] 🔤 Loaded authentic Minecraft TTF font: " + font.getFontName());
            }
        } catch (Exception e) {
            System.err.println("[FontRenderer] Notice: Using fallback bold font: " + e.getMessage());
        }

        if (font == null) {
            font = new Font("Dialog", Font.BOLD, 18);
        }

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        this.baseFontHeight = fm.getHeight();

        int x = 4;
        int y = fm.getAscent() + 4;
        int rowHeight = fm.getHeight() + 6;

        for (int c = 32; c < 127; c++) {
            char ch = (char) c;
            int charW = fm.charWidth(ch);
            int charH = fm.getHeight();

            if (x + charW + 6 >= FONT_TEX_SIZE) {
                x = 4;
                y += rowHeight;
            }

            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(ch), x, y);

            glyphU0[c] = (float) x / FONT_TEX_SIZE;
            glyphV0[c] = (float) (y - fm.getAscent()) / FONT_TEX_SIZE;
            glyphU1[c] = (float) (x + charW) / FONT_TEX_SIZE;
            glyphV1[c] = (float) (y - fm.getAscent() + charH) / FONT_TEX_SIZE;
            glyphWidth[c] = charW;
            glyphHeight[c] = charH;

            x += charW + 6;
        }
        g.dispose();

        // Upload to OpenGL
        int[] pixels = new int[FONT_TEX_SIZE * FONT_TEX_SIZE];
        image.getRGB(0, 0, FONT_TEX_SIZE, FONT_TEX_SIZE, pixels, 0, FONT_TEX_SIZE);

        ByteBuffer buffer = MemoryUtil.memAlloc(FONT_TEX_SIZE * FONT_TEX_SIZE * 4);
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int gCol = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            buffer.put((byte) r);
            buffer.put((byte) gCol);
            buffer.put((byte) b);
            buffer.put((byte) a);
        }
        buffer.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, FONT_TEX_SIZE, FONT_TEX_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, float scale, int colorHex, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        if (shadow) {
            // Authentic Minecraft Shadow: dark tinted copy 2px down-right
            int shadowColor = ((colorHex & 0xFCFCFC) >> 2) | 0x111111;
            renderText(gui, text, x + 2.0f * scale, y + 2.0f * scale, scale, shadowColor);
        }

        renderText(gui, text, x, y, scale, colorHex);
    }

    private void renderText(GuiRenderer gui, String text, float x, float y, float scale, int colorHex) {
        float curX = x;
        float r = ((colorHex >> 16) & 0xFF) / 255.0f;
        float g = ((colorHex >> 8) & 0xFF) / 255.0f;
        float b = (colorHex & 0xFF) / 255.0f;

        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c < 32 || c >= GLYPH_COUNT) c = 32;

            float w = glyphWidth[c] * scale;
            float h = glyphHeight[c] * scale;

            gui.drawTexturedQuad(
                textureId,
                curX, y, w, h,
                glyphU0[c], glyphV0[c], glyphU1[c], glyphV1[c],
                r, g, b, 1.0f
            );

            curX += w + (1.0f * scale);
        }
    }

    public float getStringWidth(String text, float scale) {
        if (text == null || text.isEmpty()) return 0;
        float total = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c < 32 || c >= GLYPH_COUNT) c = 32;
            total += (glyphWidth[c] * scale) + (1.0f * scale);
        }
        return total;
    }

    public float getFontHeight(float scale) {
        return (baseFontHeight > 0 ? baseFontHeight : 20.0f) * scale;
    }

    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }

    public int getTextureId() {
        return textureId;
    }
}
