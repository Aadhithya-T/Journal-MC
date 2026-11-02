package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Rebuilt WorldCreateScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, seed randomization, and world name validation.
 */
public class WorldCreateScreen extends Screen {
    private TextField nameField;
    private TextField seedField;
    private String validationError = null;
    private final Random random = new Random();

    public WorldCreateScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    public TextField getNameField() {
        return nameField;
    }

    public TextField getSeedField() {
        return seedField;
    }

    public String getValidationError() {
        return validationError;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        validationError = null;

        int formW = Math.min(320, width - 24);
        int formX = GuiLayout.centerX(width, formW);
        int startY = Math.max(38, height / 2 - 86);

        // 1. World Name TextField
        nameField = new TextField(1, formX, startY + 14, formW, 20);
        nameField.setPlaceholder("Enter world name...");
        nameField.setMaxLength(28);
        nameField.setText("New Adventure");
        nameField.setOnTextChanged(text -> {
            if (validationError != null && !text.trim().isEmpty()) {
                validationError = null;
            }
        });
        nameField.setOnEnterPressed(this::createWorld);
        addWidget(nameField);
        focusManager.setFocus(nameField);

        // 2. Seed Field & Randomize Button
        int seedBtnW = 60;
        int seedFieldW = formW - seedBtnW - 4;
        seedField = new TextField(2, formX, startY + 50, seedFieldW, 20);
        seedField.setPlaceholder("Leave blank for random seed...");
        seedField.setMaxLength(20);
        seedField.setText(String.valueOf(Math.abs(random.nextLong() % 100000000L)));
        seedField.setOnEnterPressed(this::createWorld);
        addWidget(seedField);

        Button randomSeedBtn = new Button(3, "🎲 Random", formX + seedFieldW + 4, startY + 50, seedBtnW, 20, () -> {
            long newSeed = Math.abs(random.nextLong() % 100000000L);
            seedField.setText(String.valueOf(newSeed));
        });
        randomSeedBtn.setTooltip("Generate a new random seed");
        addWidget(randomSeedBtn);

        // 3. Action Buttons at Bottom
        int btnH = 20;
        int btnGap = 8;
        int btnW = (formW - btnGap) / 2;
        int btnY = height - 32;

        Button createBtn = new Button(4, "Create New World", formX, btnY, btnW, btnH, this::createWorld);
        addWidget(createBtn);

        Button cancelBtn = new Button(5, "Cancel", formX + btnW + btnGap, btnY, btnW, btnH, () -> {
            if (app != null) app.setScreen(new WorldSelectScreen(app));
        });
        addWidget(cancelBtn);
    }

    public void createWorld() {
        String enteredName = (nameField != null) ? nameField.getText().trim() : "";
        if (enteredName.isEmpty()) {
            validationError = "World name cannot be empty!";
            return;
        }

        long finalSeed;
        String seedText = (seedField != null) ? seedField.getText().trim() : "";
        if (seedText.isEmpty()) {
            finalSeed = System.currentTimeMillis() % 100000000L;
        } else {
            try {
                finalSeed = Long.parseLong(seedText);
            } catch (NumberFormatException e) {
                finalSeed = (long) seedText.hashCode();
            }
        }

        if (app != null) {
            app.enterWorld(finalSeed, enteredName, "Multi-Biome", null);
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Background overlay & menu strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.45f);
        gui.drawMenuHeaderFooterStrips(width, height, 32, 42);

        // 2. Header Title
        String title = "Create New World";
        float titleScale = 0.82f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), 10, titleScale, 0xffffff, true);

        // 3. Form section labels & card displays
        int formW = Math.min(320, width - 24);
        int formX = GuiLayout.centerX(width, formW);
        int startY = Math.max(38, height / 2 - 86);

        // World Name Label
        font.drawString(gui, "World Name", formX, startY + 2, 0.52f, 0xaaaaaa, true);

        // Seed Label
        font.drawString(gui, "World Seed", formX, startY + 38, 0.52f, 0xaaaaaa, true);

        // 4. Game Mode & World Info Cards
        int card1Y = startY + 76;
        gui.drawBevelBox(formX, card1Y, formW, 22, 0x18181c, 0x484852, 0x202024);
        font.drawString(gui, "⚔ Game Mode: Hardcore", formX + 8, card1Y + 5, 0.55f, 0xffffff, true);
        font.drawString(gui, "(One Life. Permadeath.)", formX + 130, card1Y + 6, 0.45f, 0x888888, false);

        int card2Y = card1Y + 26;
        gui.drawBevelBox(formX, card2Y, formW, 22, 0x18181c, 0x484852, 0x202024);
        font.drawString(gui, "World Type: Multi-Biome", formX + 8, card2Y + 5, 0.55f, 0xdddddd, true);
        font.drawString(gui, "2500x2500 Finite Bounds", formX + 140, card2Y + 6, 0.45f, 0x888888, false);

        // Validation Error notice if present
        if (validationError != null) {
            float errW = font.getStringWidth(validationError, 0.52f);
            font.drawString(gui, validationError, GuiLayout.centerX(width, (int) errW), card2Y + 30, 0.52f, 0xff4444, true);
        }

        // 5. Render Widgets & Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (app != null) app.setScreen(new WorldSelectScreen(app));
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
