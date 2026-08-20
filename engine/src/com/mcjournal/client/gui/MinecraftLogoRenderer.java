package com.mcjournal.client.gui;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class MinecraftLogoRenderer {
    private static final int LOGO_TEX_W = 512;
    private static final int LOGO_TEX_H = 160;

    private int textureId = 0;

    public void init() {
        if (textureId != 0) return;

        BufferedImage image = new BufferedImage(LOGO_TEX_W, LOGO_TEX_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Font mcFontLarge = null;
        Font mcFontSmall = null;

        try (InputStream is = MinecraftLogoRenderer.class.getResourceAsStream("/fonts/minecraft.ttf")) {
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                mcFontLarge = baseFont.deriveFont(Font.BOLD, 58.0f);
                mcFontSmall = baseFont.deriveFont(Font.BOLD, 18.0f);
            }
        } catch (Exception ignored) {}

        if (mcFontLarge == null) {
            mcFontLarge = new Font("Dialog", Font.BOLD, 54);
            mcFontSmall = new Font("Dialog", Font.BOLD, 16);
        }

        String title = "MINECRAFT";
        g.setFont(mcFontLarge);
        FontMetrics fm = g.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = (LOGO_TEX_W - titleW) / 2;
        int titleY = 66;

        // 1. Render 3D Extrusion Shadow for "MINECRAFT"
        int extrusionDepth = 7;
        for (int d = extrusionDepth; d >= 1; d--) {
            g.setColor(new Color(25, 25, 25, 255));
            g.drawString(title, titleX, titleY + d);
            g.drawString(title, titleX + d, titleY + d);
            g.drawString(title, titleX - 1, titleY + d);
            g.drawString(title, titleX + 1, titleY + d);
        }

        // Dark Outline
        g.setColor(new Color(10, 10, 10, 255));
        for (int ox = -2; ox <= 2; ox++) {
            for (int oy = -2; oy <= 2; oy++) {
                if (ox != 0 || oy != 0) {
                    g.drawString(title, titleX + ox, titleY + oy);
                }
            }
        }

        // Stone Front Face (Beveled Texture)
        g.setColor(new Color(155, 155, 155, 255));
        g.drawString(title, titleX, titleY);

        // Stone Highlight Top Edge
        g.setColor(new Color(215, 215, 215, 255));
        g.drawString(title, titleX, titleY - 1);

        // Stone Texture Inner Cracks & Highlights
        g.setColor(new Color(175, 175, 175, 255));
        g.drawString(title, titleX + 1, titleY);

        // 2. Render "JAVA EDITION" Banner Box below title
        String subTitle = "JAVA EDITION";
        g.setFont(mcFontSmall);
        FontMetrics fmSub = g.getFontMetrics();
        int subW = fmSub.stringWidth(subTitle);
        int badgeW = subW + 28;
        int badgeH = 26;
        int badgeX = (LOGO_TEX_W - badgeW) / 2;
        int badgeY = titleY + 12;

        // Badge 3D shadow
        g.setColor(new Color(15, 15, 15, 220));
        g.fillRect(badgeX + 3, badgeY + 3, badgeW, badgeH);

        // Badge dark body
        g.setColor(new Color(30, 30, 35, 245));
        g.fillRect(badgeX, badgeY, badgeW, badgeH);

        // Badge white/light highlight border
        g.setColor(new Color(255, 255, 255, 240));
        g.drawRect(badgeX, badgeY, badgeW, badgeH);
        g.setColor(new Color(100, 100, 100, 240));
        g.drawRect(badgeX + 1, badgeY + 1, badgeW - 2, badgeH - 2);

        // "JAVA EDITION" Text
        int subX = badgeX + (badgeW - subW) / 2;
        int subY = badgeY + fmSub.getAscent() + (badgeH - fmSub.getHeight()) / 2 + 1;

        g.setColor(new Color(15, 15, 15, 255));
        g.drawString(subTitle, subX + 2, subY + 2); // Shadow

        g.setColor(new Color(255, 255, 255, 255));
        g.drawString(subTitle, subX, subY);

        g.dispose();

        // 3. Upload to OpenGL Texture
        int[] pixels = new int[LOGO_TEX_W * LOGO_TEX_H];
        image.getRGB(0, 0, LOGO_TEX_W, LOGO_TEX_H, pixels, 0, LOGO_TEX_W);

        ByteBuffer buffer = MemoryUtil.memAlloc(LOGO_TEX_W * LOGO_TEX_H * 4);
        for (int argb : pixels) {
            byte a = (byte) ((argb >> 24) & 0xFF);
            byte r = (byte) ((argb >> 16) & 0xFF);
            byte gCol = (byte) ((argb >> 8) & 0xFF);
            byte b = (byte) (argb & 0xFF);

            buffer.put(r).put(gCol).put(b).put(a);
        }
        buffer.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, LOGO_TEX_W, LOGO_TEX_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void render(GuiRenderer gui, float x, float y, float width, float height) {
        if (textureId == 0) return;
        gui.drawTexturedQuad(textureId, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }
}
