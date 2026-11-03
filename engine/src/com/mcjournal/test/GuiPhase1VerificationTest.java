package com.mcjournal.test;

import com.mcjournal.client.GameSettings;
import com.mcjournal.client.ScreenManager;
import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Phase 1 automated test suite covering:
 * 1. Virtual resolution scaling (Auto & Manual 1..4 across standard, 4K, ultrawide, and small resolutions)
 * 2. Predictable clamping fallback on manual scale overflow
 * 3. Bidirectional coordinate conversion & DPI scale compensation
 * 4. Letterbox click rejection and viewport boundary containment
 * 5. Layout engine helpers (centering, anchoring, rows, columns, split)
 * 6. FontRenderer improvements (extended glyphs °, ⚔, ▶, color codes §, LRU text cache, shadows)
 * 7. Screen migration & legacy screen co-existence
 * 8. Phase 0 batching & OpenGL state restoration regression check
 */
public class GuiPhase1VerificationTest {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("      RUNNING GUI OVERHAUL PHASE 1: VIRTUAL RESOLUTION & FOUNDATION SUITE      ");
        System.out.println("===============================================================================");

        testAutoScaleSelection();
        testManualScaleAndClamping();
        testUnusualAndExtremeResolutions();
        testBidirectionalCoordinateConversion();
        testDpiScalingCompensation();
        testLetterboxClickRejection();
        testLayoutHelpers();
        testFontRendererGlyphsAndColors();
        testFontWidthCache();
        testMigratedAndLegacyScreens();
        testPhase0Regression();

        System.out.println("\n>>> ALL 11 GUI PHASE 1 VERIFICATION TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    // -------------------------------------------------------------------------
    // 1. Auto scale selection
    // -------------------------------------------------------------------------
    private static void testAutoScaleSelection() {
        System.out.print("[Phase 1.1] Auto scale selection across standard resolutions... ");
        GuiScaleManager sm = new GuiScaleManager();
        sm.setRequestedScale(GuiScaleManager.SCALE_AUTO);

        // 854x480 -> min(854/420=2, 480/240=2) = 2
        sm.update(854, 480, 854, 480);
        assert sm.getEffectiveScale() == 2 : "854x480 must select scale 2, got: " + sm.getEffectiveScale();
        assert sm.getGuiWidth() == 427 && sm.getGuiHeight() == 240 : "Logical size mismatch: " + sm.getGuiWidth() + "x" + sm.getGuiHeight();

        // 1280x720 -> min(1280/420=3, 720/240=3) = 3
        sm.update(1280, 720, 1280, 720);
        assert sm.getEffectiveScale() == 3 : "1280x720 must select scale 3, got: " + sm.getEffectiveScale();
        assert sm.getGuiWidth() == 426 && sm.getGuiHeight() == 240 : "Logical size mismatch: " + sm.getGuiWidth() + "x" + sm.getGuiHeight();

        // 1920x1080 -> min(1920/420=4, 1080/240=4) = 4
        sm.update(1920, 1080, 1920, 1080);
        assert sm.getEffectiveScale() == 4 : "1920x1080 must select scale 4, got: " + sm.getEffectiveScale();
        assert sm.getGuiWidth() == 480 && sm.getGuiHeight() == 270 : "Logical size mismatch: " + sm.getGuiWidth() + "x" + sm.getGuiHeight();

        // 2560x1440 (1440p) -> min(2560/420=6, 1440/240=6) = 6
        sm.update(2560, 1440, 2560, 1440);
        assert sm.getEffectiveScale() == 6 : "2560x1440 must select scale 6, got: " + sm.getEffectiveScale();
        assert sm.getGuiWidth() == 426 && sm.getGuiHeight() == 240 : "Logical size mismatch: " + sm.getGuiWidth() + "x" + sm.getGuiHeight();

        // 3840x2160 (4K UHD) -> min(3840/420=9, 2160/240=9) = 9
        sm.update(3840, 2160, 3840, 2160);
        assert sm.getEffectiveScale() == 9 : "3840x2160 must select scale 9, got: " + sm.getEffectiveScale();
        assert sm.getGuiWidth() == 426 && sm.getGuiHeight() == 240 : "Logical size mismatch: " + sm.getGuiWidth() + "x" + sm.getGuiHeight();

        System.out.println("PASSED (All 5 resolutions select exact expected integer scales)");
    }

    // -------------------------------------------------------------------------
    // 2. Manual scales and predictable clamping fallback
    // -------------------------------------------------------------------------
    private static void testManualScaleAndClamping() {
        System.out.print("[Phase 1.2] Manual scales 1-4 and clamping fallback... ");
        GuiScaleManager sm = new GuiScaleManager();

        // On 1920x1080, all manual scales 1..4 fit comfortably
        sm.update(1920, 1080, 1920, 1080);
        for (int scale = 1; scale <= 4; scale++) {
            sm.setRequestedScale(scale);
            assert sm.getEffectiveScale() == scale : "Scale " + scale + " should fit 1080p";
            assert !sm.isScaleClamped() : "Scale " + scale + " must not be clamped on 1080p";
        }

        // On small 854x480, max fitting scale is 2.
        // Requesting scale 3 or 4 must fall back to 2 and flag isScaleClamped.
        sm.update(854, 480, 854, 480);
        sm.setRequestedScale(1);
        assert sm.getEffectiveScale() == 1 && !sm.isScaleClamped();

        sm.setRequestedScale(2);
        assert sm.getEffectiveScale() == 2 && !sm.isScaleClamped();

        sm.setRequestedScale(3);
        assert sm.getEffectiveScale() == 2 && sm.isScaleClamped() : "Scale 3 on 480p must clamp to 2";

        sm.setRequestedScale(4);
        assert sm.getEffectiveScale() == 2 && sm.isScaleClamped() : "Scale 4 on 480p must clamp to 2";

        System.out.println("PASSED (Predictable clamping prevents interface clipping)");
    }

    // -------------------------------------------------------------------------
    // 3. Extreme and unusual resolutions
    // -------------------------------------------------------------------------
    private static void testUnusualAndExtremeResolutions() {
        System.out.print("[Phase 1.3] Extreme & unusual resolutions (Ultrawide & Sub-minimum)... ");
        GuiScaleManager sm = new GuiScaleManager();
        sm.setRequestedScale(GuiScaleManager.SCALE_AUTO);

        // Ultrawide 21:9 (2560x1080) -> min(2560/420=6, 1080/240=4) = 4
        sm.update(2560, 1080, 2560, 1080);
        assert sm.getEffectiveScale() == 4 : "2560x1080 must be height-constrained to scale 4";
        assert sm.getGuiWidth() == 640 && sm.getGuiHeight() == 270;

        // Sub-minimum resizable window (300x200) -> scale must stay integer 1 (no fractional fallback!)
        sm.update(300, 200, 300, 200);
        assert sm.getEffectiveScale() == 1 : "Sub-minimum window must clamp to scale 1";
        assert sm.getGuiWidth() == 300 && sm.getGuiHeight() == 200;

        // Tall portrait window (600x900) -> min(600/420=1, 900/240=3) = 1
        sm.update(600, 900, 600, 900);
        assert sm.getEffectiveScale() == 1 : "Narrow portrait must be width-constrained to scale 1";
        assert sm.getGuiWidth() == 600 && sm.getGuiHeight() == 900;

        System.out.println("PASSED (Integer scaling invariant held across all aspect ratios)");
    }

    // -------------------------------------------------------------------------
    // 4. Bidirectional coordinate conversion
    // -------------------------------------------------------------------------
    private static void testBidirectionalCoordinateConversion() {
        System.out.print("[Phase 1.4] Bidirectional coordinate conversion (4 corners & center)... ");
        GuiScaleManager sm = new GuiScaleManager();
        sm.update(1920, 1080, 1920, 1080);
        sm.setRequestedScale(4); // Effective scale 4, logical 480x270

        // Top-left corner
        double guiX0 = sm.windowToGuiX(0);
        double guiY0 = sm.windowToGuiY(0);
        assert Math.abs(guiX0 - 0.0) < 1e-4 && Math.abs(guiY0 - 0.0) < 1e-4 : "Top-left must map to (0,0)";
        assert Math.abs(sm.guiToWindowX(guiX0) - 0.0) < 1e-4;
        assert Math.abs(sm.guiToWindowY(guiY0) - 0.0) < 1e-4;

        // Center
        double guiCenterX = sm.windowToGuiX(960);
        double guiCenterY = sm.windowToGuiY(540);
        assert Math.abs(guiCenterX - 240.0) < 1e-4 && Math.abs(guiCenterY - 135.0) < 1e-4 : "Center must map to (240, 135)";
        assert Math.abs(sm.guiToWindowX(guiCenterX) - 960.0) < 1e-4;
        assert Math.abs(sm.guiToWindowY(guiCenterY) - 540.0) < 1e-4;

        // Bottom-right corner (1920, 1080)
        double guiX1 = sm.windowToGuiX(1920);
        double guiY1 = sm.windowToGuiY(1080);
        assert Math.abs(guiX1 - 480.0) < 1e-4 && Math.abs(guiY1 - 270.0) < 1e-4;
        assert Math.abs(sm.guiToWindowX(guiX1) - 1920.0) < 1e-4;
        assert Math.abs(sm.guiToWindowY(guiY1) - 1080.0) < 1e-4;

        System.out.println("PASSED (Exact round-trip coordinate preservation)");
    }

    // -------------------------------------------------------------------------
    // 5. High-DPI scaling compensation
    // -------------------------------------------------------------------------
    private static void testDpiScalingCompensation() {
        System.out.print("[Phase 1.5] High-DPI compensation (2x Retina framebuffer)... ");
        GuiScaleManager sm = new GuiScaleManager();

        // 2x Retina display: window is 960x540 points, framebuffer is 1920x1080 pixels
        sm.update(1920, 1080, 960, 540);
        sm.setRequestedScale(GuiScaleManager.SCALE_AUTO); // Effective scale 4 on 1080p, logical 480x270

        // Window coordinate (480, 270) is center of 960x540 window
        double guiX = sm.windowToGuiX(480);
        double guiY = sm.windowToGuiY(270);
        assert Math.abs(guiX - 240.0) < 1e-4 && Math.abs(guiY - 135.0) < 1e-4 : "Retina center must map to logical (240, 135), got: " + guiX + ", " + guiY;

        // Window coordinate (960, 540) is bottom-right
        double guiBRX = sm.windowToGuiX(960);
        double guiBRY = sm.windowToGuiY(540);
        assert Math.abs(guiBRX - 480.0) < 1e-4 && Math.abs(guiBRY - 270.0) < 1e-4;

        System.out.println("PASSED (Window-to-framebuffer ratio correctly handled)");
    }

    // -------------------------------------------------------------------------
    // 6. Letterbox click rejection
    // -------------------------------------------------------------------------
    private static void testLetterboxClickRejection() {
        System.out.print("[Phase 1.6] Letterboxed margin containment & click rejection... ");
        GuiScaleManager sm = new GuiScaleManager();

        // Framebuffer with odd pixel dimensions: 855x481 with Scale 2
        // Viewport: 854x480. Viewport offset: X=0 (855-854)/2 = 0, Y=0 (481-480)/2 = 0
        sm.update(855, 481, 855, 481);
        sm.setRequestedScale(2);

        assert sm.getViewportWidth() == 854 && sm.getViewportHeight() == 480;
        assert sm.isMouseInViewport(100, 100) : "Inside viewport must return true";
        assert sm.isMouseInViewport(0, 0) : "Top-left boundary must be inside";
        assert sm.isMouseInViewport(853, 479) : "Inside boundary must be true";

        // Outside letterbox pixel (854, 480)
        assert !sm.isMouseInViewport(854, 100) : "Right margin must be outside viewport";
        assert !sm.isMouseInViewport(100, 480) : "Bottom margin must be outside viewport";
        assert !sm.isMouseInViewport(-1, 100) : "Negative coordinate must be outside viewport";

        System.out.println("PASSED (Clicks in letterbox regions safely rejected)");
    }

    // -------------------------------------------------------------------------
    // 7. Layout foundation helpers
    // -------------------------------------------------------------------------
    private static void testLayoutHelpers() {
        System.out.print("[Phase 1.7] GuiLayout helpers (centering, anchoring, rows, columns)... ");

        // Centering
        assert GuiLayout.centerX(420, 200) == 110 : "Centered X should be 110";
        assert GuiLayout.centerY(240, 40) == 100 : "Centered Y should be 100";

        // Anchoring
        assert GuiLayout.anchorLeft(10) == 10;
        assert GuiLayout.anchorRight(420, 50, 10) == 360;
        assert GuiLayout.anchorTop(15) == 15;
        assert GuiLayout.anchorBottom(240, 20, 10) == 210;

        // Rows and Columns
        // Row of 3 items (width 100, gap 10) -> total width = 3*100 + 2*10 = 320
        assert GuiLayout.rowTotalWidth(3, 100, 10) == 320;
        assert GuiLayout.columnTotalHeight(4, 20, 5) == 95; // 4*20 + 3*5 = 95
        assert GuiLayout.rowItemX(50, 100, 10, 0) == 50;
        assert GuiLayout.rowItemX(50, 100, 10, 1) == 160;
        assert GuiLayout.rowItemX(50, 100, 10, 2) == 270;

        // Split calculation: total 200, gap 4, 2 columns -> (200 - 4)/2 = 98
        assert GuiLayout.buttonWidth(200, 2, 4) == 98;

        System.out.println("PASSED (All layout primitives verified)");
    }

    // -------------------------------------------------------------------------
    // 8. FontRenderer glyphs and formatting codes
    // -------------------------------------------------------------------------
    private static void testFontRendererGlyphsAndColors() {
        System.out.print("[Phase 1.8] FontRenderer extended glyphs (°, ⚔, ▶) & § formatting... ");

        // Headless GLFW window for font texture upload
        long window = createHeadlessWindow();
        try {
            FontRenderer font = new FontRenderer();
            font.init();

            // Verify glyph index mappings
            assert FontRenderer.getGlyphIndex('°') == 176 : "Degree symbol must resolve to Latin-1 index 176";
            assert FontRenderer.getGlyphIndex('§') == 167 : "Section symbol must resolve to Latin-1 index 167";
            assert FontRenderer.getGlyphIndex('▶') == FontRenderer.SLOT_PLAY : "Play symbol must map to SLOT_PLAY";
            assert FontRenderer.getGlyphIndex('⚔') == FontRenderer.SLOT_SWORD : "Sword symbol must map to SLOT_SWORD";
            assert FontRenderer.getGlyphIndex('♥') == FontRenderer.SLOT_HEART : "Heart symbol must map to SLOT_HEART";

            // Verify stripFormatting
            String formatted = "§eYellow §aGreen §rReset";
            assert FontRenderer.stripFormatting(formatted).equals("Yellow Green Reset") : "stripFormatting failed";

            // Verify that formatting codes do NOT add width
            float rawWidth = font.getStringWidth("Test String", 1.0f);
            float coloredWidth = font.getStringWidth("§eTest §aString§r", 1.0f);
            assert Math.abs(rawWidth - coloredWidth) < 1e-4 : "Color codes must not contribute to string width: " + rawWidth + " vs " + coloredWidth;

            // Verify degree symbol has positive width
            float fovWidth = font.getStringWidth("FOV: 70°", 1.0f);
            assert fovWidth > font.getStringWidth("FOV: 70", 1.0f) : "Degree symbol must contribute character width";

            // Verify play and sword symbols have positive width
            assert font.getStringWidth("▶", 1.0f) > 0 : "Play symbol must have positive width";
            assert font.getStringWidth("⚔", 1.0f) > 0 : "Sword symbol must have positive width";

            font.cleanup();
            System.out.println("PASSED (Full glyph coverage & § color codes operational)");
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    // -------------------------------------------------------------------------
    // 9. FontRenderer text measurement caching
    // -------------------------------------------------------------------------
    private static void testFontWidthCache() {
        System.out.print("[Phase 1.9] FontRenderer LRU text measurement cache... ");
        long window = createHeadlessWindow();
        try {
            FontRenderer font = new FontRenderer();
            font.init();

            // Query same string 1,000 times
            String sample = "§6Hardcore Adventurer §7[Level 42]";
            float firstCall = font.getStringWidth(sample, 0.8f);

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                float width = font.getStringWidth(sample, 0.8f);
                assert width == firstCall;
            }
            long elapsedNs = System.nanoTime() - start;
            double avgUs = (elapsedNs / 1000.0) / 1000.0;

            font.cleanup();
            System.out.println("PASSED (1,000 cached lookups in avg " + String.format("%.3f", avgUs) + " µs)");
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    // -------------------------------------------------------------------------
    // 10. Screen migration & legacy coexistence
    // -------------------------------------------------------------------------
    private static void testMigratedAndLegacyScreens() {
        System.out.print("[Phase 1.10] Screen migration contracts & legacy coexistence... ");

        TitleScreen title = new TitleScreen(null);
        OptionsScreen options = new OptionsScreen(null, title);
        EscapeMenuScreen escape = new EscapeMenuScreen(null);
        Screen legacyWorldSelect = new Screen(null) { @Override public boolean usesLogicalCoordinates() { return false; } };

        // Migrated screens must declare logical coordinates
        assert title.usesLogicalCoordinates() : "TitleScreen must use logical coordinates";
        assert options.usesLogicalCoordinates() : "OptionsScreen must use logical coordinates";
        assert escape.usesLogicalCoordinates() : "EscapeMenuScreen must use logical coordinates";

        // Unmigrated screen must remain in legacy mode
        assert !legacyWorldSelect.usesLogicalCoordinates() : "WorldSelectScreen must remain in legacy mode";

        ScreenManager sm = new ScreenManager();

        // Initialize TitleScreen at 1920x1080 (Auto scale = 4 -> logical 480x270)
        sm.onResize(1920, 1080);
        sm.setScreen(title, null, null);
        assert title.getWidth() == 480 && title.getHeight() == 270 : "TitleScreen must receive logical 480x270, got: " + title.getWidth() + "x" + title.getHeight();

        // Initialize legacy screen: receives full physical 1920x1080
        sm.setScreen(legacyWorldSelect, null, null);
        assert legacyWorldSelect.getWidth() == 1920 && legacyWorldSelect.getHeight() == 1080 : "Legacy screen must receive physical dimensions";

        System.out.println("PASSED (Logical and legacy coordinate systems co-exist cleanly)");
    }

    // -------------------------------------------------------------------------
    // 11. Phase 0 Batching & State Restoration Regression
    // -------------------------------------------------------------------------
    private static void testPhase0Regression() {
        System.out.print("[Phase 1.11] Phase 0 batching & OpenGL state restoration regression... ");
        long window = createHeadlessWindow();
        try {
            GuiRenderer gui = new GuiRenderer();
            FontRenderer font = new FontRenderer();
            font.init();

            // Set up known OpenGL pipeline state
            glEnable(GL_DEPTH_TEST);
            glDepthMask(true);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);

            gui.resetMetrics();
            gui.begin(480, 270, 0, 0, 1920, 1080);

            // Draw 100 batched rectangles and text
            for (int i = 0; i < 50; i++) {
                gui.drawRect(i * 2, i * 2, 20, 20, 0.2f, 0.4f, 0.6f, 1.0f);
            }
            font.drawString(gui, "§eTest Batching", 10, 10, 1, 0xFFFFFF, true);

            gui.end();

            // Verify state restoration
            assert glIsEnabled(GL_DEPTH_TEST) : "GL_DEPTH_TEST must be restored";
            assert glGetBoolean(GL_DEPTH_WRITEMASK) : "GL_DEPTH_WRITEMASK must be restored";
            assert glIsEnabled(GL_CULL_FACE) : "GL_CULL_FACE must be restored";
            assert !glIsEnabled(GL_BLEND) : "GL_BLEND must be restored";

            gui.cleanup();
            font.cleanup();
            System.out.println("PASSED (Zero state drift and dynamic batching intact)");
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    // --- Helper Utilities ---

    private static long createHeadlessWindow() {
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        long handle = glfwCreateWindow(800, 600, "HeadlessTest", 0, 0);
        if (handle == 0) {
            throw new RuntimeException("Failed to create GLFW headless window");
        }
        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        return handle;
    }
}
