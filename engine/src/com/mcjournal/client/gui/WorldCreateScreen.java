package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class WorldCreateScreen extends Screen {
    private String worldName = "New Adventure";
    private final String gameMode = "Hardcore"; // Hardcore ONLY
    private String biome = "Plains";
    private long seed = System.currentTimeMillis() % 100000L;

    public WorldCreateScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 340;
        int btnH = 42;
        int centerX = (width - btnW) / 2;
        int startY = 120;

        // 1. Biome Preset Toggle
        buttons.add(new Button(1, "Biome: " + biome, centerX, startY + 130, btnW, btnH, () -> {
            if (biome.equals("Plains")) biome = "Birch Forest";
            else if (biome.equals("Birch Forest")) biome = "Mountains";
            else if (biome.equals("Mountains")) biome = "River Valley";
            else biome = "Plains";
            buttons.get(0).setText("Biome: " + biome);
        }));

        // 2. Create World Button
        buttons.add(new Button(2, "Create Hardcore World", centerX, startY + 195, btnW, btnH, () -> {
            com.mcjournal.client.WorldSaveManager.saveWorld(worldName, biome, seed);
            app.enterWorld(seed, worldName, biome);
        }));

        // 3. Cancel Button
        buttons.add(new Button(3, "Cancel", centerX, startY + 248, btnW, btnH, () -> {
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Dark slate background
        gui.drawRect(0, 0, width, height, 0.10f, 0.10f, 0.12f, 1.0f);

        // Header Title
        String title = "Create Hardcore World";
        float titleW = font.getStringWidth(title, 1.5f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 30, 1.5f, 0xff5555, true);

        // World Name Box
        int boxW = 340;
        int boxH = 40;
        int centerX = (width - boxW) / 2;
        int nameY = 110;

        font.drawString(gui, "World Name:", centerX, nameY - 20, 0.9f, 0xaaaaaa, false);
        gui.drawBevelBox(centerX, nameY, boxW, boxH, 0x000000, 0x555555, 0x222222);
        font.drawString(gui, worldName + "_", centerX + 12, nameY + 12, 1.0f, 0xffffff, false);

        // Game Mode Display Box (Hardcore Locked)
        int modeY = nameY + 54;
        gui.drawBevelBox(centerX, modeY, boxW, 46, 0x2b1010, 0xaa2222, 0x550a0a);
        font.drawString(gui, "⚔ Game Mode: HARDCORE", centerX + 14, modeY + 10, 1.0f, 0xff5555, true);
        font.drawString(gui, "Locked: Permadeath & authentic survival challenge", centerX + 14, modeY + 28, 0.75f, 0xdd8888, false);

        // Seed info
        String seedInfo = "World Seed: " + seed;
        float seedW = font.getStringWidth(seedInfo, 0.85f);
        font.drawString(gui, seedInfo, (width - seedW) / 2.0f, height - 32, 0.85f, 0x777777, false);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
