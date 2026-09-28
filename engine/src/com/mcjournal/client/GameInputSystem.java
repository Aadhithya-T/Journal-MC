package com.mcjournal.client;

import com.mcjournal.Item;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.client.gui.EscapeMenuScreen;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * GameInputSystem processes in-game player inputs:
 * mouse look / camera rotation, hotbar selection, scrolling, and item throwing (Q key).
 */
public class GameInputSystem {
    private static final float MOUSE_SENSITIVITY = 0.12f;
    private static final double TICK_DURATION = 0.050;

    // Item Throwing / Drop (Q Key)
    private boolean wasQDown = false;
    private float qHoldTimer = 0.0f;

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

    public void handleItemDropInput(InputHandler input, Player player, Camera camera, ItemEntityManager itemEntityManager, FirstPersonHandRenderer handRenderer) {
        if (input == null || player == null || player.isDead || itemEntityManager == null) return;

        boolean qDown = input.isKeyDown(GLFW_KEY_Q);
        if (qDown) {
            if (!wasQDown) {
                // First press: throw item immediately
                throwHeldItem(input, player, camera, itemEntityManager, handRenderer);
                qHoldTimer = 0.0f;
            } else {
                // Holding Q down: repeat throw after 0.35s delay every 0.18s
                qHoldTimer += (float) TICK_DURATION;
                if (qHoldTimer >= 0.35f) {
                    throwHeldItem(input, player, camera, itemEntityManager, handRenderer);
                    qHoldTimer = 0.18f;
                }
            }
        } else {
            qHoldTimer = 0.0f;
        }
        wasQDown = qDown;
    }

    private void throwHeldItem(InputHandler input, Player player, Camera camera, ItemEntityManager itemEntityManager, FirstPersonHandRenderer handRenderer) {
        if (player == null || player.isDead || itemEntityManager == null) return;

        boolean dropAll = input.isKeyDown(GLFW_KEY_LEFT_CONTROL) || input.isKeyDown(GLFW_KEY_RIGHT_CONTROL);
        int[] dropped = player.dropSelectedItem(dropAll);
        if (dropped != null && dropped[0] != 0 && dropped[1] > 0) {
            byte dropType = (byte) dropped[0];
            int count = dropped[1];

            Vector3f look = camera.getLookDirection();
            float eyeX = player.pos.x;
            float eyeY = player.pos.y + Player.EYE_HEIGHT - 0.25f;
            float eyeZ = player.pos.z;

            // Spawn item slightly in front of the player's eye along the camera look direction
            float spawnX = eyeX + look.x * 0.35f;
            float spawnY = eyeY + look.y * 0.35f;
            float spawnZ = eyeZ + look.z * 0.35f;

            // Throw velocity accurately oriented with the character's camera view angle (yaw and pitch)
            float throwSpeed = 6.2f;
            float vx = look.x * throwSpeed + player.velocity.x * 0.35f;
            float vy = look.y * throwSpeed + 1.2f; // Slight loft upward for authentic Minecraft ballistic trajectory
            float vz = look.z * throwSpeed + player.velocity.z * 0.35f;

            itemEntityManager.spawnThrownItem(dropType, count, spawnX, spawnY, spawnZ, vx, vy, vz);

            if (handRenderer != null) {
                handRenderer.triggerSwing();
            }

            System.out.println("[Inventory] 🏹 Threw " + count + "x " + (Item.isTool(dropType) ? Item.getName(dropType) : BlockStateRegistry.getBlockType(dropType).getName()));
        }
    }
}
