package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen {
    protected final MCJournalApp app;
    protected int width;
    protected int height;
    protected final List<Button> buttons = new ArrayList<>();

    public Screen(MCJournalApp app) {
        this.app = app;
    }

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        this.buttons.clear();
    }

    public void update(double mouseX, double mouseY) {
        for (Button btn : buttons) {
            btn.update(mouseX, mouseY);
        }
    }

    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        for (Button btn : buttons) {
            btn.render(gui, font);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Button btn : buttons) {
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
    }

    public void charTyped(char character) {
    }
}
