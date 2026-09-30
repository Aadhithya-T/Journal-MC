package com.mcjournal.client.gui;

import com.mcjournal.client.GameSettings;
import com.mcjournal.client.MCJournalApp;

public class OptionsScreen extends Screen {
    private final Screen parentScreen;

    public OptionsScreen(MCJournalApp app, Screen parentScreen) {
        super(app);
        this.parentScreen = parentScreen;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        buttons.clear();

        int btnW = 190;
        int btnH = 40;
        int gap = 16;
        int totalRowW = btnW * 2 + gap; // 396px
        int startX = (width - totalRowW) / 2;
        int startY = height / 2 - 80;

        GameSettings settings = app.getSettings();

        // Row 1: FOV & Max Framerate (FPS limit)
        buttons.add(new Button(1, "FOV: " + (int) settings.fov + "°", startX, startY, btnW, btnH, () -> {
            settings.cycleFov();
            app.getCamera().setFov(settings.fov);
            app.getCamera().updateProjection(app.getWindow().getAspectRatio());
            init(width, height);
        }));

        buttons.add(new Button(2, "Max FPS: " + settings.getFpsDisplayString(), startX + btnW + gap, startY, btnW, btnH, () -> {
            settings.cycleFpsLimit();
            app.applySettings();
            init(width, height);
        }));

        // Row 2: Render Distance & Show FPS
        buttons.add(new Button(3, "Render Dist: " + settings.renderDistance + " Chunks", startX, startY + 48, btnW, btnH, () -> {
            settings.cycleRenderDistance();
            app.applySettings();
            init(width, height);
        }));

        buttons.add(new Button(4, "Show FPS: " + (settings.showFps ? "ON" : "OFF"), startX + btnW + gap, startY + 48, btnW, btnH, () -> {
            settings.toggleShowFps();
            init(width, height);
        }));

        // Row 3: Fog Quality
        buttons.add(new Button(5, "Fog: " + settings.getFogModeString(), (width - btnW) / 2, startY + 96, btnW, btnH, () -> {
            settings.cycleFogMode();
            init(width, height);
        }));

        // Done Button (Centered bottom)
        int doneW = 320;
        buttons.add(new Button(10, "Done", (width - doneW) / 2, startY + 160, doneW, btnH, () -> {
            settings.save();
            app.applySettings();
            app.setScreen(parentScreen);
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Darkened background vignette
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);

        // Title
        String title = "Video Settings & Options";
        float titleW = font.getStringWidth(title, 1.30f);
        font.drawString(gui, title, (width - titleW) / 2.0f, height / 2 - 125, 1.30f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            app.getSettings().save();
            app.applySettings();
            app.setScreen(parentScreen);
        }
    }
}
