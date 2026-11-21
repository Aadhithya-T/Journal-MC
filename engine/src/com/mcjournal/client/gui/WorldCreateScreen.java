package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * World creation screen using logical coordinates, shared content column geometry,
 * centralized input dispatch, two-line info cards, seed randomization, and validation.
 */
public class WorldCreateScreen extends Screen {
    private TextField nameField;
    private TextField seedField;
    private Button randomSeedBtn;
    private Button createBtn;
    private Button cancelBtn;
    private String validationError = null;
    private final Random random = new Random();

    // Cached layout geometry
    private int formX;
    private int formW;
    private int startY;
    private int cardH;
    private int card1Y;
    private int card2Y;
    private int nameLabelY;
    private int seedLabelY;

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
        String savedName = (nameField != null) ? nameField.getText() : null;
        int savedNameCaret = (nameField != null) ? nameField.getCaretPosition() : 0;
        String savedSeed = (seedField != null) ? seedField.getText() : null;
        int savedSeedCaret = (seedField != null) ? seedField.getCaretPosition() : 0;
        boolean seedWasFocused = (seedField != null && focusManager.getFocusedWidget() == seedField);

        super.init(width, height);
        if (savedName == null) {
            validationError = null;
        }

        // 1. World Name TextField
        nameField = new TextField(1, 0, 0, 100, 20);
        nameField.setPlaceholder("Enter world name...");
        nameField.setMaxLength(28);
        nameField.setText(savedName != null ? savedName : "New Adventure");
        if (savedName != null) {
            nameField.setCaretPosition(savedNameCaret);
        }
        nameField.setOnTextChanged(text -> {
            if (validationError != null && !text.trim().isEmpty()) {
                validationError = null;
            }
        });
        nameField.setOnEnterPressed(this::createWorld);
        addWidget(nameField);

        // 2. Seed Field & Randomize Button
        seedField = new TextField(2, 0, 0, 100, 20);
        seedField.setPlaceholder("Leave blank for random seed...");
        seedField.setMaxLength(20);
        seedField.setText(savedSeed != null ? savedSeed : String.valueOf(Math.abs(random.nextLong() % 100000000L)));
        if (savedSeed != null) {
            seedField.setCaretPosition(savedSeedCaret);
        }
        seedField.setOnEnterPressed(this::createWorld);
        addWidget(seedField);

        randomSeedBtn = new Button(3, "Random", 0, 0, 60, 20, () -> {
            long newSeed = Math.abs(random.nextLong() % 100000000L);
            seedField.setText(String.valueOf(newSeed));
        });
        randomSeedBtn.setTooltip("Generate a new random seed");
        addWidget(randomSeedBtn);

        // 3. Action Buttons at Bottom
        createBtn = new Button(4, "Create New World", 0, 0, 100, GuiLayout.ROW_HEIGHT, this::createWorld);
        addWidget(createBtn);

        cancelBtn = new Button(5, "Cancel", 0, 0, 100, GuiLayout.ROW_HEIGHT, () -> {
            if (app != null) app.setScreen(new WorldSelectScreen(app));
        });
        addWidget(cancelBtn);

        if (seedWasFocused) {
            focusManager.setFocus(seedField);
        } else {
            focusManager.setFocus(nameField);
        }

        layout();
    }

    @Override
    public void layout() {
        // Shared content column
        GuiLayout.Column column = GuiLayout.contentColumn(width, 316, 16);
        this.formX = column.x;
        this.formW = column.width;
        this.startY = Math.max(30, height / 2 - 96);

        // 1. World Name TextField
        this.nameLabelY = startY;
        int nameFieldY = nameLabelY + 12;
        if (nameField != null) {
            nameField.setPosition(formX, nameFieldY);
            nameField.setSize(formW, 20);
        }

        // 2. Seed Field & Randomize Button
        int gutter = GuiLayout.GUTTER;
        int seedBtnW = 60;
        int seedFieldW = formW - seedBtnW - gutter;
        this.seedLabelY = nameFieldY + 22;
        int seedFieldY = seedLabelY + 12;

        if (seedField != null) {
            seedField.setPosition(formX, seedFieldY);
            seedField.setSize(seedFieldW, 20);
        }

        if (randomSeedBtn != null) {
            randomSeedBtn.setPosition(formX + seedFieldW + gutter, seedFieldY);
            randomSeedBtn.setSize(seedBtnW, 20);
        }

        // 3. Two-Line Cards Geometry
        int padY = 5;
        int lineGap = 3;
        int bodyH = 12;
        int captionH = 10;
        this.cardH = padY * 2 + bodyH + lineGap + captionH;
        this.card1Y = seedFieldY + 26;
        this.card2Y = card1Y + cardH + 6;

        // 4. Action Buttons at Bottom
        int btnH = GuiLayout.ROW_HEIGHT;
        int btnW = GuiLayout.buttonWidth(formW, 2, gutter);
        int btnY = height - 28;

        if (createBtn != null) {
            createBtn.setPosition(formX, btnY);
            createBtn.setSize(btnW, btnH);
        }

        int btn2X = formX + btnW + gutter;
        int btn2W = formW - btnW - gutter;
        if (cancelBtn != null) {
            cancelBtn.setPosition(btn2X, btnY);
            cancelBtn.setSize(btn2W, btnH);
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
        font.drawCentered(gui, title, width / 2.0f, GuiLayout.TITLE_Y, TextStyle.TITLE);

        // 3. Form section labels
        font.drawString(gui, "World Name", formX, nameLabelY, TextStyle.BODY.withColor(0xaaaaaa));
        font.drawString(gui, "World Seed", formX, seedLabelY, TextStyle.BODY.withColor(0xaaaaaa));

        // 4. Game Mode & World Info Cards (Vertical two-line cards without horizontal collisions)
        int bodyH = Math.round(font.getLineHeight(TextStyle.BODY));
        int padY = 5;
        int lineGap = 3;

        // Card 1: Game Mode
        gui.drawBevelBox(formX, card1Y, formW, cardH, 0x18181c, 0x484852, 0x202024);
        int c1Line1Y = card1Y + padY;
        int c1Line2Y = c1Line1Y + bodyH + lineGap;
        font.drawString(gui, "Game Mode: Exploration", formX + 8, c1Line1Y, TextStyle.BODY);
        font.drawString(gui, "Building & Adventure", formX + 8, c1Line2Y, TextStyle.CAPTION);

        // Card 2: World Type
        gui.drawBevelBox(formX, card2Y, formW, cardH, 0x18181c, 0x484852, 0x202024);
        int c2Line1Y = card2Y + padY;
        int c2Line2Y = c2Line1Y + bodyH + lineGap;
        font.drawString(gui, "World Type: Multi-Biome", formX + 8, c2Line1Y, TextStyle.BODY);
        font.drawString(gui, "2500 × 2500 · Finite bounds", formX + 8, c2Line2Y, TextStyle.CAPTION);

        // Validation Error notice if present
        if (validationError != null) {
            font.drawCentered(gui, validationError, width / 2.0f, card2Y + cardH + 6, TextStyle.BODY, 0xff4444);
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
    public int getFormX() { return formX; }
    public int getFormW() { return formW; }
    public int getNameLabelY() { return nameLabelY; }
    public int getSeedLabelY() { return seedLabelY; }
    public int getCard1Y() { return card1Y; }
    public int getCard2Y() { return card2Y; }
    public int getCardH() { return cardH; }
}