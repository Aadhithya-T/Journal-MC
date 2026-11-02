package com.mcjournal.test;

import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Headless test harness to capture high-fidelity offscreen screenshots of
 * screens across multiple resolutions and GUI scale settings.
 */
public class GuiScreenCaptureHelper {

    public static void main(String[] args) {
        String phaseDir = args.length > 0 ? args[0] : "phase1";
        captureScreens(phaseDir);
    }

    public static void captureScreens(String subDir) {
        File outDir = new File("docs/screenshots/" + subDir);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        int[][] resolutions = {
                {854, 480},
                {1280, 720},
                {1920, 1080}
        };

        long window = 0;
        try {
            if (!glfwInit()) {
                System.err.println("[ScreenCapture] GLFW init failed");
                return;
            }

            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

            window = glfwCreateWindow(800, 600, "HeadlessCapture", 0, 0);
            if (window == 0) {
                System.err.println("[ScreenCapture] Failed to create GLFW window");
                return;
            }

            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            GuiRenderer gui = new GuiRenderer();
            FontRenderer font = new FontRenderer();
            font.init();

            GuiScaleManager scaleManager = new GuiScaleManager();

            for (int[] res : resolutions) {
                int resW = res[0];
                int resH = res[1];

                scaleManager.setRequestedScale(0); // Auto scale
                scaleManager.update(resW, resH, resW, resH);
                int guiW = scaleManager.getGuiWidth();
                int guiH = scaleManager.getGuiHeight();

                // 1. Capture WorldSelectScreen
                WorldSelectScreen selectScreen = new WorldSelectScreen(null);
                selectScreen.init(guiW, guiH);
                captureScreen(gui, font, selectScreen, scaleManager, resW, resH, guiW, guiH,
                        new File(outDir, "world_select_" + resW + "x" + resH + ".png"));

                // 2. Capture WorldCreateScreen
                WorldCreateScreen createScreen = new WorldCreateScreen(null);
                createScreen.init(guiW, guiH);
                captureScreen(gui, font, createScreen, scaleManager, resW, resH, guiW, guiH,
                        new File(outDir, "world_create_" + resW + "x" + resH + ".png"));
            }

            gui.cleanup();
            font.cleanup();
            System.out.println("[ScreenCapture] Saved captures to " + outDir.getPath());
        } catch (Throwable t) {
            System.err.println("[ScreenCapture] Capture error: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (window != 0) {
                glfwDestroyWindow(window);
                glfwTerminate();
            }
        }
    }

    private static void captureScreen(GuiRenderer gui, FontRenderer font, Screen screen,
                                      GuiScaleManager scale, int fbW, int fbH, int guiW, int guiH, File outFile) {
        int fbo = glGenFramebuffers();
        int colorTex = glGenTextures();

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glBindTexture(GL_TEXTURE_2D, colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, fbW, fbH, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Incomplete FBO for " + outFile.getName());
            return;
        }

        glViewport(0, 0, fbW, fbH);
        glClearColor(0.12f, 0.12f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        gui.begin(guiW, guiH, scale.getViewportX(), scale.getViewportY(),
                scale.getViewportWidth(), scale.getViewportHeight());

        screen.render(gui, font, -1, -1, 0.016f);

        gui.end();

        ByteBuffer pixels = MemoryUtil.memAlloc(fbW * fbH * 4);
        glReadPixels(0, 0, fbW, fbH, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        BufferedImage image = new BufferedImage(fbW, fbH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < fbH; y++) {
            for (int x = 0; x < fbW; x++) {
                int srcIndex = ((fbH - 1 - y) * fbW + x) * 4;
                int r = pixels.get(srcIndex) & 0xFF;
                int g = pixels.get(srcIndex + 1) & 0xFF;
                int b = pixels.get(srcIndex + 2) & 0xFF;
                int a = pixels.get(srcIndex + 3) & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }
        MemoryUtil.memFree(pixels);

        try {
            ImageIO.write(image, "PNG", outFile);
        } catch (Exception e) {
            System.err.println("Failed to write " + outFile + ": " + e.getMessage());
        }

        glDeleteTextures(colorTex);
        glDeleteFramebuffers(fbo);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
