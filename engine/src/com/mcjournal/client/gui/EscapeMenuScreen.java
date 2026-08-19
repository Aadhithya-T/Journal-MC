package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class EscapeMenuScreen extends Screen {
    public EscapeMenuScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 320;
        int btnH = 40;
        int centerX = (width - btnW) / 2;
        int startY = height / 2 - 38;

        // 1. Back to Game (Resume)
        buttons.add(new Button(1, "Back to Game", centerX, startY, btnW, btnH, () -> {
            app.resumeGame();
        }));

        // 2. Options / Journal Row (Split 2-column)
        int splitW = (btnW - 8) / 2; // 156px each
        buttons.add(new Button(2, "Journal & Stats", centerX, startY + 48, splitW, btnH, () -> {
            // Journal stats view
        }));

        buttons.add(new Button(3, "Options...", centerX + splitW + 8, startY + 48, splitW, btnH, () -> {
            // Options view
        }));

        // 3. Save and Quit to Title
        buttons.add(new Button(4, "Save and Quit to Title", centerX, startY + 96, btnW, btnH, () -> {
            app.saveAndQuitToTitle();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Semi-transparent darkened vignette overlay over the frozen 3D world
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);

        // Header Title
        String title = "Game Menu";
        float titleW = font.getStringWidth(title, 1.40f);
        font.drawString(gui, title, (width - titleW) / 2.0f, height / 2 - 82, 1.40f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        // Pressing ESC again inside the escape menu resumes the game!
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            app.resumeGame();
        }
    }
}
