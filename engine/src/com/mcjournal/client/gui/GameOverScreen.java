package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;

public class GameOverScreen extends Screen {
    public GameOverScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = 320;
        int btnH = 40;
        int centerX = (width - btnW) / 2;
        int startY = height / 2 + 20;

        buttons.add(new Button(1, "Delete World & Return to Title", centerX, startY, btnW, btnH, () -> {
            WorldSaveManager.deleteWorld();
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Red Death Vignette Overlay
        gui.drawRect(0, 0, width, height, 0.50f, 0.03f, 0.03f, 0.92f);

        // Header Title (Minecraft 1.20 Bold "You died!")
        String title = "You died!";
        float titleW = font.getStringWidth(title, 2.3f);
        font.drawString(gui, title, (width - titleW) / 2.0f, height / 4, 2.3f, 0xff3333, true);

        // Score & Hardcore loss subtitles
        String score = "Score: 0";
        float scoreW = font.getStringWidth(score, 0.95f);
        font.drawString(gui, score, (width - scoreW) / 2.0f, height / 4 + 50, 0.95f, 0xffffff, true);

        String notice = "Hardcore Mode: Your world has been deleted permanently.";
        float noticeW = font.getStringWidth(notice, 0.85f);
        font.drawString(gui, notice, (width - noticeW) / 2.0f, height / 4 + 75, 0.85f, 0xcccccc, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
