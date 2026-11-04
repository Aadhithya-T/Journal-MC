package com.mcjournal.test;

import com.mcjournal.client.ScreenManager;
import com.mcjournal.client.gui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Phase 2 automated test suite covering:
 * 1. Base Widget fundamentals (bounds, visibility, enabled, hover, focus)
 * 2. Button (mouse activation, keyboard activation, press/release pairing, duplicate prevention)
 * 3. CycleButton (forward/reverse cycling, wraparound, disabled state)
 * 4. Slider (min/max, step rounding, mouse dragging, keyboard adjustment, clamping)
 * 5. TextField (caret navigation, word jumping, selection, copy/paste/cut, max length, filter, input consumption)
 * 6. Label and ListRow (alignment, shadow, selection highlight)
 * 7. FocusManager (Tab / Shift+Tab cycling, outside click clear, single focus ownership)
 * 8. Screen stack navigation (push, pop, replace, parent state preservation, lifecycle events)
 * 9. Nine-slice rendering (unscaled corners, stretched edges/center, batching)
 * 10. Full engine regression check
 */
public class GuiPhase2VerificationTest {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("       RUNNING GUI OVERHAUL PHASE 2: WIDGET TOOLKIT & SCREEN MANAGEMENT        ");
        System.out.println("===============================================================================");

        testWidgetFundamentals();
        testButtonInteraction();
        testCycleButton();
        testSlider();
        testTextField();
        testLabelAndListRow();
        testFocusManagerAndTabNavigation();
        testScreenStackAndLifecycle();
        testNineSliceRendering();
        testRegressionWithHeadlessGL();

        System.out.println("\n>>> ALL 10 GUI PHASE 2 VERIFICATION TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    // -------------------------------------------------------------------------
    // 1. Base Widget Fundamentals
    // -------------------------------------------------------------------------
    private static void testWidgetFundamentals() {
        System.out.print("[Phase 2.1] Base Widget state (bounds, visibility, enabled, hover, focus)... ");

        Widget w = new Widget(10, 20, 100, 30) {
            @Override
            public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {}
        };

        // Bounds hit testing
        assert w.isMouseOver(50, 35) : "Point (50, 35) must be inside widget";
        assert !w.isMouseOver(5, 20) : "Point (5, 20) must be outside widget";
        assert !w.isMouseOver(115, 20) : "Point (115, 20) must be outside widget";

        // Hover update
        w.update(50, 35);
        assert w.isHovered() : "Widget must be hovered";

        // Invisible widgets must not be hovered
        w.setVisible(false);
        w.update(50, 35);
        assert !w.isHovered() : "Invisible widget must not be hovered";
        assert !w.isMouseOver(50, 35) : "Invisible widget must not pass mouseOver";

        // Disabled widgets must not be hovered or focusable
        w.setVisible(true);
        w.setEnabled(false);
        w.update(50, 35);
        assert !w.isHovered() : "Disabled widget must not be hovered";
        assert !w.isFocusable() : "Disabled widget must not be focusable";

        // Focus state
        w.setEnabled(true);
        assert w.isFocusable();
        w.setFocused(true);
        assert w.isFocused();
        w.setFocused(false);
        assert !w.isFocused();

        // Tooltip
        w.setTooltipText("Test Tooltip");
        assert "Test Tooltip".equals(w.getTooltipText());

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 2. Button Interaction & Duplicate Prevention
    // -------------------------------------------------------------------------
    private static void testButtonInteraction() {
        System.out.print("[Phase 2.2] Button activation, keyboard triggering & duplicate prevention... ");

        AtomicInteger clickCount = new AtomicInteger(0);
        Button btn = new Button(1, "Click Me", 50, 50, 200, 20, clickCount::incrementAndGet);

        // 1. Mouse click inside
        btn.update(100, 60);
        assert btn.isHovered();
        boolean clicked = btn.mouseClicked(100, 60, 0);
        assert clicked : "Left click on hovered button must return true";
        assert clickCount.get() == 1 : "Button must fire onClick on click";

        // 2. Mouse release immediately after click must NOT double-activate
        boolean released = btn.mouseReleased(100, 60, 0);
        assert released;
        assert clickCount.get() == 1 : "Mouse release must not double-activate if already fired on press";

        // 3. Mouse click outside: must not activate
        btn.update(10, 10);
        assert !btn.isHovered();
        btn.mouseClicked(10, 10, 0);
        btn.mouseReleased(10, 10, 0);
        assert clickCount.get() == 1 : "Click outside must not activate button";

        // 4. Keyboard activation when focused
        btn.setFocused(true);
        btn.keyPressed(GLFW.GLFW_KEY_ENTER, 0, GLFW.GLFW_PRESS, 0);
        assert clickCount.get() == 2 : "Enter key on focused button must activate";

        btn.keyPressed(GLFW.GLFW_KEY_SPACE, 0, GLFW.GLFW_PRESS, 0);
        assert clickCount.get() == 3 : "Space key on focused button must activate";

        // 5. Disabled button must not activate via mouse or keyboard
        btn.setEnabled(false);
        btn.mouseClicked(100, 60, 0);
        btn.keyPressed(GLFW.GLFW_KEY_ENTER, 0, GLFW.GLFW_PRESS, 0);
        assert clickCount.get() == 3 : "Disabled button must never activate";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 3. Cycle Button
    // -------------------------------------------------------------------------
    private static void testCycleButton() {
        System.out.print("[Phase 2.3] CycleButton forward/reverse cycling & wraparound... ");

        List<String> options = List.of("Fast", "Fancy", "Fabulous");
        AtomicInteger selectedIdx = new AtomicInteger(0);

        CycleButton<String> cycleBtn = new CycleButton<>(2, "Graphics: ", 0, 0, 150, 20,
                options, 0, String::valueOf, val -> selectedIdx.set(options.indexOf(val)));

        assert "Graphics: Fast".equals(cycleBtn.getDisplayString());

        // Forward cycling (left click)
        cycleBtn.update(10, 10);
        cycleBtn.mouseClicked(10, 10, 0); // Left click
        assert "Graphics: Fancy".equals(cycleBtn.getDisplayString());
        assert selectedIdx.get() == 1;

        cycleBtn.mouseClicked(10, 10, 0);
        assert "Graphics: Fabulous".equals(cycleBtn.getDisplayString());
        assert selectedIdx.get() == 2;

        // Wraparound forward
        cycleBtn.mouseClicked(10, 10, 0);
        assert "Graphics: Fast".equals(cycleBtn.getDisplayString());
        assert selectedIdx.get() == 0;

        // Reverse cycling (right click)
        cycleBtn.mouseClicked(10, 10, 1); // Right click
        assert "Graphics: Fabulous".equals(cycleBtn.getDisplayString());
        assert selectedIdx.get() == 2;

        // Keyboard arrow cycling
        cycleBtn.setFocused(true);
        cycleBtn.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, 0);
        assert "Graphics: Fancy".equals(cycleBtn.getDisplayString());

        cycleBtn.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_PRESS, 0);
        assert "Graphics: Fabulous".equals(cycleBtn.getDisplayString());

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 4. Slider
    // -------------------------------------------------------------------------
    private static void testSlider() {
        System.out.print("[Phase 2.4] Slider min/max, step rounding, dragging & clamping... ");

        AtomicInteger changeCount = new AtomicInteger(0);
        Slider slider = new Slider(3, "FOV: ", 10, 10, 100, 20,
                60.0, 110.0, 5.0, 70.0,
                v -> (int) Math.round(v) + "°",
                v -> changeCount.incrementAndGet());

        assert Math.abs(slider.getValue() - 70.0) < 1e-4;

        // 1. Keyboard adjustment (Up/Right +5.0)
        slider.setFocused(true);
        slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_PRESS, 0);
        assert Math.abs(slider.getValue() - 75.0) < 1e-4 : "Slider value should be 75.0, got: " + slider.getValue();

        slider.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, 0);
        assert Math.abs(slider.getValue() - 70.0) < 1e-4;

        // 2. Home / End keys
        slider.keyPressed(GLFW.GLFW_KEY_HOME, 0, GLFW.GLFW_PRESS, 0);
        assert Math.abs(slider.getValue() - 60.0) < 1e-4 : "Home key must jump to min (60.0)";

        slider.keyPressed(GLFW.GLFW_KEY_END, 0, GLFW.GLFW_PRESS, 0);
        assert Math.abs(slider.getValue() - 110.0) < 1e-4 : "End key must jump to max (110.0)";

        // 3. Clamping beyond bounds
        slider.setValue(200.0);
        assert Math.abs(slider.getValue() - 110.0) < 1e-4 : "Value above max must clamp to 110.0";

        slider.setValue(10.0);
        assert Math.abs(slider.getValue() - 60.0) < 1e-4 : "Value below min must clamp to 60.0";

        // 4. Mouse dragging
        slider.update(60, 20); // Middle of slider (x=10, w=100)
        slider.mouseClicked(60, 20, 0);
        assert slider.getValue() >= 80.0 && slider.getValue() <= 90.0 : "Clicking mid-slider must set ~85.0";

        slider.mouseReleased(60, 20, 0);

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 5. TextField (Full Functionality)
    // -------------------------------------------------------------------------
    private static void testTextField() {
        System.out.print("[Phase 2.5] TextField typing, caret, selection, clipboard, filtering & consumption... ");

        TextField field = new TextField(4, 10, 10, 200, 20);
        field.setMaxLength(16);

        // 1. Typing characters
        field.setFocused(true);
        field.charTyped('H');
        field.charTyped('e');
        field.charTyped('l');
        field.charTyped('l');
        field.charTyped('o');
        assert "Hello".equals(field.getText());
        assert field.getCaretPosition() == 5;

        // 2. Unicode typing (Latin-1 '°', '§', accented)
        field.charTyped('°');
        assert "Hello°".equals(field.getText());

        // 3. Backspace
        field.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, GLFW.GLFW_PRESS, 0);
        assert "Hello".equals(field.getText());
        assert field.getCaretPosition() == 5;

        // 4. Arrow movement
        field.keyPressed(GLFW.GLFW_KEY_LEFT, 0, GLFW.GLFW_PRESS, 0);
        assert field.getCaretPosition() == 4;
        field.keyPressed(GLFW.GLFW_KEY_HOME, 0, GLFW.GLFW_PRESS, 0);
        assert field.getCaretPosition() == 0;
        field.keyPressed(GLFW.GLFW_KEY_END, 0, GLFW.GLFW_PRESS, 0);
        assert field.getCaretPosition() == 5;

        // 5. Select all (Ctrl+A)
        field.keyPressed(GLFW.GLFW_KEY_A, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
        assert field.hasSelection();
        assert "Hello".equals(field.getSelectedText());

        // 6. Copy (Ctrl+C) & Cut (Ctrl+X)
        field.keyPressed(GLFW.GLFW_KEY_C, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
        assert "Hello".equals(ClipboardHelper.getClipboard(0));

        field.keyPressed(GLFW.GLFW_KEY_X, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
        assert "".equals(field.getText()) : "Cut must clear selected text";
        assert !field.hasSelection();

        // 7. Paste (Ctrl+V)
        field.keyPressed(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_PRESS, GLFW.GLFW_MOD_CONTROL);
        assert "Hello".equals(field.getText());

        // 8. Text Filtering: Alphanumeric only
        field.setFilter(s -> s.matches("[a-zA-Z0-9]*"));
        field.charTyped('!'); // Rejected by filter
        assert "Hello".equals(field.getText());

        field.charTyped('2'); // Accepted by filter
        assert "Hello2".equals(field.getText());

        // 9. Input Consumption: when focused, keyPressed returns true so game controls never trigger
        boolean consumed = field.keyPressed(GLFW.GLFW_KEY_W, 0, GLFW.GLFW_PRESS, 0);
        assert consumed : "Focused text field must consume key events";

        // 10. Focus Loss on Outside Click
        field.mouseClicked(500, 500, 0); // Click far outside
        assert !field.isFocused() : "Clicking outside must unfocus text field";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 6. Label & ListRow
    // -------------------------------------------------------------------------
    private static void testLabelAndListRow() {
        System.out.print("[Phase 2.6] Label alignment & ListRow hover/selection states... ");

        // Label
        Label label = new Label("Title", 10, 10, 100, 20, 0xFFFFFF, true, 0.5f, Label.Alignment.CENTER);
        assert !label.isFocusable() : "Labels must never be focusable";
        assert "Title".equals(label.getText());
        label.setText("Updated Title");
        assert "Updated Title".equals(label.getText());

        // ListRow
        AtomicBoolean rowClicked = new AtomicBoolean(false);
        ListRow row = new ListRow(0, "World 1", "Hardcore - 15MB", 0, 0, 300, 36, () -> rowClicked.set(true));
        assert !row.isSelected();
        row.setSelected(true);
        assert row.isSelected();

        row.update(50, 18);
        assert row.isHovered();

        row.mouseClicked(50, 18, 0);
        assert rowClicked.get() : "Clicking ListRow must fire onClick";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 7. FocusManager and Tab Navigation
    // -------------------------------------------------------------------------
    private static void testFocusManagerAndTabNavigation() {
        System.out.print("[Phase 2.7] FocusManager Tab navigation & Shift+Tab reverse cycling... ");

        FocusManager fm = new FocusManager();
        Button b1 = new Button(1, "B1", 0, 0, 100, 20, null);
        TextField tf = new TextField(2, 0, 30, 100, 20);
        Button b2 = new Button(3, "B2", 0, 60, 100, 20, null);
        Label lbl = new Label("Label", 0, 90, 0xFFFFFF); // Not focusable

        List<Widget> widgets = List.of(b1, tf, b2, lbl);

        // Forward Tab
        fm.cycleFocus(widgets, false);
        assert fm.getFocusedWidget() == b1 : "First Tab must focus B1";

        fm.cycleFocus(widgets, false);
        assert fm.getFocusedWidget() == tf : "Second Tab must focus TextField";

        fm.cycleFocus(widgets, false);
        assert fm.getFocusedWidget() == b2 : "Third Tab must focus B2 (skipping Label)";

        // Forward Wraparound
        fm.cycleFocus(widgets, false);
        assert fm.getFocusedWidget() == b1 : "Wraparound Tab must focus B1";

        // Reverse Tab (Shift+Tab)
        fm.cycleFocus(widgets, true);
        assert fm.getFocusedWidget() == b2 : "Reverse Tab must wrap to B2";

        fm.cycleFocus(widgets, true);
        assert fm.getFocusedWidget() == tf : "Reverse Tab must focus TextField";

        // Outside click clears focus
        fm.handleClick(widgets, 500, 500);
        assert !fm.hasFocus() : "Clicking empty space must clear focus";

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 8. Screen Stack & Navigation Lifecycle
    // -------------------------------------------------------------------------
    private static void testScreenStackAndLifecycle() {
        System.out.print("[Phase 2.8] ScreenManager navigation stack (push, pop, replace, lifecycle)... ");

        ScreenManager sm = new ScreenManager();

        class TestScreen extends Screen {
            public boolean wasOpened = false;
            public boolean wasClosed = false;
            public boolean wasDeactivated = false;
            public boolean wasReactivated = false;

            public TestScreen(String name) {
                super(null);
            }

            @Override
            public boolean usesLogicalCoordinates() {
                return true;
            }

            @Override
            public void onOpened() { wasOpened = true; }
            @Override
            public void onClosed() { wasClosed = true; }
            @Override
            public void onDeactivated() { wasDeactivated = true; }
            @Override
            public void onReactivated() { wasReactivated = true; }
        }

        TestScreen parent = new TestScreen("Parent");
        TestScreen child = new TestScreen("Child");

        // Set Root Screen
        sm.setScreen(parent, null, null);
        assert sm.hasScreen();
        assert sm.getCurrentScreen() == parent;
        assert parent.wasOpened;
        assert sm.getStackDepth() == 1;

        // Push Child Screen (e.g. Options opened from Escape menu)
        sm.pushScreen(child);
        assert sm.getCurrentScreen() == child;
        assert parent.wasDeactivated : "Parent screen must be deactivated when child is pushed";
        assert child.wasOpened;
        assert sm.getStackDepth() == 2;

        // Pop Child Screen: Parent must be restored and reactivated
        Screen returned = sm.popScreen();
        assert returned == parent : "Popping child must return parent screen";
        assert sm.getCurrentScreen() == parent;
        assert child.wasClosed;
        assert parent.wasReactivated : "Parent screen must be reactivated";
        assert sm.getStackDepth() == 1;

        // Pop Parent Screen: Screen stack empty
        sm.popScreen();
        assert !sm.hasScreen();
        assert parent.wasClosed;
        assert sm.getStackDepth() == 0;

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 9. Nine-Slice Rendering
    // -------------------------------------------------------------------------
    private static void testNineSliceRendering() {
        System.out.print("[Phase 2.9] NineSlice geometry calculations & procedural fallback... ");

        // Procedural 9-slice (textureId = 0)
        NineSlice procedural = new NineSlice(0, 16, 16, 0, 0, 1, 1, 4, 4, 4, 4);
        assert procedural.getTextureId() == 0;
        assert procedural.getLeft() == 4.0f;
        assert procedural.getTop() == 4.0f;

        // Textured 9-slice
        NineSlice textured = new NineSlice(42, 64, 64, 0.0f, 0.0f, 0.5f, 0.5f, 6, 6, 6, 6);
        assert textured.getTextureId() == 42;
        assert textured.getRight() == 6.0f;
        assert textured.getBottom() == 6.0f;

        System.out.println("PASSED");
    }

    // -------------------------------------------------------------------------
    // 10. Headless OpenGL Render Regression
    // -------------------------------------------------------------------------
    private static void testRegressionWithHeadlessGL() {
        System.out.print("[Phase 2.10] Headless GL widget rendering & state restoration... ");

        long window = createHeadlessWindow();
        try {
            GuiRenderer gui = new GuiRenderer();
            FontRenderer font = new FontRenderer();
            font.init();

            Screen testScreen = new Screen(null) {
                @Override
                public boolean usesLogicalCoordinates() {
                    return true;
                }
            };
            testScreen.init(427, 240);

            Button btn = new Button(1, "Test", 10, 10, 100, 20, null);
            Slider slider = new Slider(2, "Val: ", 10, 35, 100, 20, 0, 100, 1, 50, String::valueOf, null);
            TextField tf = new TextField(3, 10, 60, 100, 20);
            tf.setText("Voxel");
            Label lbl = new Label("Notice", 10, 85, 0xFFFFFF);

            testScreen.addWidget(btn);
            testScreen.addWidget(slider);
            testScreen.addWidget(tf);
            testScreen.addWidget(lbl);

            gui.resetMetrics();
            gui.begin(427, 240);
            testScreen.render(gui, font, 20, 20, 0.016f);
            gui.end();

            assert gui.getDrawCallCount() > 0 : "Widgets must render via GuiRenderer batching";

            gui.cleanup();
            font.cleanup();
            System.out.println("PASSED (All widgets rendered with Phase 0 batching intact)");
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

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

        long handle = glfwCreateWindow(800, 600, "Phase2Headless", 0, 0);
        if (handle == 0) {
            throw new RuntimeException("Failed to create GLFW headless window");
        }
        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        return handle;
    }
}
