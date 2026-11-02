package com.mcjournal.client.gui;

import com.mcjournal.client.AtmosphericTimeSystem;
import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;
import com.mcjournal.client.WorldSession;
import org.lwjgl.glfw.GLFW;

/**
 * Rebuilt GameOverScreen using logical coordinates, Widget toolkit,
 * centralized input dispatch, and safe confirmation dialogs.
 * Hardcore worlds are NEVER deleted automatically upon death.
 */
public class GameOverScreen extends Screen {
    public GameOverScreen(MCJournalApp app) {
        super(app);
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int btnW = Math.min(200, width - 24);
        int btnH = 20;
        int centerX = GuiLayout.centerX(width, btnW);
        int startY = height / 2 + 15;

        // 1. Return to Title Screen (Safe - does NOT delete world)
        Button returnBtn = new Button(1, "Title Screen", centerX, startY, btnW, btnH, () -> {
            if (app != null) app.setScreen(new TitleScreen(app));
        });
        addWidget(returnBtn);
        focusManager.setFocus(returnBtn);

        // 2. Delete World Button (Explicit Confirmation Required)
        Button deleteBtn = new Button(2, "Delete World", centerX, startY + 24, btnW, btnH, this::promptDeleteWorld);
        addWidget(deleteBtn);
    }

    public void promptDeleteWorld() {
        String prompt = "Are you sure you want to delete this hardcore world?\nAll journal records will be erased permanently!";
        ConfirmScreen confirm = new ConfirmScreen(app, "Delete Hardcore World?", prompt, "Delete World", "Cancel", true,
                () -> {
                    WorldSaveManager.deleteWorld();
                    if (app != null) app.setScreen(new TitleScreen(app));
                },
                () -> {
                    if (app != null) app.popScreen();
                });

        if (app != null) {
            app.pushScreen(confirm);
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Red Death Vignette Overlay
        gui.drawRect(0, 0, width, height, 0.50f, 0.03f, 0.03f, 0.92f);

        // 2. Header Title ("You died!")
        String title = "You died!";
        float titleScale = 1.6f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), height / 4, titleScale, 0xff3333, true);

        // 3. Survival Duration / Real Stats Summary
        long daysSurvived = 0;
        if (app != null) {
            AtmosphericTimeSystem atmo = app.getAtmosphericSystem();
            if (atmo != null) {
                daysSurvived = (long) (atmo.getWorldTimeTicks() / 24000.0);
            }
        }
        String score = "Days Survived: " + daysSurvived;
        float scoreScale = 1.0f;
        float scoreW = font.getStringWidth(score, scoreScale);
        font.drawString(gui, score, GuiLayout.centerX(width, (int) scoreW), height / 4 + 36, scoreScale, 0xffffff, true);

        String notice = "Hardcore Mode: One Life. Permadeath.";
        float noticeScale = 1.0f;
        float noticeW = font.getStringWidth(notice, noticeScale);
        font.drawString(gui, notice, GuiLayout.centerX(width, (int) noticeW), height / 4 + 52, noticeScale, 0xddaaaa, true);

        // 4. Render Action Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (app != null) app.setScreen(new TitleScreen(app));
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
