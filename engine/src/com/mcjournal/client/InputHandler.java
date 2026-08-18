package com.mcjournal.client;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private double mouseDeltaX = 0;
    private double mouseDeltaY = 0;
    private double scrollDelta = 0;
    private boolean firstMouse = true;

    public void init(long windowHandle) {
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = (action != GLFW_RELEASE);
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            mouseDeltaX += (xpos - lastMouseX);
            mouseDeltaY += (ypos - lastMouseY);
            lastMouseX = xpos;
            lastMouseY = ypos;
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = (action != GLFW_RELEASE);
            }
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            scrollDelta += yoffset;
        });
    }

    public boolean isKeyDown(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keys[keyCode];
        }
        return false;
    }

    public boolean isMouseButtonDown(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }

    public double consumeMouseDeltaX() {
        double dx = mouseDeltaX;
        mouseDeltaX = 0;
        return dx;
    }

    public double consumeMouseDeltaY() {
        double dy = mouseDeltaY;
        mouseDeltaY = 0;
        return dy;
    }

    public double consumeScrollDelta() {
        double delta = scrollDelta;
        scrollDelta = 0;
        return delta;
    }
}
