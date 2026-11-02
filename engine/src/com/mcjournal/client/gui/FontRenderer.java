package com.mcjournal.client.gui;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Bitmap font renderer with pluggable font profiles, native pixel-grid rasterization,
 * integer scaling (1x, 2x), color formatting (§0-§f, §r), and full Latin-1 & special symbol coverage.
 */
public class FontRenderer {
    private static final int FONT_TEX_SIZE = 512;
    private static final int GLYPH_COUNT = 512;

    // Special mapped glyph slots
    public static final int SLOT_PLAY = 256;   // ▶ (U+25B6)
    public static final int SLOT_SWORD = 257;  // ⚔ (U+2694)
    public static final int SLOT_HEART = 258;  // ♥ (U+2665)
    public static final int SLOT_SKULL = 259;  // ☠ (U+2620)

    // Standard 16 Color Table (§0 - §f)
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

    private static final Set<Character> WARNED_CHARS = Collections.synchronizedSet(new HashSet<>());

    /**
     * Pluggable font configuration describing file location, native bake size,
     * OpenType metrics, and license information.
     */
    public static class FontConfig {
        private final String name;
        private final String resourcePath;
        private final float bakeSize;
        private final int unitsPerEm;
        private final boolean isMonospaced;
        private final String license;
        private final String author;

        public FontConfig(String name, String resourcePath, float bakeSize, int unitsPerEm,
                          boolean isMonospaced, String license, String author) {
            this.name = name;
            this.resourcePath = resourcePath;
            this.bakeSize = bakeSize;
            this.unitsPerEm = unitsPerEm;
            this.isMonospaced = isMonospaced;
            this.license = license;
            this.author = author;
        }

        public String getName() { return name; }
        public String getResourcePath() { return resourcePath; }
        public float getBakeSize() { return bakeSize; }
        public int getUnitsPerEm() { return unitsPerEm; }
        public boolean isMonospaced() { return isMonospaced; }
        public String getLicense() { return license; }
        public String getAuthor() { return author; }
    }

    public static final FontConfig MONOCRAFT = new FontConfig(
            "Monocraft",
            "/fonts/minecraft.ttf",
            9.0f,
            1080,
            true,
            "SIL Open Font License 1.1",
            "Idrees Hassan (IdreesInc)"
    );

    public static final FontConfig MINECRAFT_PROPORTIONAL = new FontConfig(
            "Minecraft-Font",
            "/fonts/minecraft-proportional.otf",
            8.0f,
            18,
            false,
            "SIL Open Font License 1.1",
            "Idrees Hassan (IdreesInc)"
    );

    /**
     * Default active font profile.
     * To switch between fonts across the entire application, change this single configuration line:
     * e.g., FontRenderer.ACTIVE_CONFIG = FontRenderer.MONOCRAFT;
     *   or: FontRenderer.ACTIVE_CONFIG = FontRenderer.MINECRAFT_PROPORTIONAL;
     */
    public static FontConfig ACTIVE_CONFIG = MONOCRAFT;

    private final FontConfig config;
    private int textureId;
    private final float[] glyphU0 = new float[GLYPH_COUNT];
    private final float[] glyphV0 = new float[GLYPH_COUNT];
    private final float[] glyphU1 = new float[GLYPH_COUNT];
    private final float[] glyphV1 = new float[GLYPH_COUNT];
    private final int[] glyphWidth = new int[GLYPH_COUNT];
    private final int[] glyphHeight = new int[GLYPH_COUNT];

    private int baseAscent = 8;
    private int baseCapHeight = 7;
    private int baseLineHeight = 10;

    public FontRenderer() {
        this(ACTIVE_CONFIG);
    }

    public FontRenderer(FontConfig config) {
        this.config = (config != null) ? config : ACTIVE_CONFIG;
        init();
    }

    public FontConfig getConfig() {
        return config;
    }

    /**
     * Resolves a character to its glyph array slot index.
     * Logs a warning when an unmapped char above 255 falls back to space.
     */
    public static int getGlyphIndex(char ch) {
        if (ch == '▶' || ch == 9654) return SLOT_PLAY;
        if (ch == '⚔' || ch == 9876) return SLOT_SWORD;
        if (ch == '♥' || ch == 9829) return SLOT_HEART;
        if (ch == '☠' || ch == 9760) return SLOT_SKULL;
        if (ch >= 32 && ch < 256) return (int) ch;

        if (ch > 255 && WARNED_CHARS.add(ch)) {
            System.err.println("[FontRenderer] Warning: Character U+" + Integer.toHexString(ch).toUpperCase() + " ('" + ch + "') above 255 falls back to space");
        }
        return 32; // fallback to space
    }

    public void init() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        BufferedImage image = new BufferedImage(FONT_TEX_SIZE, FONT_TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Crisp pixel rendering without blurry subpixel antialiasing
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        Font font = null;
        try (InputStream is = FontRenderer.class.getResourceAsStream(config.getResourcePath())) {
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                font = baseFont.deriveFont(Font.PLAIN, config.getBakeSize());
                System.out.println("[FontRenderer] Loaded TTF font: " + font.getFontName() + " (" + config.getName() + " at " + config.getBakeSize() + "px)");
            } else {
                System.err.println("[FontRenderer] Warning: Font resource not found at " + config.getResourcePath() + ", using fallback");
            }
        } catch (Exception e) {
            System.err.println("[FontRenderer] Notice: Error loading font (" + e.getMessage() + "), using fallback");
        }

        if (font == null) {
            font = new Font("Monospaced", Font.PLAIN, Math.round(config.getBakeSize()));
        }

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        this.baseAscent = fm.getAscent();
        this.baseLineHeight = fm.getHeight();

        FontRenderContext frc = g.getFontRenderContext();
        try {
            GlyphVector gv = font.createGlyphVector(frc, "H");
            int ch = (int) Math.round(gv.getPixelBounds(null, 0, 0).getHeight());
            this.baseCapHeight = (ch > 0) ? ch : Math.max(1, Math.round(baseAscent * 0.75f));
        } catch (Throwable t) {
            this.baseCapHeight = Math.max(1, Math.round(baseAscent * 0.75f));
        }

        int x = 4;
        int y = fm.getAscent() + 4;
        int rowHeight = fm.getHeight() + 4;

        // 1. Populate ASCII and Latin-1 supplement (32..255)
        for (int c = 32; c < 256; c++) {
            char ch = (char) c;
            int charW = Math.max(1, fm.charWidth(ch));
            int charH = fm.getHeight();

            if (x + charW + 4 >= FONT_TEX_SIZE) {
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

            x += charW + 4;
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

            if (x + charW + 4 >= FONT_TEX_SIZE) {
                x = 4;
                y += rowHeight;
            }

            g.setColor(Color.WHITE);
            if (font.canDisplay(symbolChar)) {
                g.drawString(String.valueOf(symbolChar), x, y);
            } else {
                renderProceduralSymbol(g, slotIdx, x, y - fm.getAscent(), charW, charH);
            }

            glyphU0[slotIdx] = (float) x / FONT_TEX_SIZE;
            glyphV0[slotIdx] = (float) (y - fm.getAscent()) / FONT_TEX_SIZE;
            glyphU1[slotIdx] = (float) (x + charW) / FONT_TEX_SIZE;
            glyphV1[slotIdx] = (float) (y - fm.getAscent() + charH) / FONT_TEX_SIZE;
            glyphWidth[slotIdx] = charW;
            glyphHeight[slotIdx] = charH;

            x += charW + 4;
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
    }

    private void renderProceduralSymbol(Graphics2D g, int slot, int x, int y, int w, int h) {
        if (slot == SLOT_PLAY) {
            int[] px = {x + 1, x + w - 1, x + 1};
            int[] py = {y + 2, y + h / 2, y + h - 2};
            g.fillPolygon(px, py, 3);
        } else if (slot == SLOT_SWORD) {
            g.drawLine(x + 1, y + h - 2, x + w - 2, y + 1);
            g.drawLine(x + 2, y + h - 1, x + w - 1, y + 2);
            g.drawLine(x + 1, y + h - 4, x + 4, y + h - 1);
        } else if (slot == SLOT_HEART) {
            g.fillRect(x + 1, y + 2, w - 2, h - 4);
        } else {
            g.drawRect(x + 1, y + 1, w - 2, h - 2);
        }
    }

    // --- Typography Metric Accessors ---

    public float getAscent(float scale) {
        int intScale = Math.max(1, Math.round(scale));
        return (baseAscent > 0 ? baseAscent : 8.0f) * intScale;
    }

    public float getAscent(TextStyle style) {
        return getAscent(style != null ? style.getScale() : 1.0f);
    }

    public float getCapHeight(float scale) {
        int intScale = Math.max(1, Math.round(scale));
        return (baseCapHeight > 0 ? baseCapHeight : 7.0f) * intScale;
    }

    public float getCapHeight(TextStyle style) {
        return getCapHeight(style != null ? style.getScale() : 1.0f);
    }

    public float getLineHeight(float scale) {
        int intScale = Math.max(1, Math.round(scale));
        return (baseLineHeight > 0 ? baseLineHeight : 10.0f) * intScale;
    }

    public float getLineHeight(TextStyle style) {
        return getLineHeight(style != null ? style.getScale() : 1.0f);
    }

    public float getFontHeight(float scale) {
        return getLineHeight(scale);
    }

    public float getFontHeight(int scale) {
        return getLineHeight((float) scale);
    }

    // --- Measurement API ---

    public float width(String text, TextStyle style) {
        return width(text, style != null ? style.getScale() : 1.0f);
    }

    public float width(String text, float scale) {
        if (text == null || text.isEmpty()) return 0.0f;

        int intScale = Math.max(1, Math.round(scale));
        int totalWidth = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Formatting codes (§.) do not occupy visual width
            if (ch == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }

            int c = getGlyphIndex(ch);
            totalWidth += glyphWidth[c] * intScale;
        }

        return (float) totalWidth;
    }

    public float getStringWidth(String text, float scale) {
        return width(text, scale);
    }

    public float getStringWidth(String text, int scale) {
        return width(text, (float) scale);
    }

    // --- Drawing API (snapped to whole integer coordinates) ---

    public void drawString(GuiRenderer gui, String text, float x, float y, TextStyle style) {
        if (style == null) {
            drawString(gui, text, x, y, 1.0f, 0xFFFFFF, true);
        } else {
            drawString(gui, text, x, y, style.getScale(), style.getColorHex(), style.hasShadow());
        }
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, TextStyle style, int colorHex) {
        if (style == null) {
            drawString(gui, text, x, y, 1.0f, colorHex, true);
        } else {
            drawString(gui, text, x, y, style.getScale(), colorHex, style.hasShadow());
        }
    }

    public void drawCentered(GuiRenderer gui, String text, float centerX, float y, TextStyle style) {
        if (text == null || text.isEmpty()) return;
        int intScale = style != null ? Math.max(1, Math.round(style.getScale())) : 1;
        float w = width(text, intScale);
        int drawX = Math.round(centerX - (w / 2.0f));
        int drawY = Math.round(y);
        drawString(gui, text, drawX, drawY, intScale, style != null ? style.getColorHex() : 0xFFFFFF, style != null && style.hasShadow());
    }

    public void drawCentered(GuiRenderer gui, String text, float centerX, float y, TextStyle style, int colorHex) {
        if (text == null || text.isEmpty()) return;
        int intScale = style != null ? Math.max(1, Math.round(style.getScale())) : 1;
        float w = width(text, intScale);
        int drawX = Math.round(centerX - (w / 2.0f));
        int drawY = Math.round(y);
        drawString(gui, text, drawX, drawY, intScale, colorHex, style != null && style.hasShadow());
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, float scale, int colorHex, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        int intScale = Math.max(1, Math.round(scale));
        int drawX = Math.round(x);
        int drawY = Math.round(y);

        if (shadow) {
            int shadowOffset = intScale;
            renderText(gui, text, drawX + shadowOffset, drawY + shadowOffset, intScale, colorHex, true);
        }

        renderText(gui, text, drawX, drawY, intScale, colorHex, false);
    }

    public void drawString(GuiRenderer gui, String text, float x, float y, int scale, int colorHex, boolean shadow) {
        drawString(gui, text, x, y, (float) scale, colorHex, shadow);
    }

    private void renderText(GuiRenderer gui, String text, int x, int y, int intScale, int defaultColor, boolean isShadow) {
        int curX = x;
        int activeColor = defaultColor;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Handle Formatting Codes (§0-§f, §r)
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
            int w = glyphWidth[c] * intScale;
            int h = glyphHeight[c] * intScale;

            int effectiveColor;
            if (isShadow) {
                // Shadow: 25% brightness of the active text color
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

            curX += w;
        }
    }

    // --- Ellipsize Utility ---

    public String ellipsize(String text, float maxWidth, TextStyle style) {
        return ellipsize(text, maxWidth, style != null ? style.getScale() : 1.0f);
    }

    public String ellipsize(String text, float maxWidth, float scale) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) return "";
        if (width(text, scale) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        float ellWidth = width(ellipsis, scale);
        if (ellWidth > maxWidth) {
            return ".";
        }

        int len = text.length();
        while (len > 0) {
            String candidate = text.substring(0, len) + ellipsis;
            if (width(candidate, scale) <= maxWidth) {
                return candidate;
            }
            len--;
        }
        return ellipsis;
    }

    /**
     * Strips all color formatting codes (§.) from the string.
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
    }

    public int getTextureId() {
        return textureId;
    }
}
