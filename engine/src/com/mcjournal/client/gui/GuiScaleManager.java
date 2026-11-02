package com.mcjournal.client.gui;

/**
 * Centralized manager for GUI virtual resolution, integer scaling, viewport calculations,
 * and bidirectional coordinate transformations.
 *
 * Implements strict integer scaling policies with predictable fallback clamping
 * to prevent interface clipping at high manual scales or small window sizes.
 */
public class GuiScaleManager {
    public static final int SCALE_AUTO = 0;
    public static final int MIN_SCALE = 1;
    public static final int MAX_MANUAL_SCALE = 4;

    // Minimum design resolution in logical units
    public static final int MIN_DESIGN_WIDTH = 420;
    public static final int MIN_DESIGN_HEIGHT = 240;

    private int requestedScale = SCALE_AUTO;
    private int effectiveScale = 1;
    private boolean isScaleClamped = false;

    // Framebuffer dimensions (physical pixels)
    private int framebufferWidth = 854;
    private int framebufferHeight = 480;

    // Window dimensions (GLFW screen coordinate units / points)
    private int windowWidth = 854;
    private int windowHeight = 480;

    // Logical GUI dimensions (coordinates used by screens and widgets)
    private int guiWidth = 427;
    private int guiHeight = 240;

    // Active GUI viewport in framebuffer pixels
    private int viewportX = 0;
    private int viewportY = 0;
    private int viewportWidth = 854;
    private int viewportHeight = 480;

    public GuiScaleManager() {
        recalculate();
    }

    /**
     * Updates physical framebuffer and window dimensions and recalculates scaling.
     *
     * @param fbWidth      Physical framebuffer width in pixels
     * @param fbHeight     Physical framebuffer height in pixels
     * @param winWidth     GLFW window width in screen coordinates
     * @param winHeight    GLFW window height in screen coordinates
     */
    public void update(int fbWidth, int fbHeight, int winWidth, int winHeight) {
        this.framebufferWidth = Math.max(1, fbWidth);
        this.framebufferHeight = Math.max(1, fbHeight);
        this.windowWidth = Math.max(1, winWidth);
        this.windowHeight = Math.max(1, winHeight);
        recalculate();
    }

    /**
     * Sets the requested scale (0 = Auto, 1..4 = Manual).
     */
    public void setRequestedScale(int scale) {
        if (scale < 0 || scale > MAX_MANUAL_SCALE) {
            scale = SCALE_AUTO;
        }
        this.requestedScale = scale;
        recalculate();
    }

    /**
     * Recalculates effective scale, logical dimensions, and viewport offsets.
     */
    public void recalculate() {
        // Calculate the maximum integer scale that fits the minimum logical design resolution
        int maxFittingScale = Math.max(1, Math.min(
                framebufferWidth / MIN_DESIGN_WIDTH,
                framebufferHeight / MIN_DESIGN_HEIGHT
        ));

        if (requestedScale == SCALE_AUTO) {
            effectiveScale = maxFittingScale;
            isScaleClamped = false;
        } else {
            if (requestedScale <= maxFittingScale) {
                effectiveScale = requestedScale;
                isScaleClamped = false;
            } else {
                // Predictable fallback: clamp to maxFittingScale to prevent interface clipping
                effectiveScale = maxFittingScale;
                isScaleClamped = true;
            }
        }

        // Logical GUI dimensions
        guiWidth = Math.max(1, framebufferWidth / effectiveScale);
        guiHeight = Math.max(1, framebufferHeight / effectiveScale);

        // Viewport dimensions in physical pixels
        viewportWidth = guiWidth * effectiveScale;
        viewportHeight = guiHeight * effectiveScale;

        // Center remainder pixels within framebuffer (letterbox/pillarbox offset)
        viewportX = (framebufferWidth - viewportWidth) / 2;
        viewportY = (framebufferHeight - viewportHeight) / 2;
    }

    // --- Coordinate Transformations ---

    /**
     * Converts a GLFW window X coordinate into logical GUI coordinates.
     */
    public double windowToGuiX(double winX) {
        double fbX = (windowWidth > 0) ? (winX * (double) framebufferWidth / (double) windowWidth) : winX;
        double relX = fbX - viewportX;
        return relX / (double) effectiveScale;
    }

    /**
     * Converts a GLFW window Y coordinate into logical GUI coordinates.
     */
    public double windowToGuiY(double winY) {
        double fbY = (windowHeight > 0) ? (winY * (double) framebufferHeight / (double) windowHeight) : winY;
        double relY = fbY - viewportY;
        return relY / (double) effectiveScale;
    }

    /**
     * Returns true if the window coordinate lies inside the active GUI viewport.
     * Mouse clicks outside this region must be ignored to prevent activating elements in letterboxed margins.
     */
    public boolean isMouseInViewport(double winX, double winY) {
        double fbX = (windowWidth > 0) ? (winX * (double) framebufferWidth / (double) windowWidth) : winX;
        double fbY = (windowHeight > 0) ? (winY * (double) framebufferHeight / (double) windowHeight) : winY;
        return fbX >= viewportX && fbX < viewportX + viewportWidth
                && fbY >= viewportY && fbY < viewportY + viewportHeight;
    }

    /**
     * Converts a logical GUI X coordinate back to GLFW window coordinates.
     */
    public double guiToWindowX(double guiX) {
        double fbX = viewportX + (guiX * effectiveScale);
        return (framebufferWidth > 0) ? (fbX * (double) windowWidth / (double) framebufferWidth) : fbX;
    }

    /**
     * Converts a logical GUI Y coordinate back to GLFW window coordinates.
     */
    public double guiToWindowY(double guiY) {
        double fbY = viewportY + (guiY * effectiveScale);
        return (framebufferHeight > 0) ? (fbY * (double) windowHeight / (double) framebufferHeight) : fbY;
    }

    // --- Getters ---

    public int getRequestedScale() {
        return requestedScale;
    }

    public int getEffectiveScale() {
        return effectiveScale;
    }

    public boolean isScaleClamped() {
        return isScaleClamped;
    }

    public int getGuiWidth() {
        return guiWidth;
    }

    public int getGuiHeight() {
        return guiHeight;
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getViewportX() {
        return viewportX;
    }

    public int getViewportY() {
        return viewportY;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }
}
