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
    public boolean usesLogicalCoordinates() {
        return true;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        buttons.clear();

        int btnW = 150;
        int btnH = 20;
        int gap = 8;
        int totalRowW = btnW * 2 + gap; // 308px
        int startX = GuiLayout.centerX(width, totalRowW);
        int startY = height / 2 - 45;

        GameSettings settings = (app != null) ? app.getSettings() : new GameSettings();

        // Row 1: FOV & Max Framerate (FPS limit)
        buttons.add(new Button(1, "FOV: " + (int) settings.fov + "°", startX, startY, btnW, btnH, () -> {
            settings.cycleFov();
            if (app != null && app.getCamera() != null) app.getCamera().setFov(settings.fov);
            if (app != null && app.getCamera() != null && app.getWindow() != null) app.getCamera().updateProjection(app.getWindow().getAspectRatio());
            init(width, height);
        }));

        buttons.add(new Button(2, "Max FPS: " + settings.getFpsDisplayString(), startX + btnW + gap, startY, btnW, btnH, () -> {
            settings.cycleFpsLimit();
            if (app != null) app.applySettings();
            init(width, height);
        }));

        // Row 2: Render Distance & Show FPS
        buttons.add(new Button(3, "Render Dist: " + settings.renderDistance + " Chunks", startX, startY + 24, btnW, btnH, () -> {
            settings.cycleRenderDistance();
            if (app != null) app.applySettings();
            init(width, height);
        }));

        buttons.add(new Button(4, "Show FPS: " + (settings.showFps ? "ON" : "OFF"), startX + btnW + gap, startY + 24, btnW, btnH, () -> {
            settings.toggleShowFps();
            init(width, height);
        }));

        // Row 3: Fog Quality & GUI Scale
        buttons.add(new Button(5, "Fog: " + settings.getFogModeString(), startX, startY + 48, btnW, btnH, () -> {
            settings.cycleFogMode();
            init(width, height);
        }));

        buttons.add(new Button(6, "GUI Scale: " + settings.getGuiScaleDisplayString(), startX + btnW + gap, startY + 48, btnW, btnH, () -> {
            settings.cycleGuiScale();
            if (app != null) app.applySettings();
            init(width, height);
        }));

        // Done Button (Centered bottom)
        int doneW = 200;
        int doneX = GuiLayout.centerX(width, doneW);
        buttons.add(new Button(10, "Done", doneX, startY + 76, doneW, btnH, () -> {
            settings.save();
            if (app != null) app.applySettings();
            if (app != null) app.setScreen(parentScreen);
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Darkened background vignette
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);

        // Title
        String title = "Video Settings & Options";
        float titleW = font.getStringWidth(title, 0.70f);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), height / 2 - 68, 0.70f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (app != null) app.getSettings().save();
            if (app != null) app.applySettings();
            if (app != null) app.setScreen(parentScreen);
        }
    }
}
