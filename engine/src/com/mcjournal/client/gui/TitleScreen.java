package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

/**
 * Rebuilt TitleScreen using logical coordinates, Widget toolkit,
 * original Journal-MC branding (JournalLogoRenderer), and clean navigation.
 */
public class TitleScreen extends Screen {
    private float splashTimer = 0;
    private final String[] splashes = {
        "One Life. One World.",
        "Native Java 26 + OpenGL 3.3!",
        "Finite 2500x2500 Boundaries!",
        "Write your adventurer's journal!",
        "100% Native Voxel Power!",
        "Procedural Biomes & Natural Borders!"
    };
    private final String currentSplash;

    private final JournalLogoRenderer logoRenderer = new JournalLogoRenderer();

    public TitleScreen(MCJournalApp app) {
        super(app);
        this.currentSplash = splashes[(int) (Math.random() * splashes.length)];
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    public JournalLogoRenderer getLogoRenderer() {
        return logoRenderer;
    }

    public String getCurrentSplash() {
        return currentSplash;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        logoRenderer.init();

        int btnWidth = 200;
        int btnHeight = 20;
        int centerX = GuiLayout.centerX(width, btnWidth);

        // Position menu buttons comfortably below logo
        int startY = Math.max(70, height / 2 - 5);

        // 1. Singleplayer Button (Row 1)
        Button singleplayerBtn = new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            if (app != null) app.setScreen(new WorldSelectScreen(app));
        });
        addWidget(singleplayerBtn);
        focusManager.setFocus(singleplayerBtn);

        // 2. Options... & Quit Game (Row 2, split 2-column)
        int splitW = GuiLayout.buttonWidth(btnWidth, 2, 4); // 98px
        Button optionsBtn = new Button(2, "Options...", centerX, startY + 24, splitW, btnHeight, () -> {
            if (app != null) app.setScreen(new OptionsScreen(app, this));
        });
        addWidget(optionsBtn);

        Button quitBtn = new Button(3, "Quit Game", centerX + splitW + 4, startY + 24, splitW, btnHeight, () -> {
            if (app != null) app.quitGame();
        });
        addWidget(quitBtn);
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        splashTimer += deltaTime * 3.2f;

        // 1. Soft darkened vignette overlay over the live 3D background
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.25f);

        // 2. Render Stylized Journal-MC Title Logo
        int logoW = Math.min(260, width - 20);
        int logoH = (int) (logoW * (140.0f / 512.0f));
        int logoX = GuiLayout.centerX(width, logoW);
        int logoY = Math.max(8, height / 2 - 95);

        logoRenderer.render(gui, logoX, logoY, logoW, logoH);

        // 3. Pulsing Yellow Splash Text
        float scalePulse = 0.52f + (float) Math.sin(splashTimer) * 0.04f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        float splashX = logoX + logoW - (splashW * 0.65f) - 5;
        float splashY = logoY + logoH - 6;

        font.drawString(gui, currentSplash, splashX, splashY, scalePulse, 0xffff00, true);

        // 4. Authentic Journal-MC Corner Footers
        String leftFooter = "Journal-MC v1.0.0 — Voxel Engine";
        font.drawString(gui, leftFooter, GuiLayout.anchorLeft(4), GuiLayout.anchorBottom(height, 10, 2), 0.45f, 0xcccccc, true);

        String rightFooter = "Java 26 + OpenGL 3.3";
        float rightW = font.getStringWidth(rightFooter, 0.45f);
        font.drawString(gui, rightFooter, GuiLayout.anchorRight(width, (int) rightW, 4), GuiLayout.anchorBottom(height, 10, 2), 0.45f, 0xcccccc, true);

        // 5. Render Buttons and Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void dispose() {
        super.dispose();
        logoRenderer.cleanup();
    }
}
