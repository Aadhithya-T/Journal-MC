package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class TitleScreen extends Screen {
    private float splashTimer = 0;
    private final String[] splashes = {
        "Now in pure Native Java 26 + OpenGL!",
        "Loom Virtual Threads Enabled!",
        "Faithful 64x Textures!",
        "Write your adventurer's story!",
        "100 Chunks of voxel freedom!",
        "0-Latency Instant Launch!"
    };
    private String currentSplash;

    public TitleScreen(MCJournalApp app) {
        super(app);
        this.currentSplash = splashes[(int) (Math.random() * splashes.length)];
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnWidth = 340;
        int btnHeight = 44;
        int centerX = (width - btnWidth) / 2;
        int startY = height / 2 - 20;

        // 1. Singleplayer / Select World
        buttons.add(new Button(1, "Singleplayer (Select World)", centerX, startY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Create New World
        buttons.add(new Button(2, "Create New World", centerX, startY + 54, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldCreateScreen(app));
        }));

        // 3. Settings & Texture Packs
        buttons.add(new Button(3, "Options & Texture Packs", centerX, startY + 108, btnWidth, btnHeight, () -> {
            // Options
        }));

        // 4. Quit Game
        buttons.add(new Button(4, "Quit Game", centerX, startY + 162, btnWidth, btnHeight, () -> {
            app.getWindow().close();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        splashTimer += deltaTime * 3.0f;

        // Background: Darkened Vignette / Slate Soil Overlay
        gui.drawRect(0, 0, width, height, 0.12f, 0.12f, 0.14f, 1.0f);

        // Header Minecraft Title Banner Box
        int bannerW = 540;
        int bannerH = 74;
        int bannerX = (width - bannerW) / 2;
        int bannerY = 50;

        gui.drawBevelBox(bannerX, bannerY, bannerW, bannerH, 0x222222, 0x555555, 0x111111);

        String title = "MINECRAFT JOURNAL";
        float titleW = font.getStringWidth(title, 2.0f);
        font.drawString(gui, title, (width - titleW) / 2.0f, bannerY + 22, 2.0f, 0xffff55, true);

        // Pulsing Yellow Splash Text
        float scalePulse = 0.95f + (float) Math.sin(splashTimer) * 0.08f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        font.drawString(gui, currentSplash, bannerX + bannerW - splashW + 20, bannerY + bannerH + 4, scalePulse, 0xffff00, true);

        // Subtitle
        String sub = "Standalone Desktop Voxel Engine Edition (Java 26 + OpenGL 3.3)";
        float subW = font.getStringWidth(sub, 0.85f);
        font.drawString(gui, sub, (width - subW) / 2.0f, height - 32, 0.85f, 0x888888, false);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
