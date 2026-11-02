package com.mcjournal.test;

import java.awt.Font;
import java.io.File;

public class FontInspector {
    public static void main(String[] args) throws Exception {
        Font mono = Font.createFont(Font.TRUETYPE_FONT, new File("engine/resources/fonts/minecraft.ttf"));
        Font prop = Font.createFont(Font.TRUETYPE_FONT, new File("engine/resources/fonts/minecraft-proportional.otf"));
        for (char ch : new char[]{'▶', '♥', '⚔', '☠', '·', '×'}) {
            System.out.println("char '" + ch + "' (U+" + Integer.toHexString(ch) + "): mono=" + mono.canDisplay(ch) + ", prop=" + prop.canDisplay(ch));
        }
    }
}
