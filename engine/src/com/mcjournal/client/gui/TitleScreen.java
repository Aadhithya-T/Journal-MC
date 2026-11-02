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
    public boolean usesLogicalCoordinates() {
        return true;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        logoRenderer.init();

        int btnWidth = 200;
        int btnHeight = 20;
        int centerX = GuiLayout.centerX(width, btnWidth);

        // Position menu below logo
        int startY = Math.max(65, height / 2 - 10);

        // 1. Singleplayer Button (Row 1)
        buttons.add(new Button(1, "Singleplayer", centerX, startY, btnWidth, btnHeight, () -> {
            if (app != null) app.setScreen(new WorldSelectScreen(app));
        }));

        // 2. Multiplayer Button (Row 2)
        buttons.add(new Button(2, "Multiplayer", centerX, startY + 24, btnWidth, btnHeight, () -> {
            multiplayerNotice = "Multiplayer servers coming in a future update!";
            noticeTimer = 3.0f;
        }));

        // 3. Options... & Quit Game (Row 3, split 2-column)
        int splitW = GuiLayout.splitWidth(btnWidth, 4, 2); // 98px
        buttons.add(new Button(3, "Options...", centerX, startY + 48, splitW, btnHeight, () -> {
            if (app != null) app.setScreen(new OptionsScreen(app, this));
        }));

        buttons.add(new Button(4, "Quit Game", centerX + splitW + 4, startY + 48, splitW, btnHeight, () -> {
            if (app != null) app.quitGame();
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

        // 1. Render 3D Stone Minecraft Title Logo (250x68)
        int logoW = Math.min(260, width - 20);
        int logoH = (int) (logoW * (140.0f / 512.0f));
        int logoX = GuiLayout.centerX(width, logoW);
        int logoY = Math.max(8, height / 2 - 95);

        logoRenderer.render(gui, logoX, logoY, logoW, logoH);

        // 2. Pulsing Yellow Splash Text (Angled at bottom right of logo)
        float scalePulse = 0.52f + (float) Math.sin(splashTimer) * 0.04f;
        float splashW = font.getStringWidth(currentSplash, scalePulse);
        float splashX = logoX + logoW - (splashW * 0.65f) - 5;
        float splashY = logoY + logoH - 6;

        font.drawString(gui, currentSplash, splashX, splashY, scalePulse, 0xffff00, true);

        // 3. Multiplayer / Notice Toast
        if (multiplayerNotice != null) {
            float noticeW = font.getStringWidth(multiplayerNotice, 0.48f);
            float toastX = GuiLayout.centerX(width, (int) noticeW);
            float toastY = height / 2 - 20;
            gui.drawBevelBox(toastX - 8, toastY - 4, noticeW + 16, 18, 0x221808, 0xaa7700, 0x553300);
            font.drawString(gui, multiplayerNotice, toastX, toastY, 0.48f, 0xffcc00, true);
        }

        // 4. Authentic Minecraft Corner Footers
        String leftFooter = "Minecraft* 26.1.2 - Singleplayer (Hardcore Edition)";
        font.drawString(gui, leftFooter, GuiLayout.anchorLeft(4), GuiLayout.anchorBottom(height, 10, 2), 0.45f, 0xcccccc, true);

        String rightFooter = "Java 26 + OpenGL 3.3";
        float rightW = font.getStringWidth(rightFooter, 0.45f);
        font.drawString(gui, rightFooter, GuiLayout.anchorRight(width, (int) rightW, 4), GuiLayout.anchorBottom(height, 10, 2), 0.45f, 0xcccccc, true);

        // 5. Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
