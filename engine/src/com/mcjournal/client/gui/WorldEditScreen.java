package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt WorldEditScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, and safe confirmation dialog for world deletion.
 */
public class WorldEditScreen extends Screen {
    private final WorldSaveManager.SavedWorld world;
    private TextField nameField;

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

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int formW = Math.min(320, width - 24);
        int formX = GuiLayout.centerX(width, formW);
        int startY = Math.max(48, height / 2 - 70);

        // 1. World Name TextField
        nameField = new TextField(1, formX, startY + 16, formW, 22);
        nameField.setPlaceholder("Enter world name...");
        nameField.setMaxLength(28);
        String initialName = (world != null && world.name != null) ? world.name : "New Adventure";
        nameField.setText(initialName);
        nameField.setOnEnterPressed(this::saveAndExit);
        addWidget(nameField);
        focusManager.setFocus(nameField);

        // 2. Action Buttons at Bottom
        int btnH = 20;
        int btnGap = 6;
        int btnY = height - 32;

        // Save (Left) | Delete (Middle) | Cancel (Right)
        int btnW = (formW - btnGap * 2) / 3;
        int saveX = formX;
        int deleteX = saveX + btnW + btnGap;
        int cancelX = deleteX + btnW + btnGap;

        Button saveBtn = new Button(2, "Save", saveX, btnY, btnW, btnH, this::saveAndExit);
        addWidget(saveBtn);

        Button deleteBtn = new Button(3, "Delete", deleteX, btnY, btnW, btnH, this::promptDeleteWorld);
        addWidget(deleteBtn);

        Button cancelBtn = new Button(4, "Cancel", cancelX, btnY, btnW, btnH, this::cancelAndExit);
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
        gui.drawMenuHeaderFooterStrips(width, height, 36, 44);

        // 2. Header Title
        String title = "Edit World";
        float titleScale = 0.85f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), 12, titleScale, 0xffffff, true);

        // 3. Form section
        int formW = Math.min(320, width - 24);
        int formX = GuiLayout.centerX(width, formW);
        int startY = Math.max(48, height / 2 - 70);

        // Section Label
        font.drawString(gui, "World Name", formX, startY + 2, 0.55f, 0xaaaaaa, true);

        // 4. World Info Card
        if (world != null) {
            int cardY = startY + 46;
            int cardH = 50;
            gui.drawBevelBox(formX, cardY, formW, cardH, 0x18181c, 0x484852, 0x202024);

            String modeStr = "⚔ Mode: Hardcore (" + world.biome + ")";
            font.drawString(gui, modeStr, formX + 8, cardY + 8, 0.55f, 0xdddddd, true);

            String seedStr = "Seed: " + world.seed;
            if (world.createdAt != null && !world.createdAt.isEmpty()) {
                seedStr += "  |  Created: " + world.createdAt;
            }
            font.drawString(gui, seedStr, formX + 8, cardY + 22, 0.48f, 0x888888, false);

            String boundsStr = "Bounds: " + world.worldWidth + "x" + world.worldDepth + "x" + world.worldHeight + " (Finite)";
            font.drawString(gui, boundsStr, formX + 8, cardY + 34, 0.48f, 0x777777, false);
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
