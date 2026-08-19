package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class WorldCreateScreen extends Screen {
    private String worldName = "New Adventure";
    private final String gameMode = "Hardcore"; // Hardcore ONLY
    private long seed = System.currentTimeMillis() % 1000000L;

    public WorldCreateScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 200;
        int btnH = 40;
        int bottomY = height - 58;

        // 1. Create Hardcore World Button (Left)
        buttons.add(new Button(1, "Create New World", width / 2 - 205, bottomY, btnW, btnH, () -> {
            app.enterWorld(seed, worldName, "Multi-Biome", null);
        }));

        // 2. Cancel Button (Right)
        buttons.add(new Button(2, "Cancel", width / 2 + 5, bottomY, btnW, btnH, () -> {
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Dark translucent slate background with header/footer strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.45f);
        gui.drawMenuHeaderFooterStrips(width, height, 64, 80);

        // Header Title
        String title = "Create New World";
        float titleW = font.getStringWidth(title, 1.30f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 18, 1.30f, 0xffffff, true);

        // Form Container
        int boxW = 400;
        int centerX = (width - boxW) / 2;
        int startY = 82;

        // 1. World Name Input Field
        font.drawString(gui, "World Name", centerX, startY, 0.85f, 0xaaaaaa, true);
        int nameInputY = startY + 18;
        gui.drawRect(centerX - 1, nameInputY - 1, boxW + 2, 34, 0.65f, 0.65f, 0.65f, 1.0f);
        gui.drawRect(centerX, nameInputY, boxW, 32, 0.0f, 0.0f, 0.0f, 1.0f);
        font.drawString(gui, worldName + "_", centerX + 10, nameInputY + 8, 0.95f, 0xffffff, false);

        // 2. Game Mode Display Card (Hardcore Locked)
        int modeY = nameInputY + 44;
        font.drawString(gui, "Game Mode", centerX, modeY, 0.85f, 0xaaaaaa, true);
        int modeCardY = modeY + 18;
        gui.drawBevelBox(centerX, modeCardY, boxW, 52, 0x220a0a, 0xaa2222, 0x550808);
        font.drawString(gui, "⚔ Game Mode: HARDCORE", centerX + 12, modeCardY + 10, 1.0f, 0xff5555, true);
        font.drawString(gui, "Same as Survival mode, locked at hardest difficulty & 1 life.", centerX + 12, modeCardY + 30, 0.72f, 0xee9999, false);

        // 3. World Type / Biome Info Card
        int typeY = modeCardY + 62;
        font.drawString(gui, "World Type", centerX, typeY, 0.85f, 0xaaaaaa, true);
        int typeCardY = typeY + 18;
        gui.drawBevelBox(centerX, typeCardY, boxW, 36, 0x18181a, 0x555558, 0x222224);
        font.drawString(gui, "World Type: Multi-Biome (1.17 Extended Generator)", centerX + 12, typeCardY + 10, 0.82f, 0xdddddd, true);

        // 4. Seed Info Card
        int seedY = typeCardY + 46;
        gui.drawBevelBox(centerX, seedY, boxW, 34, 0x141416, 0x444448, 0x1a1a1c);
        font.drawString(gui, "Seed: " + seed + " (Height: 256, 500+ Chunks)", centerX + 12, seedY + 10, 0.80f, 0x888888, false);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
