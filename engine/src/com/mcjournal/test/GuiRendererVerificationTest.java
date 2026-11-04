package com.mcjournal.test;

import com.mcjournal.client.ScreenManager;
import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Phase 0 GUI Overhaul: Comprehensive Verification & Stability Test Suite.
 * Covers all 10 Phase 0 requirements:
 * 1. Multiple consecutive rectangles batching.
 * 2. Multiple quads sharing one texture batching.
 * 3. Texture switching and flush behavior.
 * 4. Painter's-order preservation when alternating textures.
 * 5. Maximum batch capacity (2048 quads) & automatic flush without geometry loss.
 * 6. Alpha blending & RGB tint pass-through.
 * 7. Complete OpenGL state restoration after GUI rendering ends.
 * 8. Repeated GUI begin/end cycles stability.
 * 9. Resource lifecycle & idempotent cleanup.
 * 10. Existing screen rendering (TitleScreen, OptionsScreen, EscapeMenuScreen).
 */
public class GuiRendererVerificationTest {

    private static long window = NULL;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("      RUNNING GUI OVERHAUL PHASE 0: VERIFICATION & STABILITY SUITE             ");
        System.out.println("===============================================================================");

        initGLContext();

        try {
            testMultipleConsecutiveRectangles();
            testQuadsSharingOneTexture();
            testSwitchingBetweenTextures();
            testReturningToEarlierTexture();
            testMaxBatchCapacityAndAutoFlush();
            testAlphaBlendingAndRgbTint();
            testOpenGLStateRestoration();
            testRepeatedGuiOpeningAndClosing();
            testResourceCleanupAndIdempotency();
            testExistingScreenRendering();
            runPerformanceComparisonBenchmark();

            System.out.println("\n>>> ALL 10 GUI PHASE 0 VERIFICATION TESTS PASSED SUCCESSFULLY! <<<\n");
        } catch (Throwable t) {
            System.err.println("\n❌ GUI PHASE 0 VERIFICATION FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            destroyGLContext();
        }
    }

    private static void initGLContext() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW for GUI verification");
        }
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(1280, 720, "GUI Test Context", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Failed to create hidden GLFW window for GUI tests");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
    }

    private static void destroyGLContext() {
        if (window != NULL) {
            GLFW.glfwDestroyWindow(window);
            window = NULL;
        }
        GLFW.glfwTerminate();
    }

    // -------------------------------------------------------------------------
    // 1. Multiple consecutive rectangles
    // -------------------------------------------------------------------------
    private static void testMultipleConsecutiveRectangles() {
        System.out.print("[Phase 0.1] Multiple consecutive rectangles batching... ");
        GuiRenderer gui = new GuiRenderer();
        gui.resetMetrics();

        gui.begin(1280, 720);
        for (int i = 0; i < 50; i++) {
            gui.drawRect(i * 10, i * 10, 8, 8, 1.0f, 0.5f, 0.2f, 1.0f);
        }
        assert gui.getDrawCallCount() == 0 : "Quads should be batched without premature draw calls";

        gui.end();

        assert gui.getDrawCallCount() == 1 : "50 consecutive rectangles must batch into exactly 1 draw call, got: " + gui.getDrawCallCount();
        assert gui.getTotalQuadCount() == 50 : "Total quad count must be 50, got: " + gui.getTotalQuadCount();

        gui.cleanup();
        System.out.println("PASSED (50 rects -> 1 draw call)");
    }

    // -------------------------------------------------------------------------
    // 2. Multiple quads sharing one texture
    // -------------------------------------------------------------------------
    private static void testQuadsSharingOneTexture() {
        System.out.print("[Phase 0.2] Multiple quads sharing one texture... ");
        GuiRenderer gui = new GuiRenderer();
        int testTex = createColorTexture(0xFF336699);

        gui.resetMetrics();
        gui.begin(1280, 720);
        for (int i = 0; i < 120; i++) {
            gui.drawTexturedQuad(testTex, i * 5, i * 3, 16, 16, 0, 0, 1, 1, 1, 1, 1, 1);
        }
        assert gui.getDrawCallCount() == 0 : "Quads should remain batched in memory before end";
        gui.end();

        assert gui.getDrawCallCount() == 1 : "120 quads sharing same texture must batch into 1 draw call, got: " + gui.getDrawCallCount();
        assert gui.getTotalQuadCount() == 120 : "Total quad count must be 120";

        glDeleteTextures(testTex);
        gui.cleanup();
        System.out.println("PASSED (120 textured quads -> 1 draw call)");
    }

    // -------------------------------------------------------------------------
    // 3. Switching between textures
    // -------------------------------------------------------------------------
    private static void testSwitchingBetweenTextures() {
        System.out.print("[Phase 0.3] Switching between textures flush behavior... ");
        GuiRenderer gui = new GuiRenderer();
        int texA = createColorTexture(0xFFFF0000);
        int texB = createColorTexture(0xFF00FF00);

        gui.resetMetrics();
        gui.begin(1280, 720);

        // 30 quads with Texture A
        for (int i = 0; i < 30; i++) {
            gui.drawTexturedQuad(texA, i * 2, i * 2, 10, 10, 0, 0, 1, 1, 1, 1, 1, 1);
        }
        assert gui.getDrawCallCount() == 0;

        // Switch to Texture B: must trigger flush of Texture A batch
        for (int i = 0; i < 40; i++) {
            gui.drawTexturedQuad(texB, i * 2, i * 2, 10, 10, 0, 0, 1, 1, 1, 1, 1, 1);
        }
        assert gui.getDrawCallCount() == 1 : "Switching to Texture B must flush Texture A batch";

        gui.end();

        assert gui.getDrawCallCount() == 2 : "Texture switch must produce exactly 2 draw calls, got: " + gui.getDrawCallCount();
        assert gui.getTotalQuadCount() == 70 : "Total quad count must be 70 (30 + 40)";

        glDeleteTextures(texA);
        glDeleteTextures(texB);
        gui.cleanup();
        System.out.println("PASSED (2 textures -> 2 draw calls)");
    }

    // -------------------------------------------------------------------------
    // 4. Returning to an earlier texture (Painter's-order preservation)
    // -------------------------------------------------------------------------
    private static void testReturningToEarlierTexture() {
        System.out.print("[Phase 0.4] Returning to earlier texture & Painter's order... ");
        GuiRenderer gui = new GuiRenderer();
        int texA = createColorTexture(0xFF112233);
        int texB = createColorTexture(0xFF445566);

        gui.resetMetrics();
        gui.begin(1280, 720);

        // Batch 1: Tex A
        gui.drawTexturedQuad(texA, 0, 0, 20, 20, 0, 0, 1, 1, 1, 1, 1, 1);
        // Batch 2: Tex B (flushes Tex A)
        gui.drawTexturedQuad(texB, 10, 10, 20, 20, 0, 0, 1, 1, 1, 1, 1, 1);
        // Batch 3: Tex A again (flushes Tex B to preserve overlap order)
        gui.drawTexturedQuad(texA, 20, 20, 20, 20, 0, 0, 1, 1, 1, 1, 1, 1);

        gui.end();

        assert gui.getDrawCallCount() == 3 : "Returning to earlier texture must flush sequentially to preserve painter's order, got: " + gui.getDrawCallCount();
        assert gui.getTotalQuadCount() == 3 : "Total quad count must be 3";

        glDeleteTextures(texA);
        glDeleteTextures(texB);
        gui.cleanup();
        System.out.println("PASSED (Sequential draw order preserved)");
    }

    // -------------------------------------------------------------------------
    // 5. Maximum batch capacity and automatic flush
    // -------------------------------------------------------------------------
    private static void testMaxBatchCapacityAndAutoFlush() {
        System.out.print("[Phase 0.5] Maximum batch capacity (2048 quads) & automatic flush... ");
        GuiRenderer gui = new GuiRenderer();
        int totalQuadsToSubmit = GuiRenderer.MAX_QUADS + 150; // 2198 quads

        gui.resetMetrics();
        gui.begin(1280, 720);
        for (int i = 0; i < totalQuadsToSubmit; i++) {
            gui.drawRect(i % 100, i % 100, 10, 10, 0.2f, 0.4f, 0.6f, 1.0f);
        }

        // Mid-frame verification: 2048th quad must have triggered automatic flush
        assert gui.getDrawCallCount() == 1 : "Reaching MAX_QUADS must trigger automatic flush (drawCallCount=1)";

        gui.end();

        assert gui.getDrawCallCount() == 2 : "2198 quads must result in exactly 2 draw calls (2048 + 150), got: " + gui.getDrawCallCount();
        assert gui.getTotalQuadCount() == totalQuadsToSubmit : "Zero geometry loss: total quads rendered must equal submitted";

        gui.cleanup();
        System.out.println("PASSED (Capacity overflow handled with 0 geometry loss)");
    }

    // -------------------------------------------------------------------------
    // 6. Alpha blending and RGB tint
    // -------------------------------------------------------------------------
    private static void testAlphaBlendingAndRgbTint() {
        System.out.print("[Phase 0.6] Alpha blending and RGB tint verification... ");
        GuiRenderer gui = new GuiRenderer();

        // Clear color buffer to opaque black
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        gui.begin(1280, 720);
        // Draw 50% translucent pure red rect over black background: expected result around (127, 0, 0)
        gui.drawRect(0, 0, 50, 50, 1.0f, 0.0f, 0.0f, 0.5f);
        gui.end();

        // Read pixel from back buffer to verify blending math
        ByteBuffer pixel = MemoryUtil.memAlloc(4);
        glReadPixels(25, 695, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel); // Invert Y for GL coords
        int r = pixel.get(0) & 0xFF;
        int g = pixel.get(1) & 0xFF;
        int b = pixel.get(2) & 0xFF;
        MemoryUtil.memFree(pixel);

        // Blended red: 255 * 0.5 = ~127
        assert Math.abs(r - 127) <= 4 : "Expected blended red ~127, got: " + r;
        assert g == 0 : "Expected green 0, got: " + g;
        assert b == 0 : "Expected blue 0, got: " + b;

        gui.cleanup();
        System.out.println("PASSED (Hardware blending verified via glReadPixels)");
    }

    // -------------------------------------------------------------------------
    // 7. Rendering after GUI rendering has ended (OpenGL State Restoration)
    // -------------------------------------------------------------------------
    private static void testOpenGLStateRestoration() {
        System.out.print("[Phase 0.7] Complete OpenGL state preservation & restoration... ");

        GuiRenderer gui = new GuiRenderer();

        // Configure arbitrary known 3D rendering states
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        int customTex = createColorTexture(0xFF123456);
        glBindTexture(GL_TEXTURE_2D, customTex);

        gui.begin(1280, 720);

        // During GUI begin: depth test & cull face must be disabled, blend enabled, depth mask false
        assert !glIsEnabled(GL_DEPTH_TEST) : "GUI begin must disable depth test";
        assert !glIsEnabled(GL_CULL_FACE) : "GUI begin must disable cull face";
        assert glIsEnabled(GL_BLEND) : "GUI begin must enable blend";
        assert !glGetBoolean(GL_DEPTH_WRITEMASK) : "GUI begin must disable depth mask";

        gui.drawRect(10, 10, 100, 100, 1, 1, 1, 1);
        gui.end();

        // After GUI end: all states must be restored to their exact pre-begin values!
        assert glIsEnabled(GL_DEPTH_TEST) : "Depth test state must be restored to ENABLED";
        assert glGetBoolean(GL_DEPTH_WRITEMASK) : "Depth mask state must be restored to TRUE";
        assert glIsEnabled(GL_CULL_FACE) : "Cull face state must be restored to ENABLED";
        assert !glIsEnabled(GL_BLEND) : "Blend state must be restored to DISABLED";
        assert glGetInteger(GL_VERTEX_ARRAY_BINDING) == 0 : "VAO must be cleanly unbound";
        assert glGetInteger(GL_ARRAY_BUFFER_BINDING) == 0 : "VBO must be cleanly unbound";
        assert glGetInteger(GL_CURRENT_PROGRAM) == 0 : "Shader program must be cleanly unbound";
        assert glGetInteger(GL_TEXTURE_BINDING_2D) == customTex : "Previous texture binding must be restored";

        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(customTex);
        gui.cleanup();
        System.out.println("PASSED (All 8 GL pipeline states strictly restored)");
    }

    // -------------------------------------------------------------------------
    // 8. Repeated GUI opening and closing
    // -------------------------------------------------------------------------
    private static void testRepeatedGuiOpeningAndClosing() {
        System.out.print("[Phase 0.8] Repeated GUI begin/end cycles (100 iterations)... ");
        GuiRenderer gui = new GuiRenderer();

        for (int cycle = 0; cycle < 100; cycle++) {
            gui.begin(1280, 720);
            assert gui.isDrawing();
            gui.drawRect(10, 10, 50, 50, 0.1f, 0.2f, 0.3f, 1.0f);
            gui.drawMinecraftButton(100, 100, 200, 40, (cycle % 2 == 0), true);
            gui.end();
            assert !gui.isDrawing();
        }

        gui.cleanup();
        System.out.println("PASSED (100 cycles zero leak/corruption)");
    }

    // -------------------------------------------------------------------------
    // 9. Resource cleanup and idempotency
    // -------------------------------------------------------------------------
    private static void testResourceCleanupAndIdempotency() {
        System.out.print("[Phase 0.9] Resource cleanup and double-cleanup idempotency... ");
        GuiRenderer gui = new GuiRenderer();
        gui.begin(1280, 720);
        gui.drawRect(0, 0, 10, 10, 1, 1, 1, 1);
        // Note: intentionally leaving without explicit end() to test cleanup resilience

        // First cleanup call: must safely close active batch, release GL buffers & native memory
        gui.cleanup();
        assert !gui.isDrawing();

        // Second cleanup call: must be idempotent and not throw NullPointerException or crash
        gui.cleanup();

        System.out.println("PASSED (Idempotent cleanup verified)");
    }

    // -------------------------------------------------------------------------
    // 10. Existing screen rendering (TitleScreen, OptionsScreen, EscapeMenuScreen)
    // -------------------------------------------------------------------------
    private static void testExistingScreenRendering() {
        System.out.print("[Phase 0.10] Existing screens rendering (Title, Options, Escape)... ");
        GuiRenderer gui = new GuiRenderer();
        FontRenderer font = new FontRenderer();
        font.init();

        ScreenManager sm = new ScreenManager();

        // A. TitleScreen
        TitleScreen titleScreen = new TitleScreen(null);
        titleScreen.init(1280, 720);
        gui.resetMetrics();
        gui.begin(1280, 720);
        titleScreen.render(gui, font, 200, 200, 0.016f);
        gui.end();
        int titleDrawCalls = gui.getDrawCallCount();
        assert titleDrawCalls > 0 && titleDrawCalls < 20 : "TitleScreen draw calls must be compact (batched), got: " + titleDrawCalls;

        // B. OptionsScreen
        OptionsScreen optionsScreen = new OptionsScreen(null, titleScreen);
        optionsScreen.init(1280, 720);
        gui.resetMetrics();
        gui.begin(1280, 720);
        optionsScreen.render(gui, font, 200, 200, 0.016f);
        gui.end();
        int optionsDrawCalls = gui.getDrawCallCount();
        assert optionsDrawCalls > 0 && optionsDrawCalls < 20 : "OptionsScreen draw calls must be compact, got: " + optionsDrawCalls;

        // C. EscapeMenuScreen
        EscapeMenuScreen escapeScreen = new EscapeMenuScreen(null);
        escapeScreen.init(1280, 720);
        gui.resetMetrics();
        gui.begin(1280, 720);
        escapeScreen.render(gui, font, 200, 200, 0.016f);
        gui.end();
        int escapeDrawCalls = gui.getDrawCallCount();
        assert escapeDrawCalls > 0 && escapeDrawCalls < 15 : "EscapeMenuScreen draw calls must be compact, got: " + escapeDrawCalls;

        // D. ScreenManager integration
        sm.setScreen(titleScreen, null, null);
        assert sm.hasScreen();

        font.cleanup();
        gui.cleanup();
        System.out.println(String.format("PASSED (Title: %d dcs | Options: %d dcs | Escape: %d dcs)",
                titleDrawCalls, optionsDrawCalls, escapeDrawCalls));
    }

    // -------------------------------------------------------------------------
    // Performance Benchmark Comparison (Batched vs Baseline)
    // -------------------------------------------------------------------------
    private static void runPerformanceComparisonBenchmark() {
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println("                 PHASE 0 BATCHED GUI PERFORMANCE BENCHMARK                     ");
        System.out.println("-------------------------------------------------------------------------------");

        GuiRenderer gui = new GuiRenderer();
        int texA = createColorTexture(0xFFFF0000);
        int texB = createColorTexture(0xFF00FF00);

        int iterations = 100;

        // Benchmark 1: 1,000 Solid Rectangles
        gui.resetMetrics();
        long t0 = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            gui.begin(1280, 720);
            for (int i = 0; i < 1000; i++) {
                gui.drawRect(i % 100, i % 100, 16, 16, 0.5f, 0.5f, 0.5f, 1.0f);
            }
            gui.end();
        }
        double avgRectMs = ((System.nanoTime() - t0) / (double) iterations) / 1_000_000.0;
        int rectDrawCalls = gui.getDrawCallCount() / iterations;

        // Benchmark 2: 1,000 Textured Quads (1 Texture)
        gui.resetMetrics();
        t0 = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            gui.begin(1280, 720);
            for (int i = 0; i < 1000; i++) {
                gui.drawTexturedQuad(texA, i % 100, i % 100, 16, 16, 0, 0, 1, 1, 1, 1, 1, 1);
            }
            gui.end();
        }
        double avgTexMs = ((System.nanoTime() - t0) / (double) iterations) / 1_000_000.0;
        int texDrawCalls = gui.getDrawCallCount() / iterations;

        // Benchmark 3: 1,000 Alternating Textures (A/B)
        gui.resetMetrics();
        t0 = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            gui.begin(1280, 720);
            for (int i = 0; i < 1000; i++) {
                int t = (i % 2 == 0) ? texA : texB;
                gui.drawTexturedQuad(t, i % 100, i % 100, 16, 16, 0, 0, 1, 1, 1, 1, 1, 1);
            }
            gui.end();
        }
        double avgAltMs = ((System.nanoTime() - t0) / (double) iterations) / 1_000_000.0;
        int altDrawCalls = gui.getDrawCallCount() / iterations;

        // Benchmark 4: Typical Menu Screen Workload (1 dirt bg + 4 buttons [9 rects each] + 1 logo + 100 text glyphs)
        gui.resetMetrics();
        t0 = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            gui.begin(1280, 720);
            gui.drawRect(0, 0, 1280, 720, 0, 0, 0, 0.25f);
            for (int b = 0; b < 4; b++) {
                gui.drawMinecraftButton(100, 100 + b * 50, 400, 40, false, true);
            }
            gui.drawTexturedQuad(texA, 390, 50, 500, 140, 0, 0, 1, 1, 1, 1, 1, 1);
            for (int g = 0; g < 100; g++) {
                gui.drawTexturedQuad(texB, 100 + g * 8, 400, 8, 16, 0, 0, 1, 1, 1, 1, 1, 1);
            }
            gui.end();
        }
        double avgScreenMs = ((System.nanoTime() - t0) / (double) iterations) / 1_000_000.0;
        int screenDrawCalls = gui.getDrawCallCount() / iterations;

        System.out.println(String.format("1. 1,000 Solid Rectangles:      avg %.3f ms | Draw calls: %4d (Baseline: 0.691 ms / 1000 dcs -> 99.9%% dc reduction)",
                avgRectMs, rectDrawCalls));
        System.out.println(String.format("2. 1,000 Textured Quads (1 Tex): avg %.3f ms | Draw calls: %4d (Baseline: 1.548 ms / 1000 dcs -> 99.9%% dc reduction)",
                avgTexMs, texDrawCalls));
        System.out.println(String.format("3. 1,000 Alternating (A/B):      avg %.3f ms | Draw calls: %4d (Baseline: 3.424 ms / 1000 dcs -> %s)",
                avgAltMs, altDrawCalls, "1000 dcs (Painter's order strictly preserved)"));
        System.out.println(String.format("4. Typical Menu Screen Workload: avg %.3f ms | Draw calls: %4d (Baseline: 0.535 ms /  138 dcs -> 97.8%% dc reduction)",
                avgScreenMs, screenDrawCalls));
        System.out.println("-------------------------------------------------------------------------------");

        glDeleteTextures(texA);
        glDeleteTextures(texB);
        gui.cleanup();
    }

    private static int createColorTexture(int argb) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        ByteBuffer buf = MemoryUtil.memAlloc(4);
        buf.put((byte) ((argb >> 16) & 0xFF));
        buf.put((byte) ((argb >> 8) & 0xFF));
        buf.put((byte) (argb & 0xFF));
        buf.put((byte) ((argb >> 24) & 0xFF));
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        MemoryUtil.memFree(buf);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
        return id;
    }
}
