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

        int btnW = 200;
        int btnH = 42;
        int bottomY = height - 98;

        if (currentWorld != null) {
            // Row 1: Play Selected World (Left) | Create Disabled (Right)
            buttons.add(new Button(1, "Play Selected World", width / 2 - 210, bottomY, btnW, btnH, () -> {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome);
            }));

            Button createBtn = new Button(2, "Create New World", width / 2 + 10, bottomY, btnW, btnH, () -> {});
            createBtn.setEnabled(false);
            buttons.add(createBtn);

            // Row 2: Delete (Left) | Back (Right)
            buttons.add(new Button(3, "Delete World", width / 2 - 210, bottomY + 48, btnW, btnH, () -> {
                WorldSaveManager.deleteWorld();
                init(width, height);
            }));

            buttons.add(new Button(4, "Back", width / 2 + 10, bottomY + 48, btnW, btnH, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        } else {
            // Empty State: Create New World (Left) | Back (Right)
            Button playBtn = new Button(1, "Play Selected World", width / 2 - 210, bottomY, btnW, btnH, () -> {});
            playBtn.setEnabled(false);
            buttons.add(playBtn);

            buttons.add(new Button(2, "Create New World", width / 2 + 10, bottomY, btnW, btnH, () -> {
                app.setScreen(new WorldCreateScreen(app));
            }));

            buttons.add(new Button(3, "Back", width / 2 - 100, bottomY + 48, btnW, btnH, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Semi-transparent dark overlay showing the rotating 3D world behind it
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.40f);

        // 2. Top Header and Bottom Footer Strips (Vanilla Minecraft layout)
        gui.drawMenuHeaderFooterStrips(width, height, 80, 110);

        // Header Title
        String title = "Select World";
        float titleW = font.getStringWidth(title, 1.35f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 16, 1.35f, 0xffffff, true);

        // Search Input Box
        int searchW = 340;
        int searchH = 30;
        int searchX = (width - searchW) / 2;
        int searchY = 44;
        gui.drawRect(searchX - 1, searchY - 1, searchW + 2, searchH + 2, 0.7f, 0.7f, 0.7f, 1.0f);
        gui.drawRect(searchX, searchY, searchW, searchH, 0.0f, 0.0f, 0.0f, 1.0f);
        font.drawString(gui, "Search...", searchX + 10, searchY + 7, 0.85f, 0x888888, false);

        // 3. Center World Viewport
        int boxW = 720;
        int boxH = height - 200;
        int boxX = (width - boxW) / 2;
        int boxY = 86;

        gui.drawRect(boxX, boxY, boxW, boxH, 0.0f, 0.0f, 0.0f, 0.35f);

        if (currentWorld != null) {
            int itemY = boxY + 14;
            int itemH = 78;
            int itemW = boxW - 28;
            int itemX = boxX + 14;

            // Outer white selection outline (matching Image 2 Reference)
            gui.drawRect(itemX - 1, itemY - 1, itemW + 2, itemH + 2, 1.0f, 1.0f, 1.0f, 0.9f);
            gui.drawRect(itemX, itemY, itemW, itemH, 0.12f, 0.12f, 0.14f, 0.95f);

            // World Icon Preview (Left 58x58 box)
            int iconSize = 58;
            int iconX = itemX + 10;
            int iconY = itemY + 10;
            gui.drawRect(iconX, iconY, iconSize, iconSize, 0.22f, 0.48f, 0.18f, 1.0f);
            font.drawString(gui, "⛏", iconX + 19, iconY + 17, 1.5f, 0xffffff, true);

            // Text Info Hierarchy
            int textX = iconX + iconSize + 16;

            // Line 1: Bold World Name
            font.drawString(gui, currentWorld.name, textX, itemY + 12, 1.2f, 0xffffff, true);

            // Line 2: Folder name & Timestamp in parentheses
            String timeStr = currentWorld.name + " (" + currentWorld.createdAt + ")";
            font.drawString(gui, timeStr, textX, itemY + 35, 0.82f, 0x999999, false);

            // Line 3: Hardcore Mode & Biome Details
            String modeStr = "Hardcore Mode, Biome: " + currentWorld.biome + ", Seed: " + currentWorld.seed;
            font.drawString(gui, modeStr, textX, itemY + 54, 0.80f, 0xff7777, false);

            // Single World Rule Notice
            String notice = "(Only 1 Hardcore world allowed. Delete existing world to create a new one)";
            float noticeW = font.getStringWidth(notice, 0.80f);
            font.drawString(gui, notice, (width - noticeW) / 2.0f, boxY + boxH - 26, 0.80f, 0xaaaaaa, false);
        } else {
            // Empty State
            String empty1 = "No Worlds Found";
            float e1W = font.getStringWidth(empty1, 1.3f);
            font.drawString(gui, empty1, (width - e1W) / 2.0f, boxY + 110, 1.3f, 0xcccccc, true);

            String empty2 = "Click 'Create New World' to begin your Hardcore adventure.";
            float e2W = font.getStringWidth(empty2, 0.9f);
            font.drawString(gui, empty2, (width - e2W) / 2.0f, boxY + 150, 0.9f, 0x888888, false);
        }

        // 4. Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
