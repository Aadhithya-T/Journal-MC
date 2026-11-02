package com.mcjournal.test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class FontInspector {
    public static void main(String[] args) throws Exception {
        Font prop = Font.createFont(Font.TRUETYPE_FONT, new File("engine/resources/fonts/minecraft-proportional.otf")).deriveFont(9.0f);
        BufferedImage img = new BufferedImage(120, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(prop);
        FontMetrics fm = g.getFontMetrics();
        System.out.println("Minecraft-Font 9.0px -> ascent=" + fm.getAscent() + ", descent=" + fm.getDescent() + ", height=" + fm.getHeight());
        for (char ch : new char[]{'A', 'E', 'i', 'l', '1', 'W', ' ', '.'}) {
            System.out.println("'" + ch + "': charWidth=" + fm.charWidth(ch));
        }

        g.setColor(Color.WHITE);
        g.drawString("Select World", 2, fm.getAscent() + 2);
        g.dispose();

        for (int y = 0; y < 14; y++) {
            StringBuilder sb = new StringBuilder();
            boolean has = false;
            for (int x = 0; x < 70; x++) {
                int a = (img.getRGB(x, y) >> 24) & 0xFF;
                if (a > 128) {
                    sb.append("#");
                    has = true;
                } else {
                    sb.append(".");
                }
            }
            if (has) System.out.println(String.format("%2d: %s", y, sb.toString()));
        }
    }
}
