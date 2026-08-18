package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;

public class EscapeMenuScreen extends Screen {
    private final String[] texturePacks = {
        "Faithful 64x (HD)",
        "Stylized Clean (Bare)",
        "Classic Retro 16x"
    };
    private int packIndex = 0;

    public EscapeMenuScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 340;
        int btnH = 44;
        int centerX = (width - btnW) / 2;
        int startY = height / 2 - 60;

        // 1. Back to Game (Resume)
        buttons.add(new Button(1, "Back to Game", centerX, startY, btnW, btnH, () -> {
            app.resumeGame();
        }));

        // 2. Texture Pack Switcher
        buttons.add(new Button(2, "Texture Pack: " + texturePacks[packIndex], centerX, startY + 54, btnW, btnH, () -> {
            packIndex = (packIndex + 1) % texturePacks.length;
            buttons.get(1).setText("Texture Pack: " + texturePacks[packIndex]);
            app.getAtlas().switchPack(packIndex);
        }));

        // 3. Save and Quit to Title
        buttons.add(new Button(3, "Save and Quit to Title", centerX, startY + 115, btnW, btnH, () -> {
            app.saveAndQuitToTitle();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Semi-transparent darkened vignette overlay over the frozen 3D world
        gui.drawRect(0, 0, width, height, 0.05f, 0.05f, 0.07f, 0.75f);

        // Header Title Box
        String title = "Game Menu";
        float titleW = font.getStringWidth(title, 1.6f);
        font.drawString(gui, title, (width - titleW) / 2.0f, height / 2 - 120, 1.6f, 0xffffff, true);

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
