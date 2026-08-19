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
        int btnH = 40;
        int bottomY = height - 96;

        if (currentWorld != null) {
            // Row 1: Play Selected World (Left) | Create Disabled (Right)
            buttons.add(new Button(1, "Play Selected World", width / 2 - 205, bottomY, btnW, btnH, () -> {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome, currentWorld);
            }));

            Button createBtn = new Button(2, "Create New World", width / 2 + 5, bottomY, btnW, btnH, () -> {});
            createBtn.setEnabled(false);
            buttons.add(createBtn);

            // Row 2: Delete (Left) | Cancel (Right)
            buttons.add(new Button(3, "Delete World", width / 2 - 205, bottomY + 46, btnW, btnH, () -> {
                WorldSaveManager.deleteWorld();
                init(width, height);
            }));

            buttons.add(new Button(4, "Cancel", width / 2 + 5, bottomY + 46, btnW, btnH, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        } else {
            // Empty State: Create New World (Left) | Cancel (Right)
            Button playBtn = new Button(1, "Play Selected World", width / 2 - 205, bottomY, btnW, btnH, () -> {});
            playBtn.setEnabled(false);
            buttons.add(playBtn);

            buttons.add(new Button(2, "Create New World", width / 2 + 5, bottomY, btnW, btnH, () -> {
                app.setScreen(new WorldCreateScreen(app));
            }));

            buttons.add(new Button(3, "Cancel", width / 2 - 100, bottomY + 46, btnW, btnH, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Semi-transparent dark overlay showing the rotating 3D world behind it
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.42f);

        // 2. Top Header and Bottom Footer Strips (Vanilla Minecraft layout)
        gui.drawMenuHeaderFooterStrips(width, height, 76, 108);

        // Header Title
        String title = "Select World";
        float titleW = font.getStringWidth(title, 1.30f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 14, 1.30f, 0xffffff, true);

        // Search Input Box (Minecraft 1.20 centered)
        int searchW = 380;
        int searchH = 26;
        int searchX = (width - searchW) / 2;
        int searchY = 40;
        gui.drawRect(searchX - 1, searchY - 1, searchW + 2, searchH + 2, 0.65f, 0.65f, 0.65f, 1.0f);
        gui.drawRect(searchX, searchY, searchW, searchH, 0.0f, 0.0f, 0.0f, 1.0f);
        font.drawString(gui, "Search...", searchX + 8, searchY + 5, 0.80f, 0x777777, false);

        // 3. Center World Viewport
        int boxW = Math.min(720, width - 40);
        int boxH = height - 196;
        int boxX = (width - boxW) / 2;
        int boxY = 80;

        gui.drawRect(boxX, boxY, boxW, boxH, 0.0f, 0.0f, 0.0f, 0.35f);

        if (currentWorld != null) {
            int itemY = boxY + 12;
            int itemH = 80;
            int itemW = boxW - 24;
            int itemX = boxX + 12;

            // Outer white selection outline (matching Minecraft Java 1.20 selection card)
            gui.drawRect(itemX - 1, itemY - 1, itemW + 2, itemH + 2, 1.0f, 1.0f, 1.0f, 0.95f);
            gui.drawRect(itemX, itemY, itemW, itemH, 0.10f, 0.10f, 0.12f, 0.95f);

            // World Icon Preview (Left 60x60 box with pixel-art grass icon)
            int iconSize = 60;
            int iconX = itemX + 10;
            int iconY = itemY + 10;
            gui.drawRect(iconX, iconY, iconSize, iconSize, 0.20f, 0.44f, 0.16f, 1.0f);
            gui.drawRect(iconX, iconY + 18, iconSize, iconSize - 18, 0.42f, 0.28f, 0.18f, 1.0f);
            font.drawString(gui, "⛏", iconX + 20, iconY + 18, 1.4f, 0xffffff, true);

            // Text Info Hierarchy
            int textX = iconX + iconSize + 14;

            // Line 1: Bold World Name
            font.drawString(gui, currentWorld.name, textX, itemY + 10, 1.15f, 0xffffff, true);

            // Line 2: Folder name & Timestamp in parentheses
            String timeStr = "hardcore_world.json (" + currentWorld.createdAt + ")";
            font.drawString(gui, timeStr, textX, itemY + 34, 0.80f, 0x888888, false);

            // Line 3: Hardcore Mode & Biome Details
            String modeStr = "⚔ Hardcore Mode (Permadeath) | " + currentWorld.biome + " | Seed: " + currentWorld.seed;
            font.drawString(gui, modeStr, textX, itemY + 54, 0.78f, 0xff6666, false);

            // Single World Rule Notice
            String notice = "(Only 1 Hardcore world allowed. Delete existing world to start a new journey)";
            float noticeW = font.getStringWidth(notice, 0.78f);
            font.drawString(gui, notice, (width - noticeW) / 2.0f, boxY + boxH - 24, 0.78f, 0x999999, false);
        } else {
            // Empty State
            String empty1 = "No Worlds Found";
            float e1W = font.getStringWidth(empty1, 1.25f);
            font.drawString(gui, empty1, (width - e1W) / 2.0f, boxY + (boxH / 2) - 25, 1.25f, 0xcccccc, true);

            String empty2 = "Click 'Create New World' to begin your Hardcore adventure.";
            float e2W = font.getStringWidth(empty2, 0.85f);
            font.drawString(gui, empty2, (width - e2W) / 2.0f, boxY + (boxH / 2) + 10, 0.85f, 0x888888, false);
        }

        // 4. Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
