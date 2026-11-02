package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class EscapeMenuScreen extends Screen {
    public EscapeMenuScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 200;
        int btnH = 20;
        int centerX = GuiLayout.centerX(width, btnW);
        int startY = height / 2 - 25;

        // 1. Back to Game (Resume)
        buttons.add(new Button(1, "Back to Game", centerX, startY, btnW, btnH, () -> {
            if (app != null) app.resumeGame();
        }));

        // 2. Options / Journal Row (Split 2-column)
        int splitW = GuiLayout.splitWidth(btnW, 4, 2); // 98px each
        buttons.add(new Button(2, "Journal & Stats", centerX, startY + 24, splitW, btnH, () -> {
            // Journal stats view
        }));

        buttons.add(new Button(3, "Options...", centerX + splitW + 4, startY + 24, splitW, btnH, () -> {
            if (app != null) app.setScreen(new OptionsScreen(app, this));
        }));

        // 3. Save and Quit to Title
        buttons.add(new Button(4, "Save and Quit to Title", centerX, startY + 48, btnW, btnH, () -> {
            if (app != null) app.saveAndQuitToTitle();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Semi-transparent darkened vignette overlay over the frozen 3D world
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);

        // Header Title
        String title = "Game Menu";
        float titleW = font.getStringWidth(title, 0.70f);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), height / 2 - 48, 0.70f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (app != null) app.resumeGame();
        }
    }
}
