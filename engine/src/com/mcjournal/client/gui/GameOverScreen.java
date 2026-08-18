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

        int btnW = 340;
        int btnH = 44;
        int centerX = (width - btnW) / 2;
        int startY = height / 2 + 30;

        buttons.add(new Button(1, "Delete World & Return to Title", centerX, startY, btnW, btnH, () -> {
            WorldSaveManager.deleteWorld();
            app.setScreen(new TitleScreen(app));
        }));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // Red Death Overlay
        gui.drawRect(0, 0, width, height, 0.45f, 0.05f, 0.05f, 0.92f);

        // Header Title
        String title = "YOU DIED!";
        float titleW = font.getStringWidth(title, 2.5f);
        font.drawString(gui, title, (width - titleW) / 2.0f, height / 2 - 80, 2.5f, 0xff2222, true);

        // Hardcore Loss Notice
        String notice = "Your Hardcore world has been lost to the wilderness.";
        float noticeW = font.getStringWidth(notice, 1.0f);
        font.drawString(gui, notice, (width - noticeW) / 2.0f, height / 2 - 20, 1.0f, 0xffffff, true);

        // Render Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }
}
