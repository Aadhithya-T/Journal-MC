package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;

public class WorldSelectScreen extends Screen {
    private WorldSaveManager.SavedWorld currentWorld;
    private boolean isWorldSelected = true;
    private String searchText = "";
    private float cursorTimer = 0.0f;

    public WorldSelectScreen(MCJournalApp app) {
        super(app);
        this.currentWorld = WorldSaveManager.loadWorld();
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        this.currentWorld = WorldSaveManager.loadWorld();
        this.isWorldSelected = (currentWorld != null);

        int totalBtnW = 400;
        int centerX = (width - totalBtnW) / 2;

        // Bottom Footer Button Positions
        int row1Y = height - 88;
        int row2Y = height - 44;
        int btnH = 38; // Equal height for both rows

        int row1BtnW = (totalBtnW - 8) / 2; // 196px each
        int row2BtnW = (totalBtnW - 2 * 8) / 3; // 128px each

        // --- ROW 1 ---
        // 1. Play Selected World (Left)
        Button playBtn = new Button(1, "Play Selected World", centerX, row1Y, row1BtnW, btnH, () -> {
            if (currentWorld != null) {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome, currentWorld);
            }
        });
        playBtn.setEnabled(currentWorld != null && isWorldSelected);
        buttons.add(playBtn);

        // 2. Create New World (Right, only enabled when 0 worlds exist)
        Button createBtn = new Button(2, "Create New World", centerX + row1BtnW + 8, row1Y, row1BtnW, btnH, () -> {
            if (currentWorld == null) {
                app.setScreen(new WorldCreateScreen(app));
            }
        });
        createBtn.setEnabled(currentWorld == null);
        buttons.add(createBtn);

        // --- ROW 2 (3 Buttons: Edit | Delete | Back) ---
        int x0 = centerX;
        int x1 = x0 + row2BtnW + 8;
        int x2 = x1 + row2BtnW + 8;

        // 3. Edit Button (Renames the world)
        Button editBtn = new Button(3, "Edit", x0, row2Y, row2BtnW, btnH, () -> {
            if (currentWorld != null) {
                app.setScreen(new WorldEditScreen(app, currentWorld));
            }
        });
        editBtn.setEnabled(currentWorld != null);
        buttons.add(editBtn);

        // 4. Delete Button
        Button deleteBtn = new Button(4, "Delete", x1, row2Y, row2BtnW, btnH, () -> {
            WorldSaveManager.deleteWorld();
            init(width, height);
        });
        deleteBtn.setEnabled(currentWorld != null);
        buttons.add(deleteBtn);

        // 5. Back Button
        buttons.add(new Button(5, "Back", x2, row2Y, row2BtnW, btnH, () -> {
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        cursorTimer += deltaTime * 2.0f;

        // 1. Semi-transparent dark overlay showing the background
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.35f);

        // 2. Top Header and Bottom Footer Strips
        gui.drawMenuHeaderFooterStrips(width, height, 72, 98);

        // Header Title (Centered with comfortable top margin)
        String title = "Select World";
        float titleW = font.getStringWidth(title, 1.20f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 12, 1.20f, 0xffffff, true);

        // Search Input Box (Centered, 380px wide)
        int searchW = 380;
        int searchH = 24;
        int searchX = (width - searchW) / 2;
        int searchY = 38;

        gui.drawRect(searchX - 1, searchY - 1, searchW + 2, searchH + 2, 0.65f, 0.65f, 0.65f, 1.0f);
        gui.drawRect(searchX, searchY, searchW, searchH, 0.0f, 0.0f, 0.0f, 1.0f);

        String searchDisplay = searchText.isEmpty() ? "Search..." : searchText + ((int) cursorTimer % 2 == 0 ? "_" : "");
        int searchColor = searchText.isEmpty() ? 0x777777 : 0xffffff;
        font.drawString(gui, searchDisplay, searchX + 8, searchY + 4, 0.75f, searchColor, false);

        // 3. Center World Selection Cards List (Starting at Y=96 with clear separation from header)
        int cardW = Math.min(640, width - 48);
        int cardH = 80;
        int cardX = (width - cardW) / 2;
        int cardY = 96;

        if (currentWorld != null) {
            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

            // Selection / Hover Card Background
            if (isWorldSelected) {
                gui.drawRect(cardX - 1, cardY - 1, cardW + 2, cardH + 2, 1.0f, 1.0f, 1.0f, 0.95f);
                gui.drawRect(cardX, cardY, cardW, cardH, 0.12f, 0.12f, 0.14f, 0.95f);
            } else if (isHovered) {
                gui.drawRect(cardX, cardY, cardW, cardH, 0.20f, 0.20f, 0.22f, 0.85f);
            } else {
                gui.drawRect(cardX, cardY, cardW, cardH, 0.08f, 0.08f, 0.10f, 0.75f);
            }

            // World Icon Thumbnail (56x56 box, centered vertically in card)
            int iconSize = 56;
            int iconX = cardX + 12;
            int iconY = cardY + 12;

            // Sky top / Grass middle / Dirt bottom
            gui.drawRect(iconX, iconY, iconSize, 24, 0.45f, 0.65f, 0.92f, 1.0f);        // Sky
            gui.drawRect(iconX, iconY + 24, iconSize, 8, 0.35f, 0.68f, 0.22f, 1.0f);    // Grass top
            gui.drawRect(iconX, iconY + 32, iconSize, 24, 0.52f, 0.36f, 0.22f, 1.0f);   // Dirt

            // Mini Oak Tree on Icon
            gui.drawRect(iconX + 24, iconY + 16, 8, 16, 0.40f, 0.26f, 0.12f, 1.0f);     // Trunk
            gui.drawRect(iconX + 18, iconY + 8, 20, 12, 0.28f, 0.58f, 0.18f, 1.0f);     // Canopy

            // Play Icon overlay on hover
            if (isHovered) {
                gui.drawRect(iconX, iconY, iconSize, iconSize, 0.0f, 0.0f, 0.0f, 0.4f);
                font.drawString(gui, "▶", iconX + 20, iconY + 14, 1.35f, 0xffffff, true);
            }

            // Text Info Hierarchy (All text properly padded and contained inside the card)
            int textX = iconX + iconSize + 14;

            // Line 1: World Name (Bold White)
            font.drawString(gui, currentWorld.name, textX, cardY + 12, 0.92f, 0xffffff, true);

            // Line 2: Folder name & Timestamp in parentheses
            String timeStr = "hardcore_world (" + currentWorld.createdAt + ")";
            font.drawString(gui, timeStr, textX, cardY + 34, 0.70f, 0x888888, false);

            // Line 3: Mode & Version details
            String modeStr = "Hardcore Mode, Version: 1.20 / Java 26";
            font.drawString(gui, modeStr, textX, cardY + 54, 0.70f, 0xaaaaaa, false);
        } else {
            // Empty State
            String empty1 = "No Worlds Found";
            float e1W = font.getStringWidth(empty1, 1.20f);
            font.drawString(gui, empty1, (width - e1W) / 2.0f, height / 2 - 35, 1.20f, 0xcccccc, true);

            String empty2 = "Click 'Create New World' to begin your adventure.";
            float e2W = font.getStringWidth(empty2, 0.82f);
            font.drawString(gui, empty2, (width - e2W) / 2.0f, height / 2, 0.82f, 0x888888, false);
        }

        // 4. Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int cardW = Math.min(640, width - 48);
        int cardH = 80;
        int cardX = (width - cardW) / 2;
        int cardY = 96;

        if (currentWorld != null && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
            isWorldSelected = true;
            init(width, height);

            // Click card to enter directly
            if (button == 0) {
                app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome, currentWorld);
                return true;
            }
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && currentWorld != null && isWorldSelected) {
            app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome, currentWorld);
        } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            app.setScreen(new TitleScreen(app));
        }
    }
}
