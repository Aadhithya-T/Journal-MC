package com.mcjournal.client.gui;

import com.mcjournal.client.Player;

public class HardcoreHUD {
    public void render(GuiRenderer gui, FontRenderer font, Player player, int screenWidth, int screenHeight, String biomeName) {
        int centerX = screenWidth / 2;
        int bottomY = screenHeight - 60;

        // 1. Crosshair in Screen Center
        int cy = screenHeight / 2;
        gui.drawRect(centerX - 8, cy - 1, 16, 2, 1.0f, 1.0f, 1.0f, 0.85f);
        gui.drawRect(centerX - 1, cy - 8, 2, 16, 1.0f, 1.0f, 1.0f, 0.85f);

        // 2. 9-Slot Hotbar (Bottom Center)
        int slotSize = 40;
        int hotbarW = 9 * slotSize;
        int hotbarX = centerX - hotbarW / 2;
        int hotbarY = screenHeight - 52;

        gui.drawBevelBox(hotbarX - 4, hotbarY - 4, hotbarW + 8, slotSize + 8, 0x222222, 0x555555, 0x111111);

        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + i * slotSize;
            boolean isSelected = (i == player.selectedSlot);

            int bg = isSelected ? 0x4a4a4a : 0x2a2a2a;
            int borderL = isSelected ? 0xffffff : 0x555555;
            int borderD = isSelected ? 0xffffff : 0x111111;

            gui.drawBevelBox(slotX, hotbarY, slotSize - 2, slotSize - 2, bg, borderL, borderD);

            // Slot numbers (1-9)
            font.drawString(gui, String.valueOf(i + 1), slotX + 4, hotbarY + 4, 0.65f, isSelected ? 0xffffa0 : 0x888888, false);
        }

        // 3. 10 Hardcore Hearts (Left of Hotbar)
        int heartsX = hotbarX;
        int statsY = hotbarY - 22;

        int fullHearts = player.health / 2;
        boolean halfHeart = (player.health % 2 == 1);

        for (int h = 0; h < 10; h++) {
            int heartX = heartsX + h * 16;
            if (h < fullHearts) {
                // Hardcore Crimson Full Heart
                gui.drawBevelBox(heartX, statsY, 13, 13, 0xaa1111, 0xff3333, 0x550000);
            } else if (h == fullHearts && halfHeart) {
                // Half Heart
                gui.drawBevelBox(heartX, statsY, 7, 13, 0xaa1111, 0xff3333, 0x550000);
            } else {
                // Empty Heart Container (Black/Dark Red Hardcore outline)
                gui.drawBevelBox(heartX, statsY, 13, 13, 0x1a0505, 0x441111, 0x110000);
            }
        }

        // 4. 10 Hunger Drumsticks (Right of Hotbar)
        int hungerX = hotbarX + hotbarW - (10 * 16);
        for (int hu = 0; hu < 10; hu++) {
            int drumX = hungerX + hu * 16;
            if (hu < player.hunger / 2) {
                gui.drawBevelBox(drumX, statsY, 13, 13, 0x8b5a2b, 0xcd853f, 0x4a2e12);
            } else {
                gui.drawBevelBox(drumX, statsY, 13, 13, 0x1a1208, 0x3d2812, 0x110802);
            }
        }

        // 5. Survival HUD Stats (Top-Left)
        String posStr = String.format("XYZ: %.1f / %.1f / %.1f", player.pos.x, player.pos.y, player.pos.z);
        font.drawString(gui, posStr, 12, 12, 0.85f, 0xffffff, true);

        String modeStr = "Mode: HARDCORE | Biome: " + biomeName;
        font.drawString(gui, modeStr, 12, 32, 0.75f, 0xff5555, true);

        font.drawString(gui, "Controls: WASD Move | SPACE Jump | CTRL Sprint | SHIFT Sneak | ESC Menu", 12, screenHeight - 20, 0.7f, 0x888888, false);
    }
}
