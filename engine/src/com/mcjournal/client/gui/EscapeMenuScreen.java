package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt EscapeMenuScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, and screen navigation to Journal & Stats.
 */
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

        int btnW = Math.min(200, width - 24);
        int btnH = 20;
        int centerX = GuiLayout.centerX(width, btnW);
        int startY = Math.max(50, height / 2 - 44);

        // 1. Back to Game (Resume)
        Button resumeBtn = new Button(1, "Back to Game", centerX, startY, btnW, btnH, () -> {
            if (app != null) app.resumeGame();
        });
        addWidget(resumeBtn);
        focusManager.setFocus(resumeBtn);

        // 2. Journal & Stats
        Button journalBtn = new Button(2, "Journal & Stats", centerX, startY + 24, btnW, btnH, () -> {
            if (app != null) app.setScreen(new JournalStatsScreen(app, this));
        });
        addWidget(journalBtn);

        // 3. Options...
        Button optionsBtn = new Button(3, "Options...", centerX, startY + 48, btnW, btnH, () -> {
            if (app != null) app.setScreen(new OptionsScreen(app, this));
        });
        addWidget(optionsBtn);

        // 4. Save and Quit to Title
        Button quitBtn = new Button(4, "Save and Quit to Title", centerX, startY + 72, btnW, btnH, () -> {
            if (app != null) app.saveAndQuitToTitle();
        });
        addWidget(quitBtn);
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Semi-transparent darkened vignette overlay over the frozen 3D world
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);

        // Header Title
        String title = "Game Menu";
        float titleScale = 0.85f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), Math.max(20, height / 2 - 68), titleScale, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (app != null) app.resumeGame();
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
