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
        int bottomY = height - 90;

        if (currentWorld != null) {
            // Row 1: Play Selected World (Left) | Create Disabled (Right)
            buttons.add(new Button(1, "Play Selected World", width / 2 - 210, bottomY, btnW, btnH, () -> {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome);
            }));

            Button createBtn = new Button(2, "Create New World", width / 2 + 10, bottomY, btnW, btnH, () -> {});
            createBtn.setEnabled(false); // Disabled because world already exists!
            buttons.add(createBtn);

            // Row 2: Delete (Left) | Back (Right)
            buttons.add(new Button(3, "Delete World", width / 2 - 210, bottomY + 46, btnW, btnH, () -> {
                WorldSaveManager.deleteWorld();
                init(width, height); // Refresh screen to empty state
            }));

            buttons.add(new Button(4, "Back", width / 2 + 10, bottomY + 46, btnW, btnH, () -> {
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

            buttons.add(new Button(3, "Back", width / 2 - 100, bottomY + 46, btnW, btnH, () -> {
                app.setScreen(new TitleScreen(app));
            }));
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Darkened vignette background
        gui.drawRect(0, 0, width, height, 0.08f, 0.08f, 0.10f, 1.0f);

        // Header Title
        String title = "Select World";
        float titleW = font.getStringWidth(title, 1.3f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 20, 1.3f, 0xffffff, true);

        // Search Bar Box (Vanilla Style)
        int searchW = 320;
        int searchH = 28;
        int searchX = (width - searchW) / 2;
        int searchY = 50;
        gui.drawRect(searchX - 1, searchY - 1, searchW + 2, searchH + 2, 0.6f, 0.6f, 0.6f, 1.0f);
        gui.drawRect(searchX, searchY, searchW, searchH, 0.0f, 0.0f, 0.0f, 1.0f);
        font.drawString(gui, "Search...", searchX + 8, searchY + 7, 0.75f, 0x666666, false);

        // World List Container (Dark semi-transparent viewport)
        int boxW = 680;
        int boxH = 340;
        int boxX = (width - boxW) / 2;
        int boxY = 88;

        gui.drawRect(boxX, boxY, boxW, boxH, 0.0f, 0.0f, 0.0f, 0.45f);

        if (currentWorld != null) {
            // Render Selected World Card (Matching Image 2 Reference)
            int itemY = boxY + 16;
            int itemH = 74;
            int itemW = boxW - 32;
            int itemX = boxX + 16;

            // Outer white outline border for selected card
            gui.drawRect(itemX - 1, itemY - 1, itemW + 2, itemH + 2, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(itemX, itemY, itemW, itemH, 0.12f, 0.12f, 0.14f, 0.95f);

            // World Icon Preview (Left 56x56 box)
            int iconSize = 56;
            int iconX = itemX + 8;
            int iconY = itemY + 9;
            gui.drawRect(iconX, iconY, iconSize, iconSize, 0.25f, 0.45f, 0.18f, 1.0f);
            font.drawString(gui, "⛏", iconX + 18, iconY + 16, 1.4f, 0xffffff, true);

            // Text Info (Right of icon)
            int textX = iconX + iconSize + 14;

            // Line 1: Bold World Name
            font.drawString(gui, currentWorld.name, textX, itemY + 12, 1.15f, 0xffffff, true);

            // Line 2: Folder name & Timestamp in parentheses
            String timeStr = currentWorld.name + " (" + currentWorld.createdAt + ")";
            font.drawString(gui, timeStr, textX, itemY + 34, 0.78f, 0x888888, false);

            // Line 3: Hardcore Mode & Version info
            String modeStr = "Hardcore Mode, Biome: " + currentWorld.biome + ", Seed: " + currentWorld.seed;
            font.drawString(gui, modeStr, textX, itemY + 52, 0.75f, 0xee7777, false);

            // Notice
            String notice = "(Only 1 Hardcore world allowed. Delete existing world to create a new one)";
            float noticeW = font.getStringWidth(notice, 0.75f);
            font.drawString(gui, notice, (width - noticeW) / 2.0f, boxY + boxH - 24, 0.75f, 0x777777, false);
        } else {
            // Empty State
            String empty1 = "No Worlds Found";
            float e1W = font.getStringWidth(empty1, 1.2f);
            font.drawString(gui, empty1, (width - e1W) / 2.0f, boxY + 120, 1.2f, 0x888888, true);

            String empty2 = "Click 'Create New World' to begin your Hardcore journal.";
            float e2W = font.getStringWidth(empty2, 0.85f);
            font.drawString(gui, empty2, (width - e2W) / 2.0f, boxY + 155, 0.85f, 0x555555, false);
        }

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
