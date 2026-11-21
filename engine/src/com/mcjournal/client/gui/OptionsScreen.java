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
    private Button fovBtn;
    private Button fpsBtn;
    private Button renderDistBtn;
    private Button showFpsBtn;
    private Button fogBtn;
    private Button guiScaleBtn;
    private Button doneBtn;

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

        GameSettings settings = (app != null) ? app.getSettings() : new GameSettings();

        // --- ROW 1: FOV & Max FPS ---
        fovBtn = new Button(1, "FOV: " + (int) settings.fov + "°", 0, 0, 150, 20, () -> {
            settings.cycleFov();
            if (app != null && app.getCamera() != null) app.getCamera().setFov(settings.fov);
            if (app != null && app.getCamera() != null && app.getWindow() != null) {
                app.getCamera().updateProjection(app.getWindow().getAspectRatio());
            }
            init(this.width, this.height);
        });
        addWidget(fovBtn);

        fpsBtn = new Button(2, "Max FPS: " + settings.getFpsDisplayString(), 0, 0, 150, 20, () -> {
            settings.cycleFpsLimit();
            if (app != null) app.applySettings();
            init(this.width, this.height);
        });
        addWidget(fpsBtn);

        // --- ROW 2: Render Distance & Show FPS ---
        renderDistBtn = new Button(3, "Render Dist: " + settings.renderDistance + " Chunks", 0, 0, 150, 20, () -> {
            settings.cycleRenderDistance();
            if (app != null) app.applySettings();
            init(this.width, this.height);
        });
        addWidget(renderDistBtn);

        showFpsBtn = new Button(4, "Show FPS: " + (settings.showFps ? "ON" : "OFF"), 0, 0, 150, 20, () -> {
            settings.toggleShowFps();
            init(this.width, this.height);
        });
        addWidget(showFpsBtn);

        // --- ROW 3: Fog Mode & GUI Scale ---
        fogBtn = new Button(5, "Fog: " + settings.getFogModeString(), 0, 0, 150, 20, () -> {
            settings.cycleFogMode();
            init(this.width, this.height);
        });
        addWidget(fogBtn);

        guiScaleBtn = new Button(6, "GUI Scale: " + settings.getGuiScaleDisplayString(), 0, 0, 150, 20, () -> {
            settings.cycleGuiScale();
            if (app != null) app.applySettings();
            init(this.width, this.height);
        });
        addWidget(guiScaleBtn);

        // --- Done Button (Bottom Center) ---
        doneBtn = new Button(10, "Done", 0, 0, 200, 20, this::saveAndExit);
        addWidget(doneBtn);
        focusManager.setFocus(doneBtn);

        layout();
    }

    @Override
    public void layout() {
        int btnW = 150;
        int btnH = 20;
        int gap = 8;
        int totalRowW = btnW * 2 + gap; // 308px
        int startX = GuiLayout.centerX(width, totalRowW);
        int startY = Math.max(34, height / 2 - 80);

        if (fovBtn != null) {
            fovBtn.setPosition(startX, startY);
            fovBtn.setSize(btnW, btnH);
        }
        if (fpsBtn != null) {
            fpsBtn.setPosition(startX + btnW + gap, startY);
            fpsBtn.setSize(btnW, btnH);
        }
        if (renderDistBtn != null) {
            renderDistBtn.setPosition(startX, startY + 24);
            renderDistBtn.setSize(btnW, btnH);
        }
        if (showFpsBtn != null) {
            showFpsBtn.setPosition(startX + btnW + gap, startY + 24);
            showFpsBtn.setSize(btnW, btnH);
        }
        if (fogBtn != null) {
            fogBtn.setPosition(startX, startY + 48);
            fogBtn.setSize(btnW, btnH);
        }
        if (guiScaleBtn != null) {
            guiScaleBtn.setPosition(startX + btnW + gap, startY + 48);
            guiScaleBtn.setSize(btnW, btnH);
        }

        int doneW = 200;
        int doneX = GuiLayout.centerX(width, doneW);
        if (doneBtn != null) {
            doneBtn.setPosition(doneX, height - 30);
            doneBtn.setSize(doneW, btnH);
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
