package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class WorldCreateScreen extends Screen {
    private String worldName = "New Adventure";
    private String gameMode = "Survival";
    private String biome = "Plains";
    private long seed = System.currentTimeMillis() % 100000L;

    public WorldCreateScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 320;
        int btnH = 42;
        int centerX = (width - btnW) / 2;
        int startY = 120;

        // 1. Game Mode Toggle
        buttons.add(new Button(1, "Game Mode: " + gameMode, centerX, startY + 60, btnW, btnH, () -> {
            if (gameMode.equals("Survival")) gameMode = "Hardcore";
            else if (gameMode.equals("Hardcore")) gameMode = "Creative";
            else gameMode = "Survival";
            buttons.get(0).setText("Game Mode: " + gameMode);
        }));

        // 2. Biome Preset Toggle
        buttons.add(new Button(2, "Biome: " + biome, centerX, startY + 115, btnW, btnH, () -> {
            if (biome.equals("Plains")) biome = "Birch Forest";
            else if (biome.equals("Birch Forest")) biome = "Mountains";
            else if (biome.equals("Mountains")) biome = "River Valley";
            else biome = "Plains";
            buttons.get(1).setText("Biome: " + biome);
        }));

        // 3. Create World
        buttons.add(new Button(3, "Create New World", centerX, startY + 190, btnW, btnH, () -> {
            app.enterWorld(seed, worldName, biome);
        }));

        // 4. Cancel
        buttons.add(new Button(4, "Cancel", centerX, startY + 245, btnW, btnH, () -> {
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Dark slate background
        gui.drawRect(0, 0, width, height, 0.10f, 0.10f, 0.12f, 1.0f);

        // Header Title
        String title = "Create New World";
        float titleW = font.getStringWidth(title, 1.5f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 30, 1.5f, 0xffffff, true);

        // World Name Box
        int boxW = 320;
        int boxH = 40;
        int centerX = (width - boxW) / 2;
        int nameY = 120;

        font.drawString(gui, "World Name:", centerX, nameY - 20, 0.9f, 0xaaaaaa, false);
        gui.drawBevelBox(centerX, nameY, boxW, boxH, 0x000000, 0x555555, 0x222222);
        font.drawString(gui, worldName + "_", centerX + 12, nameY + 12, 1.0f, 0xffffff, false);

        // Seed info
        String seedInfo = "Seed: " + seed;
        float seedW = font.getStringWidth(seedInfo, 0.8f);
        font.drawString(gui, seedInfo, (width - seedW) / 2.0f, height - 30, 0.8f, 0x777777, false);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
