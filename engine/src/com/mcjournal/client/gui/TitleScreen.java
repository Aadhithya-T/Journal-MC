package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class TitleScreen extends Screen {
    private float splashTimer = 0;
    private final String[] splashes = {
        "Hardcore Adventuring!",
        "Native Java 26 + OpenGL 3.3!",
        "Write your adventurer's story!",
        "One Life. One World.",
        "Procedural 64x Pixel Art!"
    };
    private String currentSplash;
    private String multiplayerNotice = null;
    private float noticeTimer = 0;

    public TitleScreen(MCJournalApp app) {
        super(app);
        this.currentSplash = splashes[(int) (Math.random() * splashes.length)];
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnWidth = 400;
        int btnHeight = 40;
        int centerX = (width - btnWidth) / 2;
        int startY = height / 2 + 10;

        // 1. Singleplayer Button (Full Width)
        buttons.add(new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Multiplayer Button (Full Width)
        buttons.add(new Button(2, "Multiplayer", centerX, startY + 48, btnWidth, btnHeight, () -> {
            multiplayerNotice = "Multiplayer servers coming in a future update!";
            noticeTimer = 3.0f;
        }));

        // 3. Options & Quit Game Buttons (Split 2-Column Row)
        int splitW = (btnWidth - 8) / 2; // 196px each with 8px gap
        buttons.add(new Button(3, "Options...", centerX, startY + 96, splitW, btnHeight, () -> {
            multiplayerNotice = "Options & Settings available via in-game F1-F12 controls!";
            noticeTimer = 3.0f;
        }));

        buttons.add(new Button(4, "Quit Game", centerX + splitW + 8, startY + 96, splitW, btnHeight, () -> {
            app.quitGame();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        splashTimer += deltaTime * 3.0f;
        if (noticeTimer > 0) {
            noticeTimer -= deltaTime;
            if (noticeTimer <= 0) multiplayerNotice = null;
        }

        // Soft vignette overlay over the live looping video background
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.28f);

        // Header Minecraft Title Banner Box
        int bannerW = 540;
        int bannerH = 72;
        int bannerX = (width - bannerW) / 2;
        int bannerY = Math.max(30, height / 2 - 190);

        gui.drawMinecraftButton(bannerX, bannerY, bannerW, bannerH, false, true);

        String title = "MINECRAFT JOURNAL";
        float titleW = font.getStringWidth(title, 1.80f);
        font.drawString(gui, title, (width - titleW) / 2.0f, bannerY + 22, 1.80f, 0xffffff, true);

        // Pulsing Yellow Splash Text
        float scalePulse = 0.95f + (float) Math.sin(splashTimer) * 0.08f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        font.drawString(gui, currentSplash, bannerX + bannerW - splashW + 15, bannerY + bannerH + 4, scalePulse, 0xffff00, true);

        // Multiplayer / Options Notice Toast
        if (multiplayerNotice != null) {
            float noticeW = font.getStringWidth(multiplayerNotice, 0.92f);
            float toastX = (width - noticeW) / 2.0f;
            float toastY = height / 2 - 40;
            gui.drawBevelBox(toastX - 16, toastY - 8, noticeW + 32, 36, 0x221808, 0xaa7700, 0x553300);
            font.drawString(gui, multiplayerNotice, toastX, toastY, 0.92f, 0xffcc00, true);
        }

        // Minecraft 1.20 Corner Footers
        String leftFooter = "Minecraft Journal 1.20 / Hardcore Native";
        font.drawString(gui, leftFooter, 12, height - 22, 0.80f, 0xcccccc, true);

        String rightFooter = "Java 26 + OpenGL 3.3 | " + app.getVideoBackgroundManager().getCurrentThemeName();
        float rightW = font.getStringWidth(rightFooter, 0.80f);
        font.drawString(gui, rightFooter, width - rightW - 12, height - 22, 0.80f, 0xcccccc, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
