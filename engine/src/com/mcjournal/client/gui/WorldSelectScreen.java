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
    private Button playBtn;
    private Button createBtn;
    private Button editBtn;
    private Button deleteBtn;
    private Button cancelBtn;
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
        String savedQuery = (searchField != null) ? searchField.getText() : null;
        int savedCaret = (searchField != null) ? searchField.getCaretPosition() : 0;
        boolean wasFocused = (searchField != null && focusManager.getFocusedWidget() == searchField);

        super.init(width, height);
        this.currentWorld = WorldSaveManager.loadWorld();
        this.isWorldSelected = (currentWorld != null);

        // 1. Search Field
        searchField = new TextField(1, 0, 0, 100, 18);
        searchField.setPlaceholder("Search worlds...");
        if (savedQuery != null) {
            searchField.setText(savedQuery);
            searchField.setCaretPosition(savedCaret);
        }
        addWidget(searchField);
        if (wasFocused) {
            focusManager.setFocus(searchField);
        }

        // 2. Action Buttons
        playBtn = new Button(2, "Play World", 0, 0, 100, GuiLayout.ROW_HEIGHT, this::launchSelectedWorld);
        addWidget(playBtn);

        createBtn = new Button(3, "Create New World", 0, 0, 100, GuiLayout.ROW_HEIGHT, () -> {
            if (app != null) app.setScreen(new WorldCreateScreen(app));
        });
        addWidget(createBtn);

        editBtn = new Button(4, "Edit", 0, 0, 100, GuiLayout.ROW_HEIGHT, () -> {
            if (currentWorld != null && app != null) {
                app.setScreen(new WorldEditScreen(app, currentWorld));
            }
        });
        addWidget(editBtn);

        deleteBtn = new Button(5, "Delete", 0, 0, 100, GuiLayout.ROW_HEIGHT, this::promptDeleteWorld);
        addWidget(deleteBtn);

        cancelBtn = new Button(6, "Cancel", 0, 0, 100, GuiLayout.ROW_HEIGHT, () -> {
            if (app != null) app.setScreen(new TitleScreen(app));
        });
        addWidget(cancelBtn);

        layout();
    }

    @Override
    public void layout() {
        // Single shared content column for search, world card, and both button rows
        GuiLayout.Column column = GuiLayout.contentColumn(width, 316, 16);
        this.colX = column.x;
        this.colW = column.width;

        // 1. Header Zone: Reserved height ensuring title and search box never collide
        int searchY = 30;
        int searchH = 18;
        if (searchField != null) {
            searchField.setPosition(colX, searchY);
            searchField.setSize(colW, searchH);
        }

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
        if (playBtn != null) {
            playBtn.setPosition(r1[0].x(), row1Y);
            playBtn.setSize(r1[0].width(), btnH);
            playBtn.setEnabled(isPlayable());
        }

        if (createBtn != null) {
            createBtn.setPosition(r1[1].x(), row1Y);
            createBtn.setSize(r1[1].width(), btnH);
        }

        // --- ROW 2 (Edit | Delete | Cancel) ---
        GuiLayout.ButtonSlot[] r2 = GuiLayout.distributeRow(colX, colW, 3, gutter);
        if (editBtn != null) {
            editBtn.setPosition(r2[0].x(), row2Y);
            editBtn.setSize(r2[0].width(), btnH);
            editBtn.setEnabled(currentWorld != null);
        }

        if (deleteBtn != null) {
            deleteBtn.setPosition(r2[1].x(), row2Y);
            deleteBtn.setSize(r2[1].width(), btnH);
            deleteBtn.setEnabled(currentWorld != null);
        }

        if (cancelBtn != null) {
            cancelBtn.setPosition(r2[2].x(), row2Y);
            cancelBtn.setSize(r2[2].width(), btnH);
        }
    }

    @Override
    public void onResize(int width, int height) {
        this.width = width;
        this.height = height;
        if (!isInitialized) {
            init(width, height, font);
        } else {
            layout();
        }
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
        String prompt = "Are you sure you want to delete\n'" + worldName + "'?\nThis action cannot be undone!";

        ConfirmScreen confirm = new ConfirmScreen(app, "Delete World?", prompt, "Delete", "Cancel", true,
                () -> {
                    WorldSaveManager.deleteWorld();
                    currentWorld = null;
                    if (app != null) {
                        app.setScreen(new WorldSelectScreen(app));
                    }
                },
                () -> {
                    if (app != null) app.popScreen();
                });

        if (app != null) {
            app.pushScreen(confirm);
        }
    }

    @Override
    public void onReactivated() {
        super.onReactivated();
        init(width, height);
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
            String worldName = currentWorld.name != null ? currentWorld.name : "World";
            font.drawString(gui, worldName, textX, cardY + 8, TextStyle.BODY);

            String createdDate = (currentWorld.createdAt != null && !currentWorld.createdAt.isEmpty())
                    ? currentWorld.createdAt
                    : "Saved";
            String timeStr = worldName + " (" + createdDate + ")";
            font.drawString(gui, timeStr, textX, cardY + 22, TextStyle.CAPTION);

            int wW = currentWorld.worldWidth > 0 ? currentWorld.worldWidth : 2500;
            int wD = currentWorld.worldDepth > 0 ? currentWorld.worldDepth : 2500;
            String modeStr = wW + " × " + wD + " · Finite World";
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