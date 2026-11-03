package com.mcjournal.client;

import com.mcjournal.client.gui.EscapeMenuScreen;

import static org.lwjgl.glfw.GLFW.*;

/**
 * GameInputSystem processes in-game player inputs:
 * mouse look / camera rotation, hotbar slot selection, and scrolling.
 * Manual item throwing and Q-key item dropping have been removed.
 */
public class GameInputSystem {
    private static final float MOUSE_SENSITIVITY = 0.12f;

    public void handleInGameMouseLook(MCJournalApp app, Window window, InputHandler input, Player player, Camera camera) {
        if (input == null || window == null || player == null || camera == null) return;

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            app.setScreen(new EscapeMenuScreen(app));
            return;
        }

        if (window.isCursorLocked()) {
            double mouseDx = input.consumeMouseDeltaX();
            double mouseDy = input.consumeMouseDeltaY();

            player.yaw += (float) (mouseDx * MOUSE_SENSITIVITY);
            player.pitch += (float) (mouseDy * MOUSE_SENSITIVITY);
            player.pitch = Math.clamp(player.pitch, -89.5f, 89.5f);

            camera.setYaw(player.yaw);
            camera.setPitch(player.pitch);

            // Hotbar keys 1-9
            for (int k = GLFW_KEY_1; k <= GLFW_KEY_9; k++) {
                if (input.isKeyDown(k)) {
                    player.selectedSlot = k - GLFW_KEY_1;
                }
            }

            double scroll = input.consumeScrollDelta();
            if (scroll != 0) {
                player.selectedSlot = (player.selectedSlot - (int) Math.signum(scroll) + 9) % 9;
            }
        }
    }
}
