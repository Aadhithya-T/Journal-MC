package com.mcjournal.test;

import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Headless test harness to capture high-fidelity offscreen screenshots of
 * screens across multiple resolutions, GUI scale settings, and font profiles.
 */
public class GuiScreenCaptureHelper {

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "phase3";
        if ("ab".equalsIgnoreCase(mode)) {
            runFontAbTest();
        } else if ("after".equalsIgnoreCase(mode)) {
            runPhase5AfterRegression();
        } else {
            captureScreens(mode);
        }
    }

    public static void runPhase5AfterRegression() {
        File outDir = new File("docs/screenshots/after");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        int[][] resolutions = {
                {854, 480},
                {1280, 720},
                {1920, 1080},
                {2560, 1440}
        };

        int[] scales = {0, 1, 2}; // 0 = Auto

        long window = 0;
        try {
            if (!glfwInit()) {
                System.err.println("[Phase5] GLFW init failed");
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
                System.err.println("[Phase5] Failed to create GLFW window");
                return;
            }

            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            GuiRenderer gui = new GuiRenderer();
            FontRenderer font = new FontRenderer(FontRenderer.ACTIVE_CONFIG);
            GuiScaleManager scaleManager = new GuiScaleManager();

            int totalCaptures = 0;

            for (int[] res : resolutions) {
                int resW = res[0];
                int resH = res[1];

                for (int scale : scales) {
                    String scaleLabel = (scale == 0) ? "auto" : ("scale" + scale);

                    scaleManager.setRequestedScale(scale);
                    scaleManager.update(resW, resH, resW, resH);
                    int guiW = scaleManager.getGuiWidth();
                    int guiH = scaleManager.getGuiHeight();

                    // 1. Verify and Capture WorldSelectScreen
                    WorldSelectScreen selectScreen = new WorldSelectScreen(null);
                    selectScreen.init(guiW, guiH);
                    List<GuiDiagnostics.Violation> selectV = GuiDiagnostics.checkScreen(selectScreen, font);
                    if (!selectV.isEmpty()) {
                        throw new AssertionError("WorldSelectScreen violation at " + resW + "x" + resH + " (" + scaleLabel + "): " + selectV.get(0));
                    }
                    captureScreen(gui, font, selectScreen, scaleManager, resW, resH, guiW, guiH,
                            new File(outDir, "world_select_" + resW + "x" + resH + "_" + scaleLabel + ".png"));

                    // 2. Verify and Capture WorldCreateScreen
                    WorldCreateScreen createScreen = new WorldCreateScreen(null);
                    createScreen.init(guiW, guiH);
                    List<GuiDiagnostics.Violation> createV = GuiDiagnostics.checkScreen(createScreen, font);
                    if (!createV.isEmpty()) {
                        throw new AssertionError("WorldCreateScreen violation at " + resW + "x" + resH + " (" + scaleLabel + "): " + createV.get(0));
                    }
                    captureScreen(gui, font, createScreen, scaleManager, resW, resH, guiW, guiH,
                            new File(outDir, "world_create_" + resW + "x" + resH + "_" + scaleLabel + ".png"));

                    totalCaptures += 2;
                }
            }

            gui.cleanup();
            font.cleanup();
            System.out.println("[Phase5] Successfully verified zero layout violations and saved " + totalCaptures + " regression captures to " + outDir.getPath());
        } catch (Throwable t) {
            System.err.println("[Phase5] Regression error: " + t.getMessage());
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (window != 0) {
                glfwDestroyWindow(window);
                glfwTerminate();
            }
        }
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

                // 3. Capture TitleScreen
                TitleScreen titleScreen = new TitleScreen(null);
                titleScreen.init(guiW, guiH);
                captureScreen(gui, font, titleScreen, scaleManager, resW, resH, guiW, guiH,
                        new File(outDir, "title_screen_" + resW + "x" + resH + ".png"));
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

    public static void runFontAbTest() {
        File outDir = new File("docs/screenshots/font-ab");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        int resW = 1280;
        int resH = 720;

        long window = 0;
        try {
            if (!glfwInit()) {
                System.err.println("[FontAbTest] GLFW init failed");
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
                System.err.println("[FontAbTest] Failed to create GLFW window");
                return;
            }

            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            GuiRenderer gui = new GuiRenderer();
            GuiScaleManager scaleManager = new GuiScaleManager();
            scaleManager.setRequestedScale(0); // Auto scale
            scaleManager.update(resW, resH, resW, resH);
            int guiW = scaleManager.getGuiWidth();
            int guiH = scaleManager.getGuiHeight();

            // --- Candidate A: Monocraft ---
            FontRenderer fontMono = new FontRenderer(FontRenderer.MONOCRAFT);

            WorldSelectScreen selectMono = new WorldSelectScreen(null);
            selectMono.init(guiW, guiH);
            BufferedImage imgSelectMono = renderToImage(gui, fontMono, selectMono, scaleManager, resW, resH, guiW, guiH);
            ImageIO.write(imgSelectMono, "png", new File(outDir, "world_select_monocraft.png"));

            WorldCreateScreen createMono = new WorldCreateScreen(null);
            createMono.init(guiW, guiH);
            BufferedImage imgCreateMono = renderToImage(gui, fontMono, createMono, scaleManager, resW, resH, guiW, guiH);
            ImageIO.write(imgCreateMono, "png", new File(outDir, "world_create_monocraft.png"));

            fontMono.cleanup();

            // --- Candidate B: Minecraft-Font (Proportional) ---
            FontRenderer fontProp = new FontRenderer(FontRenderer.MINECRAFT_PROPORTIONAL);

            WorldSelectScreen selectProp = new WorldSelectScreen(null);
            selectProp.init(guiW, guiH);
            BufferedImage imgSelectProp = renderToImage(gui, fontProp, selectProp, scaleManager, resW, resH, guiW, guiH);
            ImageIO.write(imgSelectProp, "png", new File(outDir, "world_select_proportional.png"));

            WorldCreateScreen createProp = new WorldCreateScreen(null);
            createProp.init(guiW, guiH);
            BufferedImage imgCreateProp = renderToImage(gui, fontProp, createProp, scaleManager, resW, resH, guiW, guiH);
            ImageIO.write(imgCreateProp, "png", new File(outDir, "world_create_proportional.png"));

            fontProp.cleanup();
            gui.cleanup();

            // --- Create Side-by-Side Composite Images ---
            BufferedImage compSelect = createSideBySideComposite(
                    imgSelectMono, "A: Monocraft (OFL 1.1) - Monospaced Pixel Grid",
                    imgSelectProp, "B: Minecraft-Font by IdreesInc - Proportional Grid"
            );
            ImageIO.write(compSelect, "png", new File(outDir, "world_select_ab_comparison.png"));

            BufferedImage compCreate = createSideBySideComposite(
                    imgCreateMono, "A: Monocraft (OFL 1.1) - Monospaced Pixel Grid",
                    imgCreateProp, "B: Minecraft-Font by IdreesInc - Proportional Grid"
            );
            ImageIO.write(compCreate, "png", new File(outDir, "world_create_ab_comparison.png"));

            System.out.println("[FontAbTest] Successfully generated A/B test comparisons in " + outDir.getPath());
        } catch (Throwable t) {
            System.err.println("[FontAbTest] Error: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (window != 0) {
                glfwDestroyWindow(window);
                glfwTerminate();
            }
        }
    }

    private static BufferedImage createSideBySideComposite(BufferedImage left, String leftTitle, BufferedImage right, String rightTitle) {
        int w = left.getWidth();
        int h = left.getHeight();
        int headerH = 40;
        int gutter = 8;

        BufferedImage composite = new BufferedImage(w * 2 + gutter, h + headerH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = composite.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Header Background
        g.setColor(new Color(0x18, 0x18, 0x1C));
        g.fillRect(0, 0, composite.getWidth(), headerH);

        // Dividing gutter
        g.setColor(new Color(0x30, 0x30, 0x38));
        g.fillRect(w, 0, gutter, composite.getHeight());

        // Header Labels
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(0x55, 0xFF, 0x55));
        g.drawString(leftTitle, 16, 26);

        g.setColor(new Color(0x55, 0xDD, 0xFF));
        g.drawString(rightTitle, w + gutter + 16, 26);

        // Images
        g.drawImage(left, 0, headerH, null);
        g.drawImage(right, w + gutter, headerH, null);

        g.dispose();
        return composite;
    }

    public static BufferedImage renderToImage(GuiRenderer gui, FontRenderer font, Screen screen,
                                              GuiScaleManager scaleManager, int resW, int resH, int guiW, int guiH) {
        int fbo = glGenFramebuffers();
        int colorTex = glGenTextures();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        glBindTexture(GL_TEXTURE_2D, colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, resW, resH, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete!");
        }

        glViewport(0, 0, resW, resH);
        glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        gui.begin(guiW, guiH, 0, 0, resW, resH);
        screen.render(gui, font, -1, -1, 0.016f);
        gui.end();

        ByteBuffer pixelBuffer = MemoryUtil.memAlloc(resW * resH * 4);
        glReadPixels(0, 0, resW, resH, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

        BufferedImage img = new BufferedImage(resW, resH, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < resH; y++) {
            for (int x = 0; x < resW; x++) {
                int srcY = (resH - 1 - y);
                int idx = (srcY * resW + x) * 4;
                int r = pixelBuffer.get(idx) & 0xFF;
                int gCol = pixelBuffer.get(idx + 1) & 0xFF;
                int b = pixelBuffer.get(idx + 2) & 0xFF;
                int rgb = (r << 16) | (gCol << 8) | b;
                img.setRGB(x, y, rgb);
            }
        }

        MemoryUtil.memFree(pixelBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteTextures(colorTex);
        glDeleteFramebuffers(fbo);

        return img;
    }

    public static void captureScreen(GuiRenderer gui, FontRenderer font, Screen screen,
                                     GuiScaleManager scaleManager, int resW, int resH, int guiW, int guiH,
                                     File outFile) {
        try {
            BufferedImage img = renderToImage(gui, font, screen, scaleManager, resW, resH, guiW, guiH);
            ImageIO.write(img, "png", outFile);
        } catch (Exception e) {
            System.err.println("Failed to write screenshot " + outFile.getName() + ": " + e.getMessage());
        }
    }
}
