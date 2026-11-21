package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt EscapeMenuScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, and screen navigation to Journal & Stats.
 */
public class EscapeMenuScreen extends Screen {
    private Button resumeBtn;
    private Button journalBtn;
    private Button optionsBtn;
    private Button quitBtn;

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

        // 1. Back to Game (Resume)
        resumeBtn = new Button(1, "Back to Game", 0, 0, 200, 20, () -> {
            if (app != null) app.resumeGame();
        });
        addWidget(resumeBtn);
        focusManager.setFocus(resumeBtn);

        // 2. Journal & Stats
        journalBtn = new Button(2, "Journal & Stats", 0, 0, 200, 20, () -> {
            if (app != null) app.setScreen(new JournalStatsScreen(app, this));
        });
        addWidget(journalBtn);

        // 3. Options...
        optionsBtn = new Button(3, "Options...", 0, 0, 200, 20, () -> {
            if (app != null) app.setScreen(new OptionsScreen(app, this));
        });
        addWidget(optionsBtn);

        // 4. Save and Quit to Title
        quitBtn = new Button(4, "Save and Quit to Title", 0, 0, 200, 20, () -> {
            if (app != null) app.saveAndQuitToTitle();
        });
        addWidget(quitBtn);

        layout();
    }

    @Override
    public void layout() {
        int btnW = Math.min(200, width - 24);
        int btnH = 20;
        int centerX = GuiLayout.centerX(width, btnW);
        int startY = Math.max(50, height / 2 - 44);

        if (resumeBtn != null) {
            resumeBtn.setPosition(centerX, startY);
            resumeBtn.setSize(btnW, btnH);
        }
        if (journalBtn != null) {
            journalBtn.setPosition(centerX, startY + 24);
            journalBtn.setSize(btnW, btnH);
        }
        if (optionsBtn != null) {
            optionsBtn.setPosition(centerX, startY + 48);
            optionsBtn.setSize(btnW, btnH);
        }
        if (quitBtn != null) {
            quitBtn.setPosition(centerX, startY + 72);
            quitBtn.setSize(btnW, btnH);
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
