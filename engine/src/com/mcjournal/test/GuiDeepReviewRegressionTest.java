package com.mcjournal.test;

import com.mcjournal.client.ScreenManager;
import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Comprehensive regression test suite validating the 9 GUI Deep Review findings:
 * - G01: Incomplete OpenGL Texture Binding Restoration (Texture unit 0 and active texture unit)
 * - G02: Window Dimensions vs Framebuffer Dimensions Conflation (DPI / HiDPI coordinate mapping)
 * - G03: Screen Resizing State Preservation (Search queries, form inputs, focus, and stacked dialogs)
 * - G04: Focus Lifecycle Management (Clearing on non-focusable widget clicks, disabled/hidden state)
 * - G05: Supplementary Unicode Character Input & Caret/Surrogate Safety
 * - G06: Atomic Text Replacement (Selection preservation on invalid input or filter failure)
 * - G08: Actual Framebuffer Height in OpenGL Coordinate Calculations vs Sub-Viewports
 */
public class GuiDeepReviewRegressionTest {

    private static long glWindow = NULL;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("     RUNNING GUI DEEP REVIEW: FINDINGS & REGRESSION VERIFICATION SUITE         ");
        System.out.println("===============================================================================");

        initGLContext();

        try {
            testG01TextureBindingRestoration();
            testG02DpiScalingAndCoordinateMapping();
            testG03ScreenResizeStatePreservation();
            testG04FocusManagementLifecycle();
            testG05SupplementaryUnicodeHandling();
            testG06AtomicTextReplacement();
            testG08FramebufferVsViewportHeight();

            System.out.println("\n>>> ALL GUI DEEP REVIEW REGRESSION TESTS PASSED SUCCESSFULLY! <<<\n");
        } catch (Throwable t) {
            System.err.println("\n❌ GUI DEEP REVIEW REGRESSION SUITE FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            destroyGLContext();
        }
    }

    private static void initGLContext() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW for regression tests");
        }
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        glWindow = GLFW.glfwCreateWindow(1280, 720, "GUI Regression Context", NULL, NULL);
        if (glWindow == NULL) {
            throw new IllegalStateException("Failed to create hidden GLFW window for GUI tests");
        }
        GLFW.glfwMakeContextCurrent(glWindow);
        GL.createCapabilities();
    }

    private static void destroyGLContext() {
        if (glWindow != NULL) {
            GLFW.glfwDestroyWindow(glWindow);
            glWindow = NULL;
        }
        GLFW.glfwTerminate();
    }

    // -------------------------------------------------------------------------
    // G01: OpenGL Texture Binding Restoration (Texture 0 and active texture)
    // -------------------------------------------------------------------------
    private static void testG01TextureBindingRestoration() {
        System.out.print("[G01] Incomplete OpenGL texture binding restoration on unit 0 & active unit... ");

        int texA = glGenTextures();
        int texB = glGenTextures();

        try {
            // Establish pre-GUI state:
            // Texture unit 0 has Texture B bound
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texB);

            // Active texture unit is GL_TEXTURE1, and has Texture A bound
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, texA);

            GuiRenderer gui = new GuiRenderer();
            gui.begin(854, 480);
            gui.drawRect(10, 10, 50, 50, 1.0f, 1.0f, 1.0f, 1.0f);
            gui.end();

            // Verification after GUI rendering:
            int postActiveUnit = glGetInteger(GL_ACTIVE_TEXTURE);
            assert postActiveUnit == GL_TEXTURE1 : "Active texture unit must be restored to GL_TEXTURE1, got: " + postActiveUnit;

            int postBindingUnit1 = glGetInteger(GL_TEXTURE_BINDING_2D);
            assert postBindingUnit1 == texA : "Texture on active unit 1 must be restored to texA (" + texA + "), got: " + postBindingUnit1;

            glActiveTexture(GL_TEXTURE0);
            int postBindingUnit0 = glGetInteger(GL_TEXTURE_BINDING_2D);
            assert postBindingUnit0 == texB : "Texture on unit 0 must be restored to texB (" + texB + "), got: " + postBindingUnit0;

            gui.cleanup();
            System.out.println("PASSED");
        } finally {
            glDeleteTextures(texA);
            glDeleteTextures(texB);
        }
    }

    // -------------------------------------------------------------------------
    // G02: Window Dimensions and Framebuffer Dimensions Separation (HiDPI)
    // -------------------------------------------------------------------------
    private static void testG02DpiScalingAndCoordinateMapping() {
        System.out.print("[G02] Window vs Framebuffer dimensions separation across HiDPI/scaling... ");

        int[][] configs = {
                {1280, 720, 1280, 720},   // 1x scale (Standard)
                {2560, 1440, 1280, 720},  // 2x HiDPI / Retina scale
                {3840, 2160, 1920, 1080}  // 2x 4K UHD scale
        };

        for (int[] cfg : configs) {
            int fbW = cfg[0];
            int fbH = cfg[1];
            int winW = cfg[2];
            int winH = cfg[3];

            GuiScaleManager scaleMgr = new GuiScaleManager();
            scaleMgr.setRequestedScale(GuiScaleManager.SCALE_AUTO);
            scaleMgr.update(fbW, fbH, winW, winH);

            int guiW = scaleMgr.getGuiWidth();
            int guiH = scaleMgr.getGuiHeight();

            // Create a test screen with a button located exactly at center
            AtomicBoolean buttonClicked = new AtomicBoolean(false);
            float btnW = 100;
            float btnH = 20;
            float btnX = (guiW - btnW) / 2.0f;
            float btnY = (guiH - btnH) / 2.0f;

            Screen testScreen = new Screen(null) {
                @Override
                public boolean usesLogicalCoordinates() {
                    return true;
                }

                @Override
                public void init(int w, int h) {
                    super.init(w, h);
                    Button btn = new Button(1, "Test", btnX, btnY, btnW, btnH, () -> buttonClicked.set(true));
                    addWidget(btn);
                }
            };

            ScreenManager sm = new ScreenManager();
            sm.onResize(fbW, fbH, winW, winH);
            sm.setScreen(testScreen, null);

            // Compute the GLFW window coordinate of the button's exact center
            double logicalCenterX = btnX + btnW / 2.0;
            double logicalCenterY = btnY + btnH / 2.0;

            double winCenterX = scaleMgr.guiToWindowX(logicalCenterX);
            double winCenterY = scaleMgr.guiToWindowY(logicalCenterY);

            // Verify bidirectional conversion round-trip
            double mappedGuiX = scaleMgr.windowToGuiX(winCenterX);
            double mappedGuiY = scaleMgr.windowToGuiY(winCenterY);
            assert Math.abs(mappedGuiX - logicalCenterX) < 0.01 : "Window to Gui X mapping drift: " + mappedGuiX + " vs " + logicalCenterX;
            assert Math.abs(mappedGuiY - logicalCenterY) < 0.01 : "Window to Gui Y mapping drift: " + mappedGuiY + " vs " + logicalCenterY;

            // Deliver click in GLFW window coordinates
            buttonClicked.set(false);
            boolean handled = sm.handleMouseClick(winCenterX, winCenterY, 0);
            assert handled : "Mouse click at element center must be handled for config: fb=" + fbW + "x" + fbH + ", win=" + winW + "x" + winH;
            assert buttonClicked.get() : "Button must be activated by click at center for config: fb=" + fbW + "x" + fbH + ", win=" + winW + "x" + winH;
        }

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // G03: Screen Resizing Preserves Widget State
    // -------------------------------------------------------------------------
    private static void testG03ScreenResizeStatePreservation() {
        System.out.print("[G03] Screen resizing preserves widget state, search queries & modal stack... ");

        ScreenManager sm = new ScreenManager();
        sm.onResize(1920, 1080, 1920, 1080);

        // 1. WorldSelectScreen state preservation
        WorldSelectScreen selectScreen = new WorldSelectScreen(null);
        sm.setScreen(selectScreen, null);

        TextField search = selectScreen.getSearchField();
        assert search != null : "Search field must exist";
        search.setText("MyAdventure");
        search.setCaretPosition(11);
        selectScreen.getFocusManager().setFocus(search);

        // Resize window to different resolution
        sm.onResize(1280, 720, 1280, 720);

        // Verify search query, caret, and focus were preserved
        TextField postResizeSearch = selectScreen.getSearchField();
        assert postResizeSearch.getText().equals("MyAdventure") : "Search query must be preserved on resize, got: " + postResizeSearch.getText();
        assert postResizeSearch.getCaretPosition() == 11 : "Caret position must be preserved on resize";
        assert selectScreen.getFocusManager().getFocusedWidget() == postResizeSearch : "Focus must remain on search field after resize";

        // 2. Modal dialog stacked on top of WorldSelectScreen
        AtomicBoolean confirmed = new AtomicBoolean(false);
        ConfirmScreen confirmModal = new ConfirmScreen(null, "Delete World", "Are you sure?", () -> confirmed.set(true), null);
        sm.pushScreen(confirmModal);
        assert sm.getStackDepth() == 2;
        assert sm.getCurrentScreen() == confirmModal;

        // Resize while modal is open
        sm.onResize(2560, 1440, 2560, 1440);

        // Pop modal and verify underlying WorldSelectScreen still has its query intact
        sm.popScreen();
        assert sm.getCurrentScreen() == selectScreen;
        assert selectScreen.getSearchField().getText().equals("MyAdventure") : "Search query must survive modal dialog open/close and resize";

        // 3. WorldCreateScreen state preservation
        WorldCreateScreen createScreen = new WorldCreateScreen(null);
        sm.setScreen(createScreen, null);

        createScreen.getNameField().setText("Custom Journal World");
        createScreen.getSeedField().setText("987654321");
        createScreen.getFocusManager().setFocus(createScreen.getNameField());

        sm.onResize(1366, 768, 1366, 768);

        assert createScreen.getNameField().getText().equals("Custom Journal World") : "Name field must be preserved on resize";
        assert createScreen.getSeedField().getText().equals("987654321") : "Seed field must be preserved on resize";
        assert createScreen.getFocusManager().getFocusedWidget() == createScreen.getNameField() : "Focus must remain on name field";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // G04: Focus Lifecycle Management
    // -------------------------------------------------------------------------
    private static void testG04FocusManagementLifecycle() {
        System.out.print("[G04] Focus lifecycle management on non-focusable clicks, disabled & hidden widgets... ");

        FocusManager fm = new FocusManager();
        List<Widget> widgets = new ArrayList<>();

        TextField textField = new TextField(1, 10, 10, 100, 20);
        textField.setFocusManager(fm);
        widgets.add(textField);

        Label label = new Label("Status: Ready", 10, 50, 0xFFFFFF);
        label.setFocusable(false);
        label.setFocusManager(fm);
        widgets.add(label);

        Button button = new Button(2, "Click", 10, 80, 80, 20, () -> {});
        button.setFocusManager(fm);
        widgets.add(button);

        // 1. Focus text field
        fm.setFocus(textField);
        assert fm.getFocusedWidget() == textField : "Text field must be focused";

        // 2. Click on non-focusable Label -> Focus must be cleared
        fm.handleClick(widgets, 15, 55); // Over label
        assert fm.getFocusedWidget() == null : "Focus must be cleared when clicking a non-focusable widget (Label)";

        // 3. Focus text field, click empty space -> Focus must be cleared
        fm.setFocus(textField);
        assert fm.getFocusedWidget() == textField;
        fm.handleClick(widgets, 200, 200); // Empty space
        assert fm.getFocusedWidget() == null : "Focus must be cleared when clicking empty space";

        // 4. Focus text field, disable widget -> Focus must be automatically cleared
        fm.setFocus(textField);
        assert fm.getFocusedWidget() == textField;
        textField.setEnabled(false);
        assert fm.getFocusedWidget() == null : "Focus must be cleared when focused widget is disabled";

        // 5. Re-enable, focus, make invisible -> Focus must be cleared
        textField.setEnabled(true);
        fm.setFocus(textField);
        assert fm.getFocusedWidget() == textField;
        textField.setVisible(false);
        assert fm.getFocusedWidget() == null : "Focus must be cleared when focused widget is made invisible";

        // 6. Focusable toggling
        textField.setVisible(true);
        fm.setFocus(textField);
        assert fm.getFocusedWidget() == textField;
        textField.setFocusable(false);
        assert fm.getFocusedWidget() == null : "Focus must be cleared when widget is set non-focusable";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // G05: Supplementary Unicode Character Input & Caret/Surrogate Safety
    // -------------------------------------------------------------------------
    private static void testG05SupplementaryUnicodeHandling() {
        System.out.print("[G05] Supplementary Unicode characters, emoji entry & surrogate pair safety... ");

        TextField field = new TextField(1, 0, 0, 200, 20);
        field.setMaxLength(64);
        field.setFocused(true);

        // 1. Type ASCII and supplementary emoji (😀 U+1F600, 2 code units: \uD83D\uDE00)
        field.setText("Hello ");
        field.setCaretPosition(6);

        // Simulate typing supplementary emoji code point (0x1F600)
        field.charTyped(0x1F600);
        assert field.getText().equals("Hello 😀") : "Text must contain supplementary emoji, got: " + field.getText();
        assert field.getCaretPosition() == 8 : "Caret must be at end of emoji (6 + 2 = 8), got: " + field.getCaretPosition();

        // 2. Single Backspace must remove the entire surrogate pair (2 code units), not split it!
        field.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
        assert field.getText().equals("Hello ") : "Backspace must remove entire emoji atomically, got: '" + field.getText() + "'";
        assert field.getCaretPosition() == 6 : "Caret must be at 6, got: " + field.getCaretPosition();

        // 3. Delete before supplementary character
        field.setText("A😀B");
        field.setCaretPosition(1); // Right before emoji
        field.keyPressed(GLFW.GLFW_KEY_DELETE, 0, GLFW.GLFW_PRESS, 0);
        assert field.getText().equals("AB") : "Delete before emoji must delete entire surrogate pair, got: " + field.getText();
        assert field.getCaretPosition() == 1 : "Caret must stay at 1";

        // 4. Cursor movement must never split surrogate pair
        field.setText("A😀B");
        field.setCaretPosition(1);
        field.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_PRESS, 0);
        assert field.getCaretPosition() == 3 : "Right arrow must jump over entire emoji to index 3, got: " + field.getCaretPosition();
        field.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, 0);
        assert field.getCaretPosition() == 1 : "Left arrow must jump back over entire emoji to index 1, got: " + field.getCaretPosition();

        // 5. Clipboard paste containing emoji
        field.setText("");
        field.insertText("Journal 🚀 Notes 📝");
        assert field.getText().equals("Journal 🚀 Notes 📝") : "Paste with emoji must succeed, got: " + field.getText();

        // 6. Tamil Unicode characters (BMP complex script \u0B80-\u0BFF)
        field.setText("");
        // Simulate typing Tamil code point 'த' (U+0BA4)
        field.charTyped(0x0BA4);
        assert field.getText().equals("த") : "Must handle Tamil charTyped input";
        assert field.getCaretPosition() == 1 : "Caret must advance by 1 for BMP Tamil char";

        // Clipboard paste of Tamil phrase "தமிழ் குறிப்பேடு" (Tamil Journal, 16 code units)
        field.insertText("மிழ் குறிப்பேடு");
        assert field.getText().equals("தமிழ் குறிப்பேடு") : "Full Tamil string must match, got: " + field.getText();
        assert field.getCaretPosition() == 16 : "Caret position must track Tamil code units, got: " + field.getCaretPosition();

        // Caret navigation and deletion in Tamil text
        field.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
        assert field.getText().equals("தமிழ் குறிப்பேட") : "Backspace must delete trailing Tamil character";

        // Select and replace Tamil word
        field.setText("தமிழ்");
        field.setCaretPosition(5);
        field.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_SHIFT);
        field.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_SHIFT);
        assert field.hasSelection();
        field.insertText("நெறி");
        assert field.getText().equals("தமிநெறி") : "Selection replacement in Tamil must succeed, got: " + field.getText();

        // Font rendering and measurement fallback for Tamil and supplementary characters
        FontRenderer testFont = new FontRenderer();
        testFont.init();
        float tamilWidth = testFont.getStringWidth("தமிழ் 📔", 1.0f);
        assert tamilWidth > 0 : "Tamil text with emoji must have positive layout width, got: " + tamilWidth;

        GuiRenderer testGui = new GuiRenderer();
        testGui.begin(400, 300);
        testFont.drawString(testGui, "தமிழ் 📔", 10, 10, TextStyle.BODY);
        testGui.end();
        testGui.cleanup();
        testFont.cleanup();

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // G06: Atomic Text Replacement
    // -------------------------------------------------------------------------
    private static void testG06AtomicTextReplacement() {
        System.out.print("[G06] Atomic text replacement preserving selection on validation failure... ");

        TextField field = new TextField(1, 0, 0, 200, 20);
        field.setMaxLength(30);
        field.setFocused(true);

        // Filter: only letters and spaces allowed
        field.setFilter(text -> text.matches("[a-zA-Z ]*"));
        field.setText("Minecraft Journal");

        // Select the word "Journal" (indices 10 to 17)
        field.setCaretPosition(17);
        // Emulate Shift+Left 7 times to select "Journal"
        for (int i = 0; i < 7; i++) {
            field.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_SHIFT);
        }
        assert field.hasSelection() : "Field must have selection";
        assert field.getSelectedText().equals("Journal") : "Selected text must be 'Journal', got: " + field.getSelectedText();

        // Attempt replacing selection with invalid digits (rejected by filter)
        field.insertText("12345");

        // The candidate "Minecraft 12345" fails filter; the original text and selection MUST remain untouched!
        assert field.getText().equals("Minecraft Journal") : "Text must not be modified when replacement is rejected, got: " + field.getText();
        assert field.hasSelection() : "Selection must remain active when replacement is rejected";
        assert field.getSelectedText().equals("Journal") : "Selection text must still be 'Journal', got: " + field.getSelectedText();

        // Now replace the selection with a VALID string
        field.insertText("App");
        assert field.getText().equals("Minecraft App") : "Valid replacement must succeed, got: " + field.getText();
        assert !field.hasSelection() : "Selection must be cleared after successful replacement";

        // Attempt inserting text when field is already at max capacity
        field.setMaxLength(13); // "Minecraft App" is 13 chars
        field.setCaretPosition(13);
        field.insertText("Exceeding");
        assert field.getText().equals("Minecraft App") : "Text must not exceed maxLength when field is full";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // G08: Actual Framebuffer Height in Sub-Viewport Configurations
    // -------------------------------------------------------------------------
    private static void testG08FramebufferVsViewportHeight() {
        System.out.print("[G08] Actual framebuffer height coordinate conversion vs sub-viewports... ");

        GuiRenderer gui = new GuiRenderer();

        // Simulate a prior pass configuring a sub-viewport (640x360 within a 1280x720 framebuffer)
        glViewport(0, 0, 640, 360);

        // Call begin passing explicit framebufferHeight=720, with GUI viewport centered
        gui.begin(427, 240, 0, 0, 1280, 720, 720);
        gui.drawRect(0, 0, 100, 100, 0.5f, 0.5f, 0.5f, 1.0f);
        gui.end();

        // Verify that sub-viewport before GUI was cleanly restored
        int[] vp = new int[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        assert vp[0] == 0 && vp[1] == 0 && vp[2] == 640 && vp[3] == 360 :
                "Previous sub-viewport must be restored, got: " + vp[0] + "," + vp[1] + "," + vp[2] + "," + vp[3];

        gui.cleanup();
        System.out.println("PASSED");
    }
}
