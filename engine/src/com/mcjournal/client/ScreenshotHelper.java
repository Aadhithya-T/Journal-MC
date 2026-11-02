package com.mcjournal.client;

import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.lwjgl.opengl.GL11.*;

/**
 * Utility helper to capture and persist high-resolution in-game screenshots
 * to disk without blocking render loops.
 */
public final class ScreenshotHelper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private ScreenshotHelper() {}

    public static String saveScreenshot(int width, int height) {
        if (width <= 0 || height <= 0) return null;

        File dir = new File("screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String filename = "screenshot_" + timestamp + ".png";
        File file = new File(dir, filename);

        // Deduplicate in case multiple captures occur within the same second
        int counter = 1;
        while (file.exists()) {
            filename = "screenshot_" + timestamp + "_" + counter + ".png";
            file = new File(dir, filename);
            counter++;
        }

        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIndex = ((height - 1 - y) * width + x) * 4;
                    int r = pixels.get(srcIndex) & 0xFF;
                    int g = pixels.get(srcIndex + 1) & 0xFF;
                    int b = pixels.get(srcIndex + 2) & 0xFF;
                    int a = pixels.get(srcIndex + 3) & 0xFF;
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    image.setRGB(x, y, argb);
                }
            }

            ImageIO.write(image, "PNG", file);
            System.out.println("[Screenshot] Saved screenshot to " + file.getPath());
            return filename;
        } catch (Throwable t) {
            System.err.println("[Screenshot] Failed to capture: " + t.getMessage());
            return null;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}
