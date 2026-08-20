package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

public class TitleScreen extends Screen {
    private float splashTimer = 0;
    private final String[] splashes = {
        "Now with extra hugs!",
        "Hardcore Adventuring!",
        "Native Java 26 + OpenGL 3.3!",
        "Write your adventurer's story!",
        "One Life. One World.",
        "Procedural 64x Pixel Art!",
        "100% pure voxel power!"
    };
    private final String currentSplash;
    private String multiplayerNotice = null;
    private float noticeTimer = 0;

    private final MinecraftLogoRenderer logoRenderer = new MinecraftLogoRenderer();

    public TitleScreen(MCJournalApp app) {
        super(app);
        this.currentSplash = splashes[(int) (Math.random() * splashes.length)];
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        logoRenderer.init();

        int btnWidth = 400;
        int btnHeight = 40;
        int centerX = (width - btnWidth) / 2;

        // Position menu below logo
        int logoH = 120;
        int startY = Math.max(logoH + 30, height / 2 - 10);

        // 1. Singleplayer Button (Row 1)
        buttons.add(new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Multiplayer Button (Row 2)
        buttons.add(new Button(2, "Multiplayer", centerX, startY + 48, btnWidth, btnHeight, () -> {
            multiplayerNotice = "Multiplayer servers coming in a future update!";
            noticeTimer = 3.0f;
        }));

        // 3. Quit Game Button (Row 3, full-width matching Singleplayer & Multiplayer)
        buttons.add(new Button(3, "Quit Game", centerX, startY + 96, btnWidth, btnHeight, () -> {
            app.quitGame();
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        splashTimer += deltaTime * 3.2f;
        if (noticeTimer > 0) {
            noticeTimer -= deltaTime;
            if (noticeTimer <= 0) multiplayerNotice = null;
        }

        // Soft darkened vignette overlay over the live 3D background
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.25f);

        // 1. Render 3D Stone Minecraft Title Logo (512x140)
        int logoW = Math.min(500, width - 40);
        int logoH = (int) (logoW * (140.0f / 512.0f));
        int logoX = (width - logoW) / 2;
        int logoY = Math.max(16, height / 2 - 195);

        logoRenderer.render(gui, logoX, logoY, logoW, logoH);

        // 2. Pulsing Yellow Splash Text (Angled at bottom right of logo)
        float scalePulse = 0.95f + (float) Math.sin(splashTimer) * 0.08f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        float splashX = logoX + logoW - (splashW * 0.65f) - 10;
        float splashY = logoY + logoH - 12;

        font.drawString(gui, currentSplash, splashX, splashY, scalePulse, 0xffff00, true);

        // 3. Multiplayer / Notice Toast
        if (multiplayerNotice != null) {
            float noticeW = font.getStringWidth(multiplayerNotice, 0.90f);
            float toastX = (width - noticeW) / 2.0f;
            float toastY = height / 2 - 40;
            gui.drawBevelBox(toastX - 16, toastY - 8, noticeW + 32, 36, 0x221808, 0xaa7700, 0x553300);
            font.drawString(gui, multiplayerNotice, toastX, toastY, 0.90f, 0xffcc00, true);
        }

        // 4. Authentic Minecraft Corner Footers
        String leftFooter = "Minecraft* 26.1.2 - Singleplayer (Hardcore Edition)";
        font.drawString(gui, leftFooter, 8, height - 18, 0.72f, 0xcccccc, true);

        String rightFooter = "Java 26 + OpenGL 3.3 | " + app.getVideoBackgroundManager().getCurrentThemeName();
        float rightW = font.getStringWidth(rightFooter, 0.72f);
        font.drawString(gui, rightFooter, width - rightW - 8, height - 18, 0.72f, 0xcccccc, true);

        // 5. Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
