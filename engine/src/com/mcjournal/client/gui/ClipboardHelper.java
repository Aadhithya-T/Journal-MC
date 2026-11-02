package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

/**
 * Clipboard helper supporting GLFW native clipboard with headless and unit-test fallbacks.
 */
public class ClipboardHelper {
    private static String fallbackClipboard = "";

    public static String getClipboard(long windowHandle) {
        if (windowHandle != 0) {
            try {
                String str = GLFW.glfwGetClipboardString(windowHandle);
                if (str != null) {
                    return str;
                }
            } catch (Exception ignored) {}
        }
        return fallbackClipboard;
    }

    public static void setClipboard(long windowHandle, String text) {
        fallbackClipboard = (text != null) ? text : "";
        if (windowHandle != 0) {
            try {
                GLFW.glfwSetClipboardString(windowHandle, fallbackClipboard);
            } catch (Exception ignored) {}
        }
    }
}
