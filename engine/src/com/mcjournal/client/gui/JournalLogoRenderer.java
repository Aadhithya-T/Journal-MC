package com.mcjournal.client.gui;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Original logo renderer for Journal-MC.
 * Generates an original stylized 3D voxel logo ("JOURNAL MC") with stone bevels,
 * golden highlights, and a "VOXEL ENGINE" sub-badge.
 * Completely free of Minecraft logos, trademarks, and copyright assets.
 */
public class JournalLogoRenderer {
    private static final int LOGO_TEX_W = 512;
    private static final int LOGO_TEX_H = 160;

    private int textureId = 0;

    public void init() {
        if (textureId != 0) return;
        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable noGlContext) {
            return;
        }

        BufferedImage image = new BufferedImage(LOGO_TEX_W, LOGO_TEX_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Font logoFont = null;
        try (InputStream is = JournalLogoRenderer.class.getResourceAsStream("/fonts/Monocraft.ttf")) {
            if (is != null) {
                Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
                logoFont = baseFont.deriveFont(Font.BOLD, 52.0f);
            }
        } catch (Exception ignored) {}

        if (logoFont == null) {
            logoFont = new Font("Dialog", Font.BOLD, 48);
        }

        String title = "JOURNAL MC";
        g.setFont(logoFont);
        FontMetrics fm = g.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = (LOGO_TEX_W - titleW) / 2;
        int titleY = 64;

        // 1. 3D Extrusion Shadow for "JOURNAL MC"
        int extrusionDepth = 6;
        for (int d = extrusionDepth; d >= 1; d--) {
            g.setColor(new Color(18, 18, 22, 255));
            g.drawString(title, titleX, titleY + d);
            g.drawString(title, titleX + d, titleY + d);
            g.drawString(title, titleX - 1, titleY + d);
            g.drawString(title, titleX + 1, titleY + d);
        }

        // Dark Outline
        g.setColor(new Color(8, 8, 12, 255));
        for (int ox = -2; ox <= 2; ox++) {
            for (int oy = -2; oy <= 2; oy++) {
                if (ox != 0 || oy != 0) {
                    g.drawString(title, titleX + ox, titleY + oy);
                }
            }
        }

        // Stone Base Body
        g.setColor(new Color(130, 134, 142, 255));
        g.drawString(title, titleX, titleY);

        // Warm Golden/Amber Top Bevel
        g.setColor(new Color(225, 185, 80, 255));
        g.drawString(title, titleX, titleY - 1);

        // Stone Texture Inner Highlights
        g.setColor(new Color(160, 165, 175, 255));
        g.drawString(title, titleX + 1, titleY);

        // 2. Render "VOXEL ENGINE" Subtitle Badge below title
        String subTitle = "VOXEL ENGINE";
        Font subFont = logoFont.deriveFont(Font.BOLD, 15.0f);
        g.setFont(subFont);
        FontMetrics fmSub = g.getFontMetrics();
        int subW = fmSub.stringWidth(subTitle);
        int badgeW = subW + 24;
        int badgeH = 22;
        int badgeX = (LOGO_TEX_W - badgeW) / 2;
        int badgeY = titleY + 12;

        // Badge 3D shadow
        g.setColor(new Color(10, 10, 14, 220));
        g.fillRect(badgeX + 2, badgeY + 2, badgeW, badgeH);

        // Badge dark body
        g.setColor(new Color(24, 24, 30, 245));
        g.fillRect(badgeX, badgeY, badgeW, badgeH);

        // Badge golden border
        g.setColor(new Color(210, 170, 70, 240));
        g.drawRect(badgeX, badgeY, badgeW, badgeH);
        g.setColor(new Color(90, 75, 30, 240));
        g.drawRect(badgeX + 1, badgeY + 1, badgeW - 2, badgeH - 2);

        // "VOXEL ENGINE" Text
        int subX = badgeX + (badgeW - subW) / 2;
        int subY = badgeY + fmSub.getAscent() + (badgeH - fmSub.getHeight()) / 2;

        g.setColor(new Color(10, 10, 14, 255));
        g.drawString(subTitle, subX + 1, subY + 1); // Shadow

        g.setColor(new Color(240, 240, 245, 255));
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
