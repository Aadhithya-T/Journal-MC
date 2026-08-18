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
        int btnHeight = 44;
        int centerX = (width - btnWidth) / 2;
        int startY = height / 2 + 15;

        // 1. Singleplayer Button
        buttons.add(new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Multiplayer Button
        buttons.add(new Button(2, "Multiplayer", centerX, startY + 50, btnWidth, btnHeight, () -> {
            multiplayerNotice = "Multiplayer servers coming in a future update!";
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

        // Soft vignette overlay over the rotating 3D world
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.25f);

        // Header Minecraft Title Banner Box
        int bannerW = 540;
        int bannerH = 74;
        int bannerX = (width - bannerW) / 2;
        int bannerY = 65;

        gui.drawMinecraftButton(bannerX, bannerY, bannerW, bannerH, false, true);

        String title = "MINECRAFT JOURNAL";
        float titleW = font.getStringWidth(title, 1.85f);
        font.drawString(gui, title, (width - titleW) / 2.0f, bannerY + 22, 1.85f, 0xffffff, true);

        // Pulsing Yellow Splash Text
        float scalePulse = 0.95f + (float) Math.sin(splashTimer) * 0.08f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        font.drawString(gui, currentSplash, bannerX + bannerW - splashW + 20, bannerY + bannerH + 6, scalePulse, 0xffff00, true);

        // Multiplayer Notice Toast
        if (multiplayerNotice != null) {
            float noticeW = font.getStringWidth(multiplayerNotice, 0.95f);
            float toastX = (width - noticeW) / 2.0f;
            float toastY = height / 2 - 40;
            gui.drawBevelBox(toastX - 16, toastY - 8, noticeW + 32, 36, 0x332200, 0xaa7700, 0x553300);
            font.drawString(gui, multiplayerNotice, toastX, toastY, 0.95f, 0xffcc00, true);
        }

        // Footer Subtitle
        String sub = "Hardcore Edition | Java 26 + OpenGL 3.3";
        float subW = font.getStringWidth(sub, 0.85f);
        font.drawString(gui, sub, (width - subW) / 2.0f, height - 28, 0.85f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
