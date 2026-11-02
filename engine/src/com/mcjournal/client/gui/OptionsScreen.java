package com.mcjournal.client.gui;

import com.mcjournal.client.GameSettings;
import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt OptionsScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, and structured Video & Controls settings.
 */
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

    public Screen getParentScreen() {
        return parentScreen;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 150;
        int btnH = 20;
        int gap = 8;
        int totalRowW = btnW * 2 + gap; // 308px
        int startX = GuiLayout.centerX(width, totalRowW);
        int startY = Math.max(34, height / 2 - 80);

        GameSettings settings = (app != null) ? app.getSettings() : new GameSettings();

        // --- ROW 1: FOV & Max FPS ---
        Button fovBtn = new Button(1, "FOV: " + (int) settings.fov + "°", startX, startY, btnW, btnH, () -> {
            settings.cycleFov();
            if (app != null && app.getCamera() != null) app.getCamera().setFov(settings.fov);
            if (app != null && app.getCamera() != null && app.getWindow() != null) {
                app.getCamera().updateProjection(app.getWindow().getAspectRatio());
            }
            init(width, height);
        });
        addWidget(fovBtn);

        Button fpsBtn = new Button(2, "Max FPS: " + settings.getFpsDisplayString(), startX + btnW + gap, startY, btnW, btnH, () -> {
            settings.cycleFpsLimit();
            if (app != null) app.applySettings();
            init(width, height);
        });
        addWidget(fpsBtn);

        // --- ROW 2: Render Distance & Show FPS ---
        Button renderDistBtn = new Button(3, "Render Dist: " + settings.renderDistance + " Chunks", startX, startY + 24, btnW, btnH, () -> {
            settings.cycleRenderDistance();
            if (app != null) app.applySettings();
            init(width, height);
        });
        addWidget(renderDistBtn);

        Button showFpsBtn = new Button(4, "Show FPS: " + (settings.showFps ? "ON" : "OFF"), startX + btnW + gap, startY + 24, btnW, btnH, () -> {
            settings.toggleShowFps();
            init(width, height);
        });
        addWidget(showFpsBtn);

        // --- ROW 3: Fog Mode & GUI Scale ---
        Button fogBtn = new Button(5, "Fog: " + settings.getFogModeString(), startX, startY + 48, btnW, btnH, () -> {
            settings.cycleFogMode();
            init(width, height);
        });
        addWidget(fogBtn);

        Button guiScaleBtn = new Button(6, "GUI Scale: " + settings.getGuiScaleDisplayString(), startX + btnW + gap, startY + 48, btnW, btnH, () -> {
            settings.cycleGuiScale();
            if (app != null) app.applySettings();
            init(width, height);
        });
        addWidget(guiScaleBtn);

        // --- Done Button (Bottom Center) ---
        int doneW = 200;
        int doneX = GuiLayout.centerX(width, doneW);
        Button doneBtn = new Button(10, "Done", doneX, height - 30, doneW, btnH, this::saveAndExit);
        addWidget(doneBtn);
        focusManager.setFocus(doneBtn);
    }

    public void saveAndExit() {
        if (app != null) {
            app.getSettings().save();
            app.applySettings();
            if (parentScreen != null) {
                app.setScreen(parentScreen);
            } else {
                app.popScreen();
            }
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Dark vignette background & header/footer strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.65f);
        gui.drawMenuHeaderFooterStrips(width, height, 30, 42);

        // 2. Header Title
        String title = "Video Settings & Options";
        float titleScale = 0.85f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), 8, titleScale, 0xffffff, true);

        // 3. Controls Overview Card (Positioned below buttons)
        int cardW = 308;
        int cardX = GuiLayout.centerX(width, cardW);
        int cardY = Math.max(34, height / 2 - 80) + 74;
        int cardH = 38;

        gui.drawBevelBox(cardX, cardY, cardW, cardH, 0x18181c, 0x484852, 0x202024);
        font.drawString(gui, "§7Move:§r WASD  |  §7Jump:§r Space  |  §7Sneak:§r L-Shift", cardX + 8, cardY + 8, 0.48f, 0xcccccc, false);
        font.drawString(gui, "§7Pause:§r Escape  |  §7Break:§r Left Click  |  §7Place:§r Right Click", cardX + 8, cardY + 22, 0.48f, 0xaaaaaa, false);

        // 4. Render Widgets
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                saveAndExit();
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
