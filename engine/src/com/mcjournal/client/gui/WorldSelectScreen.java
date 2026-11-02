package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt WorldSelectScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, search filtering, and safe confirmation dialogs.
 */
public class WorldSelectScreen extends Screen {
    private WorldSaveManager.SavedWorld currentWorld;
    private boolean isWorldSelected = true;
    private TextField searchField;
    private long lastCardClickTime = 0;

    public WorldSelectScreen(MCJournalApp app) {
        super(app);
        this.currentWorld = WorldSaveManager.loadWorld();
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    public WorldSaveManager.SavedWorld getCurrentWorld() {
        return currentWorld;
    }

    public boolean isWorldSelected() {
        return isWorldSelected;
    }

    public TextField getSearchField() {
        return searchField;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        this.currentWorld = WorldSaveManager.loadWorld();
        this.isWorldSelected = (currentWorld != null);

        int totalBtnW = Math.min(320, width - 24);
        int centerX = GuiLayout.centerX(width, totalBtnW);

        // 1. Search Box at top
        int searchW = Math.min(220, width - 32);
        int searchX = GuiLayout.centerX(width, searchW);
        searchField = new TextField(1, searchX, 22, searchW, 18);
        searchField.setPlaceholder("Search worlds...");
        addWidget(searchField);

        // 2. Action Buttons at Bottom (Two rows)
        int btnH = 20;
        int rowGap = 4;
        int row2Y = height - 28;
        int row1Y = row2Y - btnH - rowGap;

        // --- ROW 1 (Play Selected World | Create New World) ---
        int r1BtnW = (totalBtnW - 6) / 2;
        Button playBtn = new Button(2, "Play World", centerX, row1Y, r1BtnW, btnH, this::launchSelectedWorld);
        playBtn.setEnabled(isPlayable());
        addWidget(playBtn);

        Button createBtn = new Button(3, "Create New World", centerX + r1BtnW + 6, row1Y, r1BtnW, btnH, () -> {
            if (app != null) app.setScreen(new WorldCreateScreen(app));
        });
        addWidget(createBtn);

        // --- ROW 2 (Edit | Delete | Cancel) ---
        int r2BtnW = (totalBtnW - 8) / 3;
        int x0 = centerX;
        int x1 = x0 + r2BtnW + 4;
        int x2 = x1 + r2BtnW + 4;

        Button editBtn = new Button(4, "Edit", x0, row2Y, r2BtnW, btnH, () -> {
            if (currentWorld != null && app != null) {
                app.setScreen(new WorldEditScreen(app, currentWorld));
            }
        });
        editBtn.setEnabled(currentWorld != null);
        addWidget(editBtn);

        Button deleteBtn = new Button(5, "Delete", x1, row2Y, r2BtnW, btnH, this::promptDeleteWorld);
        deleteBtn.setEnabled(currentWorld != null);
        addWidget(deleteBtn);

        Button cancelBtn = new Button(6, "Cancel", x2, row2Y, r2BtnW, btnH, () -> {
            if (app != null) app.setScreen(new TitleScreen(app));
        });
        addWidget(cancelBtn);
    }

    private boolean isPlayable() {
        if (currentWorld == null || !isWorldSelected) return false;
        if (searchField == null) return true;
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return true;
        return currentWorld.name != null && currentWorld.name.toLowerCase().contains(query);
    }

    public void launchSelectedWorld() {
        if (isPlayable() && app != null) {
            app.enterWorld(currentWorld.seed, currentWorld.name, currentWorld.biome, currentWorld);
        }
    }

    public void promptDeleteWorld() {
        if (currentWorld == null) return;

        String worldName = currentWorld.name != null ? currentWorld.name : "this world";
        String prompt = "Are you sure you want to delete '" + worldName + "'?\nThis action cannot be undone!";

        ConfirmScreen confirm = new ConfirmScreen(app, "Delete World?", prompt, "Delete", "Cancel", true,
                () -> {
                    WorldSaveManager.deleteWorld();
                    currentWorld = null;
                    init(width, height);
                },
                () -> {
                    if (app != null) app.popScreen();
                });

        if (app != null) {
            app.pushScreen(confirm);
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Semi-transparent backdrop and header/footer strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.40f);
        gui.drawMenuHeaderFooterStrips(width, height, 44, 56);

        // 2. Header Title
        String title = "Select World";
        float titleScale = 0.80f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), 8, titleScale, 0xffffff, true);

        // 3. World List Card
        int cardW = Math.min(360, width - 24);
        int cardH = 52;
        int cardX = GuiLayout.centerX(width, cardW);
        int cardY = 52;

        boolean matchesSearch = isPlayable();

        if (currentWorld != null && matchesSearch) {
            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

            // Card background & border
            if (isWorldSelected) {
                gui.drawRect(cardX - 1, cardY - 1, cardW + 2, cardH + 2, 1.0f, 1.0f, 1.0f, 0.90f);
                gui.drawRect(cardX, cardY, cardW, cardH, 0.14f, 0.14f, 0.18f, 0.95f);
            } else if (isHovered) {
                gui.drawRect(cardX, cardY, cardW, cardH, 0.20f, 0.20f, 0.24f, 0.85f);
            } else {
                gui.drawRect(cardX, cardY, cardW, cardH, 0.10f, 0.10f, 0.12f, 0.75f);
            }

            // Thumbnail Icon (36x36)
            int iconSize = 36;
            int iconX = cardX + 8;
            int iconY = cardY + 8;

            gui.drawRect(iconX, iconY, iconSize, 16, 0.45f, 0.65f, 0.92f, 1.0f);        // Sky
            gui.drawRect(iconX, iconY + 16, iconSize, 6, 0.35f, 0.68f, 0.22f, 1.0f);    // Grass
            gui.drawRect(iconX, iconY + 22, iconSize, 14, 0.52f, 0.36f, 0.22f, 1.0f);   // Dirt

            // Mini tree
            gui.drawRect(iconX + 16, iconY + 10, 4, 12, 0.40f, 0.26f, 0.12f, 1.0f);    // Trunk
            gui.drawRect(iconX + 12, iconY + 4, 12, 8, 0.28f, 0.58f, 0.18f, 1.0f);     // Leaves

            if (isHovered) {
                gui.drawRect(iconX, iconY, iconSize, iconSize, 0.0f, 0.0f, 0.0f, 0.40f);
                font.drawString(gui, "▶", iconX + 13, iconY + 10, 0.85f, 0xffffff, true);
            }

            // Text Info Hierarchy
            int textX = iconX + iconSize + 10;
            font.drawString(gui, currentWorld.name != null ? currentWorld.name : "Hardcore World", textX, cardY + 8, 0.62f, 0xffffff, true);

            String timeStr = "hardcore_world (" + (currentWorld.createdAt != null ? currentWorld.createdAt : "Saved") + ")";
            font.drawString(gui, timeStr, textX, cardY + 22, 0.48f, 0x888888, false);

            String modeStr = "Hardcore Mode  |  2500x2500 Finite";
            font.drawString(gui, modeStr, textX, cardY + 34, 0.48f, 0xaaaaaa, false);
        } else {
            // Empty state
            String emptyTitle = (currentWorld == null) ? "No Worlds Found" : "No Matching Worlds";
            float e1W = font.getStringWidth(emptyTitle, 0.75f);
            font.drawString(gui, emptyTitle, GuiLayout.centerX(width, (int) e1W), height / 2 - 20, 0.75f, 0xcccccc, true);

            String emptyHint = (currentWorld == null)
                    ? "Click 'Create New World' to begin your adventure."
                    : "Check your search query or clear the filter.";
            float e2W = font.getStringWidth(emptyHint, 0.52f);
            font.drawString(gui, emptyHint, GuiLayout.centerX(width, (int) e2W), height / 2, 0.52f, 0x888888, false);
        }

        // 4. Render Action Buttons & Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int cardW = Math.min(360, width - 24);
        int cardH = 52;
        int cardX = GuiLayout.centerX(width, cardW);
        int cardY = 52;

        if (currentWorld != null && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
            isWorldSelected = true;
            long now = System.currentTimeMillis();
            if (button == 0 && (now - lastCardClickTime) < 300) {
                launchSelectedWorld();
                return true;
            }
            lastCardClickTime = now;
            return true;
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (isPlayable()) {
                    launchSelectedWorld();
                    return;
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (app != null) app.setScreen(new TitleScreen(app));
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
