package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

import java.util.ArrayList;
import java.util.List;

public class WorldSelectScreen extends Screen {
    public static class WorldEntry {
        public String id;
        public String name;
        public String biome;
        public String gameMode;
        public long seed;

        public WorldEntry(String id, String name, String biome, String gameMode, long seed) {
            this.id = id;
            this.name = name;
            this.biome = biome;
            this.gameMode = gameMode;
            this.seed = seed;
        }
    }

    private final List<WorldEntry> worlds = new ArrayList<>();
    private int selectedIndex = 0;

    public WorldSelectScreen(MCJournalApp app) {
        super(app);
        // Default worlds (Hardcore only)
        worlds.add(new WorldEntry("world_1", "Adventurer's Frontier", "Plains", "Hardcore", 4242));
        worlds.add(new WorldEntry("world_2", "Dense Birch Highlands", "Birch Forest", "Hardcore", 1337));
        worlds.add(new WorldEntry("world_3", "Crystal Lake Sanctuary", "River Basin", "Hardcore", 9999));
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnWidth = 160;
        int btnHeight = 40;
        int bottomY = height - 60;

        // 1. Play Selected World
        buttons.add(new Button(1, "Play Selected World", width / 2 - 250, bottomY, btnWidth + 40, btnHeight, () -> {
            if (selectedIndex >= 0 && selectedIndex < worlds.size()) {
                WorldEntry entry = worlds.get(selectedIndex);
                app.enterWorld(entry.seed, entry.name, entry.biome);
            }
        }));

        // 2. Create New World
        buttons.add(new Button(2, "Create New World", width / 2 - 40, bottomY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldCreateScreen(app));
        }));

        // 3. Back to Main Menu
        buttons.add(new Button(3, "Cancel", width / 2 + 130, bottomY, btnWidth - 30, btnHeight, () -> {
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Darkened background
        gui.drawRect(0, 0, width, height, 0.10f, 0.10f, 0.12f, 1.0f);

        // Header Title
        String title = "Select World";
        float titleW = font.getStringWidth(title, 1.5f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 24, 1.5f, 0xffffff, true);

        // World List Container
        int boxW = 680;
        int boxH = 420;
        int boxX = (width - boxW) / 2;
        int boxY = 70;

        gui.drawBevelBox(boxX, boxY, boxW, boxH, 0x181818, 0x333333, 0x080808);

        // World Entries
        int itemH = 68;
        for (int i = 0; i < worlds.size(); i++) {
            WorldEntry w = worlds.get(i);
            int itemY = boxY + 12 + (i * (itemH + 8));

            boolean isSelected = (i == selectedIndex);
            int bg = isSelected ? 0x3a3a3a : 0x222222;
            int borderL = isSelected ? 0xffffff : 0x444444;
            int borderD = isSelected ? 0xaaaaaa : 0x111111;

            gui.drawBevelBox(boxX + 12, itemY, boxW - 24, itemH, bg, borderL, borderD);

            // Icon + World Name
            font.drawString(gui, "🌍 " + w.name, boxX + 24, itemY + 14, 1.15f, isSelected ? 0xffffa0 : 0xffffff, true);

            // Details
            String details = "Biome: " + w.biome + " | Mode: " + w.gameMode + " | Seed: " + w.seed;
            font.drawString(gui, details, boxX + 24, itemY + 40, 0.85f, 0xaaaaaa, false);
        }

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int boxW = 680;
        int boxX = (width - boxW) / 2;
        int boxY = 70;
        int itemH = 68;

        for (int i = 0; i < worlds.size(); i++) {
            int itemY = boxY + 12 + (i * (itemH + 8));
            if (mouseX >= boxX + 12 && mouseX <= boxX + boxW - 12 && mouseY >= itemY && mouseY <= itemY + itemH) {
                this.selectedIndex = i;
                return true;
            }
        }

        return false;
    }
}
