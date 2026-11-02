package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;
import org.lwjgl.glfw.GLFW;

/**
 * WorldEditScreen using centralized logical coordinates, unified column layout,
 * standardized TextStyle typography, and safe confirmation dialogs.
 */
public class WorldEditScreen extends Screen {
    private final WorldSaveManager.SavedWorld world;
    private TextField nameField;

    private int formX;
    private int formW;
    private int nameLabelY;
    private int cardY;
    private int cardH;

    public WorldEditScreen(MCJournalApp app, WorldSaveManager.SavedWorld world) {
        super(app);
        this.world = world;
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    public WorldSaveManager.SavedWorld getWorld() {
        return world;
    }

    public TextField getNameField() {
        return nameField;
    }

    public String getWorldName() {
        return (nameField != null) ? nameField.getText() : (world != null ? world.name : "New Adventure");
    }

    public int getFormX() { return formX; }
    public int getFormW() { return formW; }
    public int getNameLabelY() { return nameLabelY; }
    public int getCardY() { return cardY; }
    public int getCardH() { return cardH; }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        FontRenderer fr = getFont();

        GuiLayout.Column column = GuiLayout.contentColumn(width, 316, 16);
        this.formX = column.x;
        this.formW = column.width;

        this.nameLabelY = Math.max(30, height / 2 - 76);
        int nameFieldY = nameLabelY + 12;

        // 1. World Name TextField
        nameField = new TextField(1, formX, nameFieldY, formW, GuiLayout.ROW_HEIGHT);
        nameField.setPlaceholder("Enter world name...");
        nameField.setMaxLength(28);
        String initialName = (world != null && world.name != null) ? world.name : "New Adventure";
        nameField.setText(initialName);
        nameField.setOnEnterPressed(this::saveAndExit);
        addWidget(nameField);
        focusManager.setFocus(nameField);

        // 2. Info Card Geometry
        int padY = 6;
        int bodyH = Math.round(fr.getLineHeight(TextStyle.BODY));
        int capH = Math.round(fr.getLineHeight(TextStyle.CAPTION));
        this.cardH = padY * 2 + bodyH + 4 + capH + 4 + capH;
        this.cardY = nameFieldY + GuiLayout.ROW_HEIGHT + 14;

        // 3. Action Buttons at Bottom (Save | Delete | Cancel)
        int btnH = GuiLayout.ROW_HEIGHT;
        int btnY = height - GuiLayout.BOTTOM_MARGIN;
        GuiLayout.ButtonSlot[] slots = GuiLayout.distributeRow(formX, formW, 3, GuiLayout.GUTTER);

        Button saveBtn = new Button(2, "Save", slots[0].x(), btnY, slots[0].width(), btnH, this::saveAndExit);
        addWidget(saveBtn);

        Button deleteBtn = new Button(3, "Delete", slots[1].x(), btnY, slots[1].width(), btnH, this::promptDeleteWorld);
        addWidget(deleteBtn);

        Button cancelBtn = new Button(4, "Cancel", slots[2].x(), btnY, slots[2].width(), btnH, this::cancelAndExit);
        addWidget(cancelBtn);
    }

    public void saveAndExit() {
        String enteredName = (nameField != null) ? nameField.getText().trim() : "";
        String finalName = enteredName.isEmpty() ? "New Adventure" : enteredName;
        WorldSaveManager.renameWorld(finalName);
        if (app != null) {
            app.setScreen(new WorldSelectScreen(app));
        }
    }

    public void promptDeleteWorld() {
        String targetName = (world != null && world.name != null) ? world.name : "this world";
        String msg = "Are you sure you want to delete '" + targetName + "'?\nThis action cannot be undone!";

        ConfirmScreen confirm = new ConfirmScreen(app, "Delete World?", msg, "Delete", "Cancel", true,
                () -> {
                    WorldSaveManager.deleteWorld();
                    if (app != null) {
                        app.setScreen(new WorldSelectScreen(app));
                    }
                },
                () -> {
                    if (app != null) {
                        app.popScreen();
                    }
                });

        if (app != null) {
            app.pushScreen(confirm);
        }
    }

    public void cancelAndExit() {
        if (app != null) {
            app.setScreen(new WorldSelectScreen(app));
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Background overlay & menu strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.45f);
        gui.drawMenuHeaderFooterStrips(width, height, 32, 42);

        // 2. Header Title
        font.drawCentered(gui, "Edit World", width / 2.0f, GuiLayout.TITLE_Y, TextStyle.TITLE);

        // 3. Section Label
        font.drawString(gui, "World Name", formX, nameLabelY, TextStyle.BODY.withColor(0xaaaaaa));

        // 4. World Info Card
        if (world != null) {
            int padY = 6;
            int bodyH = Math.round(font.getLineHeight(TextStyle.BODY));
            int capH = Math.round(font.getLineHeight(TextStyle.CAPTION));

            gui.drawBevelBox(formX, cardY, formW, cardH, 0x18181c, 0x484852, 0x202024);

            String biome = world.biome != null ? world.biome : "Multi-Biome";
            String modeStr = "Game Mode: Hardcore (" + biome + ")";
            font.drawString(gui, modeStr, formX + 8, cardY + padY, TextStyle.BODY);

            String createdDate = (world.createdAt != null && !world.createdAt.isEmpty()) ? world.createdAt : "Saved";
            String seedStr = "Seed: " + world.seed + " · Created: " + createdDate;
            font.drawString(gui, seedStr, formX + 8, cardY + padY + bodyH + 4, TextStyle.CAPTION);

            int wW = world.worldWidth > 0 ? world.worldWidth : 2500;
            int wD = world.worldDepth > 0 ? world.worldDepth : 2500;
            int wH = world.worldHeight > 0 ? world.worldHeight : 256;
            String boundsStr = "Bounds: " + wW + " × " + wD + " × " + wH + " (Finite)";
            font.drawString(gui, boundsStr, formX + 8, cardY + padY + bodyH + 4 + capH + 4, TextStyle.CAPTION.withColor(0x777777));
        }

        // 5. Render Widgets & Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelAndExit();
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
