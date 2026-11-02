package com.mcjournal.client.gui;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Bitmap font renderer with integer scaling, Minecraft color formatting (§0-§f, §r),
 * full Latin-1 & special symbol coverage (°, ⚔, ▶, ♥), and LRU text-width measurement caching.
 */
public class FontRenderer {
    private static final int FONT_TEX_SIZE = 512;
    private static final int GLYPH_COUNT = 512;

    // Special mapped glyph slots
    public static final int SLOT_PLAY = 256;   // ▶ (U+25B6)
    public static final int SLOT_SWORD = 257;  // ⚔ (U+2694)
    public static final int SLOT_HEART = 258;  // ♥ (U+2665)
    public static final int SLOT_SKULL = 259;  // ☠ (U+2620)

    // Minecraft Standard 16 Color Table (§0 - §f)
    private static final int[] COLOR_CODE_TABLE = {
            0x000000, // 0: Black
            0x0000AA, // 1: Dark Blue
            0x00AA00, // 2: Dark Green
            0x00AAAA, // 3: Dark Aqua
            0xAA0000, // 4: Dark Red
            0xAA00AA, // 5: Dark Purple
            0xFFAA00, // 6: Gold
            0xAAAAAA, // 7: Gray
            0x555555, // 8: Dark Gray
            0x5555FF, // 9: Blue
            0x55FF55, // a: Green
            0x55FFFF, // b: Aqua
            0xFF5555, // c: Red
            0xFF55FF, // d: Light Purple
            0xFFFF55, // e: Yellow
            0xFFFFFF  // f: White
    };

    private int textureId;
    private final float[] glyphU0 = new float[GLYPH_COUNT];
    private final float[] glyphV0 = new float[GLYPH_COUNT];
    private final float[] glyphU1 = new float[GLYPH_COUNT];
    private final float[] glyphV1 = new float[GLYPH_COUNT];
    private final int[] glyphWidth = new int[GLYPH_COUNT];
    private final int[] glyphHeight = new int[GLYPH_COUNT];
    private int baseFontHeight = 20;

    // Fast bounded LRU measurement cache to eliminate per-frame String allocations
    private final Map<String, Float> widthCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) {
            return size() > 512;
        }
    };

    /**
     * Resolves a character to its glyph array slot index.
     */
    public static int getGlyphIndex(char ch) {
        if (ch == '▶' || ch == 9654) return SLOT_PLAY;
        if (ch == '⚔' || ch == 9876) return SLOT_SWORD;
        if (ch == '♥' || ch == 9829) return SLOT_HEART;
        if (ch == '☠' || ch == 9760) return SLOT_SKULL;
        if (ch >= 32 && ch < 256) return (int) ch;
        return 32; // fallback to space
    }

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

        // 1. Populate ASCII and Latin-1 supplement (32..255), including degree '°' (176) and section '§' (167)
        for (int c = 32; c < 256; c++) {
            char ch = (char) c;
            int charW = Math.max(1, fm.charWidth(ch));
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

        // 2. Populate Special Symbol Slots
        int[][] specialSlots = {
                {SLOT_PLAY, '▶'},
                {SLOT_SWORD, '⚔'},
                {SLOT_HEART, '♥'},
                {SLOT_SKULL, '☠'}
        };

        for (int[] slot : specialSlots) {
            int slotIdx = slot[0];
            char symbolChar = (char) slot[1];
            int charW = Math.max(fm.getHeight() / 2, fm.charWidth(symbolChar));
            int charH = fm.getHeight();

            if (x + charW + 6 >= FONT_TEX_SIZE) {
                x = 4;
                y += rowHeight;
            }

            g.setColor(Color.WHITE);
            if (font.canDisplay(symbolChar)) {
                g.drawString(String.valueOf(symbolChar), x, y);
            } else {
                // Procedural pixel-art fallback if font does not support glyph
                renderProceduralSymbol(g, slotIdx, x, y - fm.getAscent(), charW, charH);
            }

            glyphU0[slotIdx] = (float) x / FONT_TEX_SIZE;
            glyphV0[slotIdx] = (float) (y - fm.getAscent()) / FONT_TEX_SIZE;
            glyphU1[slotIdx] = (float) (x + charW) / FONT_TEX_SIZE;
            glyphV1[slotIdx] = (float) (y - fm.getAscent() + charH) / FONT_TEX_SIZE;
            glyphWidth[slotIdx] = charW;
            glyphHeight[slotIdx] = charH;

            x += charW + 6;
        }

        g.dispose();

        // Upload to OpenGL
        int[] pixels = new int[FONT_TEX_SIZE * FONT_TEX_SIZE];
        image.getRGB(0, 0, FONT_TEX_SIZE, FONT_TEX_SIZE, pixels, 0, FONT_TEX_SIZE);

        ByteBuffer buffer = MemoryUtil.memAlloc(FONT_TEX_SIZE * FONT_TEX_SIZE * 4);
        for (int argb : pixels) {
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

        widthCache.clear();
    }

    private void renderProceduralSymbol(Graphics2D g, int slot, int x, int y, int w, int h) {
        if (slot == SLOT_PLAY) {
            int[] px = {x + 2, x + w - 2, x + 2};
            int[] py = {y + 3, y + h / 2, y + h - 3};
            g.fillPolygon(px, py, 3);
        } else if (slot == SLOT_SWORD) {
            g.drawLine(x + 2, y + h - 3, x + w - 3, y + 2);
            g.drawLine(x + 3, y + h - 2, x + w - 2, y + 3);
            g.drawLine(x + 2, y + h - 6, x + 6, y + h - 2);
        } else if (slot == SLOT_HEART) {
            g.fillRect(x + 2, y + 4, w - 4, h - 8);
        } else {
            g.drawRect(x + 2, y + 2, w - 4, h - 4);
        }
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, float scale, int colorHex, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        if (shadow) {
            float shadowOffset = Math.max(1.0f, Math.round(1.0f * scale));
            renderText(gui, text, x + shadowOffset, y + shadowOffset, scale, colorHex, true);
        }

        renderText(gui, text, x, y, scale, colorHex, false);
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, int scale, int colorHex, boolean shadow) {
        drawString(gui, text, x, y, (float) scale, colorHex, shadow);
    }

    private void renderText(GuiRenderer gui, String text, float x, float y, float scale, int defaultColor, boolean isShadow) {
        float curX = x;
        int activeColor = defaultColor;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Handle Minecraft Formatting Codes (§0-§f, §r)
            if (ch == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code >= '0' && code <= '9') {
                    activeColor = COLOR_CODE_TABLE[code - '0'];
                } else if (code >= 'a' && code <= 'f') {
                    activeColor = COLOR_CODE_TABLE[code - 'a' + 10];
                } else if (code == 'r') {
                    activeColor = defaultColor;
                }
                i++; // Skip format code character
                continue;
            }

            int c = getGlyphIndex(ch);

            float w = glyphWidth[c] * scale;
            float h = glyphHeight[c] * scale;

            int effectiveColor;
            if (isShadow) {
                // Authentic Minecraft Shadow: 25% brightness of the active text color
                effectiveColor = ((activeColor & 0xFCFCFC) >> 2) | 0x111111;
            } else {
                effectiveColor = activeColor;
            }

            float r = ((effectiveColor >> 16) & 0xFF) / 255.0f;
            float g = ((effectiveColor >> 8) & 0xFF) / 255.0f;
            float b = (effectiveColor & 0xFF) / 255.0f;

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

        String cacheKey = text + "@" + scale;
        Float cached = widthCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        float total = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Color formatting codes do not contribute to string width
            if (ch == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }

            int c = getGlyphIndex(ch);
            total += (glyphWidth[c] * scale) + (1.0f * scale);
        }

        widthCache.put(cacheKey, total);
        return total;
    }

    public float getStringWidth(String text, int scale) {
        return getStringWidth(text, (float) scale);
    }

    public float getFontHeight(float scale) {
        return (baseFontHeight > 0 ? baseFontHeight : 20.0f) * scale;
    }

    public float getFontHeight(int scale) {
        return getFontHeight((float) scale);
    }

    /**
     * Strips all Minecraft color formatting codes (§.) from the string.
     */
    public static String stripFormatting(String text) {
        if (text == null) return null;
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        widthCache.clear();
    }

    public int getTextureId() {
        return textureId;
    }
}
