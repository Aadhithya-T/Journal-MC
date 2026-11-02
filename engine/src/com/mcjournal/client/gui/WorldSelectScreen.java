package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;
import org.lwjgl.glfw.GLFW;

/**
 * World selection screen using logical coordinates, shared content column geometry,
 * centralized input dispatch, and safe confirmation dialogs.
 */
public class WorldSelectScreen extends Screen {
    private WorldSaveManager.SavedWorld currentWorld;
    private boolean isWorldSelected = true;
    private TextField searchField;
    private long lastCardClickTime = 0;

    // Cached layout geometry computed in init() and shared across render() and mouseClicked()
    private int colX;
    private int colW;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;

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

        // Single shared content column for search, world card, and both button rows
        GuiLayout.Column column = GuiLayout.contentColumn(width, 316, 16);
        this.colX = column.x;
        this.colW = column.width;

        // 1. Header Zone: Reserved height ensuring title and search box never collide
        // Title at y=8; search field cleanly separated below title
        int searchY = 30;
        int searchH = 18;
        searchField = new TextField(1, colX, searchY, colW, searchH);
        searchField.setPlaceholder("Search worlds...");
        addWidget(searchField);

        // 2. World Card Geometry (stored once for render and mouse interaction)
        this.cardX = colX;
        this.cardY = 52;
        this.cardW = colW;
        this.cardH = 52;

        // 3. Action Buttons at Bottom (Two uniform rows sharing one column and identical gutters)
        int btnH = GuiLayout.ROW_HEIGHT;
        int gutter = GuiLayout.GUTTER;
        int row2Y = height - GuiLayout.BOTTOM_MARGIN;
        int row1Y = row2Y - btnH - gutter;

        // --- ROW 1 (Play World | Create New World) ---
        GuiLayout.ButtonSlot[] r1 = GuiLayout.distributeRow(colX, colW, 2, gutter);
        Button playBtn = new Button(2, "Play World", r1[0].x(), row1Y, r1[0].width(), btnH, this::launchSelectedWorld);
        playBtn.setEnabled(isPlayable());
        addWidget(playBtn);

        Button createBtn = new Button(3, "Create New World", r1[1].x(), row1Y, r1[1].width(), btnH, () -> {
            if (app != null) app.setScreen(new WorldCreateScreen(app));
        });
        addWidget(createBtn);

        // --- ROW 2 (Edit | Delete | Cancel) ---
        GuiLayout.ButtonSlot[] r2 = GuiLayout.distributeRow(colX, colW, 3, gutter);

        Button editBtn = new Button(4, "Edit", r2[0].x(), row2Y, r2[0].width(), btnH, () -> {
            if (currentWorld != null && app != null) {
                app.setScreen(new WorldEditScreen(app, currentWorld));
            }
        });
        editBtn.setEnabled(currentWorld != null);
        addWidget(editBtn);

        Button deleteBtn = new Button(5, "Delete", r2[1].x(), row2Y, r2[1].width(), btnH, this::promptDeleteWorld);
        deleteBtn.setEnabled(currentWorld != null);
        addWidget(deleteBtn);

        Button cancelBtn = new Button(6, "Cancel", r2[2].x(), row2Y, r2[2].width(), btnH, () -> {
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
        gui.drawMenuHeaderFooterStrips(width, height, 48, 56);

        // 2. Header Title
        String title = "Select World";
        font.drawCentered(gui, title, width / 2.0f, 8, TextStyle.TITLE);

        // 3. World List Card (using cached layout geometry)
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
                font.drawString(gui, "▶", iconX + 12, iconY + 8, TextStyle.TITLE);
            }

            // Authentic metadata extracted from currentWorld
            int textX = iconX + iconSize + 10;
            String worldName = currentWorld.name != null ? currentWorld.name : "Hardcore World";
            font.drawString(gui, worldName, textX, cardY + 8, TextStyle.BODY);

            String createdDate = (currentWorld.createdAt != null && !currentWorld.createdAt.isEmpty())
                    ? currentWorld.createdAt
                    : "Saved";
            String timeStr = worldName + " (" + createdDate + ")";
            font.drawString(gui, timeStr, textX, cardY + 22, TextStyle.CAPTION);

            int wW = currentWorld.worldWidth > 0 ? currentWorld.worldWidth : 2500;
            int wD = currentWorld.worldDepth > 0 ? currentWorld.worldDepth : 2500;
            String modeStr = "Hardcore Mode · " + wW + " × " + wD + " Finite";
            font.drawString(gui, modeStr, textX, cardY + 34, TextStyle.CAPTION.withColor(0xaaaaaa));
        } else {
            // Empty state
            String emptyTitle = (currentWorld == null) ? "No Worlds Found" : "No Matching Worlds";
            font.drawCentered(gui, emptyTitle, width / 2.0f, height / 2 - 20, TextStyle.TITLE.withColor(0xcccccc));

            String emptyHint = (currentWorld == null)
                    ? "Click 'Create New World' to begin your adventure."
                    : "Check your search query or clear the filter.";
            font.drawCentered(gui, emptyHint, width / 2.0f, height / 2, TextStyle.CAPTION);
        }

        // 4. Render Action Buttons & Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Reuse cached geometry
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
    public int getColX() { return colX; }
    public int getColW() { return colW; }
    public int getCardX() { return cardX; }
    public int getCardY() { return cardY; }
    public int getCardW() { return cardW; }
    public int getCardH() { return cardH; }
}