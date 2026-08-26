package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class WorldCreateScreen extends Screen {
    private String worldName = "New Adventure";
    private final String gameMode = "Hardcore";
    private long seed = System.currentTimeMillis() % 1000000L;
    private float cursorTimer = 0.0f;

    public WorldCreateScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int totalBtnW = 420;
        int centerX = (width - totalBtnW) / 2;
        int btnW = (totalBtnW - 12) / 2; // 204px each
        int btnH = 40;
        int bottomY = height - 58;

        // 1. Create World Button (Left)
        buttons.add(new Button(1, "Create New World", centerX, bottomY, btnW, btnH, () -> {
            String finalName = worldName.trim().isEmpty() ? "New Adventure" : worldName.trim();
            app.enterWorld(seed, finalName, "Multi-Biome", null);
        }));

        // 2. Cancel Button (Right)
        buttons.add(new Button(2, "Cancel", centerX + btnW + 12, bottomY, btnW, btnH, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        cursorTimer += deltaTime * 2.0f;

        // 1. Dark translucent backdrop with header and footer strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.45f);
        gui.drawMenuHeaderFooterStrips(width, height, 64, 76);

        // 2. Header Title (Centered in top header strip)
        String title = "Create New World";
        float titleScale = 1.20f;
        float titleW = font.getStringWidth(title, titleScale);
        float titleH = font.getFontHeight(titleScale);
        font.drawString(gui, title, (width - titleW) / 2.0f, (64 - titleH) / 2.0f, titleScale, 0xffffff, true);

        // 3. Form Layout & Meaningful Vertical Justification
        int boxW = Math.min(520, width - 48);
        int centerX = (width - boxW) / 2;

        int headerH = 64;
        int footerH = 76;
        int availableH = height - headerH - footerH;

        // Section Dimensions
        int labelH = 16;
        int labelGap = 6;
        int cardH = 38;
        int sectionH = labelH + labelGap + cardH; // 60px per section
        int sectionGap = 24; // Generous breathing room between sections
        int totalFormH = sectionH * 4 + sectionGap * 3; // 4 sections = ~312px

        // Centered start position within the available middle region
        int startY = Math.max(headerH + 20, headerH + (availableH - totalFormH) / 2);

        // --- SECTION 1: World Name ---
        int s1Y = startY;
        font.drawString(gui, "World Name", centerX, s1Y, 0.82f, 0xaaaaaa, true);
        int nameBoxY = s1Y + labelH + labelGap;
        gui.drawRect(centerX - 1, nameBoxY - 1, boxW + 2, cardH + 2, 0.65f, 0.65f, 0.65f, 1.0f);
        gui.drawRect(centerX, nameBoxY, boxW, cardH, 0.0f, 0.0f, 0.0f, 1.0f);

        String displayName = worldName + ((int) cursorTimer % 2 == 0 ? "_" : "");
        float nameScale = 0.88f;
        float nameTextH = font.getFontHeight(nameScale);
        font.drawString(gui, displayName, centerX + 12, nameBoxY + (cardH - nameTextH) / 2.0f, nameScale, 0xffffff, false);

        // --- SECTION 2: Game Mode (Locked at Hardcore, no extra subtext) ---
        int s2Y = nameBoxY + cardH + sectionGap;
        font.drawString(gui, "Game Mode", centerX, s2Y, 0.82f, 0xaaaaaa, true);
        int modeBoxY = s2Y + labelH + labelGap;
        gui.drawBevelBox(centerX, modeBoxY, boxW, cardH, 0x18181c, 0x585860, 0x222226);
        float modeScale = 0.88f;
        float modeTextH = font.getFontHeight(modeScale);
        font.drawString(gui, "Game Mode: Hardcore", centerX + 14, modeBoxY + (cardH - modeTextH) / 2.0f, modeScale, 0xffffff, true);

        // --- SECTION 3: World Type ---
        int s3Y = modeBoxY + cardH + sectionGap;
        font.drawString(gui, "World Type", centerX, s3Y, 0.82f, 0xaaaaaa, true);
        int typeBoxY = s3Y + labelH + labelGap;
        gui.drawBevelBox(centerX, typeBoxY, boxW, cardH, 0x18181c, 0x585860, 0x222226);
        float typeScale = 0.88f;
        float typeTextH = font.getFontHeight(typeScale);
        font.drawString(gui, "World Type: Multi-Biome", centerX + 14, typeBoxY + (cardH - typeTextH) / 2.0f, typeScale, 0xdddddd, true);

        // --- SECTION 4: Seed (Seed number only) ---
        int s4Y = typeBoxY + cardH + sectionGap;
        font.drawString(gui, "Seed", centerX, s4Y, 0.82f, 0xaaaaaa, true);
        int seedBoxY = s4Y + labelH + labelGap;
        gui.drawBevelBox(centerX, seedBoxY, boxW, cardH, 0x141418, 0x484850, 0x1c1c20);
        float seedScale = 0.88f;
        float seedTextH = font.getFontHeight(seedScale);
        font.drawString(gui, "Seed: " + seed, centerX + 14, seedBoxY + (cardH - seedTextH) / 2.0f, seedScale, 0xcccccc, false);

        // 4. Render Action Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void charTyped(char character) {
        if (worldName.length() < 24 && character >= 32 && character <= 126) {
            worldName += character;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS || action == org.lwjgl.glfw.GLFW.GLFW_REPEAT) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !worldName.isEmpty()) {
                worldName = worldName.substring(0, worldName.length() - 1);
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                String finalName = worldName.trim().isEmpty() ? "New Adventure" : worldName.trim();
                app.enterWorld(seed, finalName, "Multi-Biome", null);
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                app.setScreen(new WorldSelectScreen(app));
            }
        }
    }
}
