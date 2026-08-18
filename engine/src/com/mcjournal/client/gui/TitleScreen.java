package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class TitleScreen extends Screen {
    private float splashTimer = 0;
    private final String[] splashes = {
        "Hardcore Adventuring!",
        "Native Java 26 + OpenGL 3.3!",
        "Write your adventurer's story!",
        "One Life. One World.",
        "Faithful 64x Textures!"
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

        int btnWidth = 360;
        int btnHeight = 48;
        int centerX = (width - btnWidth) / 2;
        int startY = height / 2 + 10;

        // 1. Singleplayer Button
        buttons.add(new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Multiplayer Button (Clickable, redirects to nothing)
        buttons.add(new Button(2, "Multiplayer", centerX, startY + 60, btnWidth, btnHeight, () -> {
            multiplayerNotice = "Multiplayer servers coming in future update!";
            noticeTimer = 3.0f;
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        splashTimer += deltaTime * 3.0f;
        if (noticeTimer > 0) {
            noticeTimer -= deltaTime;
            if (noticeTimer <= 0) multiplayerNotice = null;
        }

        // Background: Dark Slate Soil Overlay
        gui.drawRect(0, 0, width, height, 0.11f, 0.11f, 0.13f, 1.0f);

        // Header Minecraft Title Banner Box
        int bannerW = 560;
        int bannerH = 76;
        int bannerX = (width - bannerW) / 2;
        int bannerY = 60;

        gui.drawBevelBox(bannerX, bannerY, bannerW, bannerH, 0x222222, 0x555555, 0x111111);

        String title = "MINECRAFT JOURNAL";
        float titleW = font.getStringWidth(title, 2.0f);
        font.drawString(gui, title, (width - titleW) / 2.0f, bannerY + 24, 2.0f, 0xffff55, true);

        // Pulsing Yellow Splash Text
        float scalePulse = 0.95f + (float) Math.sin(splashTimer) * 0.08f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        font.drawString(gui, currentSplash, bannerX + bannerW - splashW + 15, bannerY + bannerH + 4, scalePulse, 0xffff00, true);

        // Multiplayer Notice Toast (if clicked)
        if (multiplayerNotice != null) {
            float noticeW = font.getStringWidth(multiplayerNotice, 0.9f);
            float toastX = (width - noticeW) / 2.0f;
            float toastY = height / 2 - 35;
            gui.drawBevelBox(toastX - 16, toastY - 8, noticeW + 32, 34, 0x332200, 0xaa7700, 0x553300);
            font.drawString(gui, multiplayerNotice, toastX, toastY, 0.9f, 0xffcc00, true);
        }

        // Footer Subtitle
        String sub = "Hardcore Edition | Java 26 + LWJGL 3.3";
        float subW = font.getStringWidth(sub, 0.85f);
        font.drawString(gui, sub, (width - subW) / 2.0f, height - 30, 0.85f, 0x777777, false);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
