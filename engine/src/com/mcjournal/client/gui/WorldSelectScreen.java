package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;

public class WorldSelectScreen extends Screen {
    private WorldSaveManager.SavedWorld currentWorld;

    public WorldSelectScreen(MCJournalApp app) {
        super(app);
        this.currentWorld = WorldSaveManager.loadWorld();
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        this.currentWorld = WorldSaveManager.loadWorld();

        int btnWidth = 170;
        int btnHeight = 44;
        int bottomY = height - 70;

        if (currentWorld != null) {
            // World Exists: Play, Delete, Cancel
            buttons.add(new Button(1, "Play World", width / 2 - 270, bottomY, btnWidth + 20, btnHeight, () -> {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome);
            }));

            buttons.add(new Button(2, "Delete World", width / 2 - 70, bottomY, btnWidth, btnHeight, () -> {
                WorldSaveManager.deleteWorld();
                init(width, height); // Refresh screen to empty state
            }));

            buttons.add(new Button(3, "Back", width / 2 + 110, bottomY, btnWidth - 30, btnHeight, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        } else {
            // No World Exists: Create New World, Back
            buttons.add(new Button(1, "Create Hardcore World", width / 2 - 190, bottomY, btnWidth + 60, btnHeight, () -> {
                app.setScreen(new WorldCreateScreen(app));
            }));

            buttons.add(new Button(2, "Back", width / 2 + 50, bottomY, btnWidth - 40, btnHeight, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Darkened background
        gui.drawRect(0, 0, width, height, 0.10f, 0.10f, 0.12f, 1.0f);

        // Header Title
        String title = "Singleplayer";
        float titleW = font.getStringWidth(title, 1.5f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 30, 1.5f, 0xffffff, true);

        // Container Box
        int boxW = 680;
        int boxH = 380;
        int boxX = (width - boxW) / 2;
        int boxY = 80;

        gui.drawBevelBox(boxX, boxY, boxW, boxH, 0x181818, 0x333333, 0x080808);

        if (currentWorld != null) {
            // Render Active Single World Entry
            int itemY = boxY + 30;
            int itemH = 100;
            gui.drawBevelBox(boxX + 24, itemY, boxW - 48, itemH, 0x2e1818, 0xaa3333, 0x441111);

            // Icon + World Name
            font.drawString(gui, "⚔ " + currentWorld.name, boxX + 44, itemY + 18, 1.25f, 0xff5555, true);

            // Details
            String details = "Biome: " + currentWorld.biome + " | Mode: Hardcore (Locked) | Seed: " + currentWorld.seed;
            font.drawString(gui, details, boxX + 44, itemY + 48, 0.85f, 0xddaaaa, false);

            String date = "Created: " + currentWorld.createdAt;
            font.drawString(gui, date, boxX + 44, itemY + 70, 0.75f, 0x888888, false);

            // Notice
            String notice = "(Only 1 Hardcore World permitted. Delete to start a new adventure)";
            float noticeW = font.getStringWidth(notice, 0.8f);
            font.drawString(gui, notice, (width - noticeW) / 2.0f, boxY + boxH - 35, 0.8f, 0x888888, false);
        } else {
            // Empty State Display
            String empty1 = "No Hardcore World Found";
            float e1W = font.getStringWidth(empty1, 1.25f);
            font.drawString(gui, empty1, (width - e1W) / 2.0f, boxY + 140, 1.25f, 0xaaaaaa, true);

            String empty2 = "Create your singleplayer world to begin your journal.";
            float e2W = font.getStringWidth(empty2, 0.9f);
            font.drawString(gui, empty2, (width - e2W) / 2.0f, boxY + 180, 0.9f, 0x666666, false);
        }

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
