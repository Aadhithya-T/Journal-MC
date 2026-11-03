package com.mcjournal.client.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic tool to verify that all screen widgets and text elements remain strictly
 * within screen bounds (x >= 0, y >= 0, x + w <= screenW, y + h <= screenH) and
 * do not unintentionally intersect with neighboring sibling elements.
 */
public class GuiDiagnostics {

    public static boolean ENABLED = false;

    public record ElementBounds(String id, String label, int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }

        public boolean intersects(ElementBounds other) {
            return this.x < other.right() && this.right() > other.x
                    && this.y < other.bottom() && this.bottom() > other.y;
        }
    }

    public static class Violation {
        public final String screenName;
        public final String type; // "OUT_OF_BOUNDS" or "OVERLAP"
        public final String message;
        public final ElementBounds elemA;
        public final ElementBounds elemB;

        public Violation(String screenName, String type, String message, ElementBounds elemA, ElementBounds elemB) {
            this.screenName = screenName;
            this.type = type;
            this.message = message;
            this.elemA = elemA;
            this.elemB = elemB;
        }

        @Override
        public String toString() {
            return "[" + screenName + "] " + type + ": " + message;
        }
    }

    /**
     * Inspects a screen for element bounds containment and mutual overlap violations.
     *
     * @param screen Screen instance to inspect
     * @param font   FontRenderer for estimating text dimensions
     * @return List of detected layout violations (empty if layout is clean)
     */
    public static List<Violation> checkScreen(Screen screen, FontRenderer font) {
        List<Violation> violations = new ArrayList<>();
        if (screen == null) return violations;

        String screenName = screen.getClass().getSimpleName();
        int screenW = screen.getWidth();
        int screenH = screen.getHeight();

        List<ElementBounds> elements = new ArrayList<>();

        // 1. Collect all registered widgets
        int widgetIdx = 0;
        for (Widget w : screen.getWidgets()) {
            if (!w.isVisible()) continue;
            widgetIdx++;
            String label = (w instanceof Button btn) ? btn.getText() :
                    (w instanceof TextField tf) ? ("Field:" + tf.getPlaceholder()) :
                    (w instanceof Label lbl) ? ("Label:" + lbl.getText()) :
                    w.getClass().getSimpleName();
            elements.add(new ElementBounds("widget-" + widgetIdx, label, Math.round(w.getX()), Math.round(w.getY()), Math.round(w.getWidth()), Math.round(w.getHeight())));
        }

        // 2. Specific screen custom elements (headers, cards, labels)
        if (screen instanceof WorldSelectScreen wss) {
            // Screen Title
            int titleH = font != null ? Math.round(font.getLineHeight(TextStyle.TITLE)) : 20;
            float titleW = font != null ? font.width("Select World", TextStyle.TITLE) : 140;
            int titleX = Math.round((screenW - titleW) / 2.0f);
            elements.add(new ElementBounds("header-title", "Select World", titleX, 8, Math.round(titleW), titleH));

            // World Selection Card
            elements.add(new ElementBounds("world-card", "World Card", wss.getCardX(), wss.getCardY(), wss.getCardW(), wss.getCardH()));
        } else if (screen instanceof WorldCreateScreen wcs) {
            // Screen Title
            int titleH = font != null ? Math.round(font.getLineHeight(TextStyle.TITLE)) : 20;
            float titleW = font != null ? font.width("Create New World", TextStyle.TITLE) : 160;
            int titleX = Math.round((screenW - titleW) / 2.0f);
            elements.add(new ElementBounds("header-title", "Create New World", titleX, 8, Math.round(titleW), titleH));

            // Form Labels
            int labelH = font != null ? Math.round(font.getLineHeight(TextStyle.BODY)) : 10;
            elements.add(new ElementBounds("lbl-name", "World Name", wcs.getFormX(), wcs.getNameLabelY(), 100, labelH));
            elements.add(new ElementBounds("lbl-seed", "World Seed", wcs.getFormX(), wcs.getSeedLabelY(), 100, labelH));

            // Two-Line Option Cards
            elements.add(new ElementBounds("card-mode", "Game Mode Card", wcs.getFormX(), wcs.getCard1Y(), wcs.getFormW(), wcs.getCardH()));
            elements.add(new ElementBounds("card-type", "World Type Card", wcs.getFormX(), wcs.getCard2Y(), wcs.getFormW(), wcs.getCardH()));
        } else if (screen instanceof WorldEditScreen wes) {
            int titleH = font != null ? Math.round(font.getLineHeight(TextStyle.TITLE)) : 20;
            float titleW = font != null ? font.width("Edit World", TextStyle.TITLE) : 100;
            int titleX = Math.round((screenW - titleW) / 2.0f);
            elements.add(new ElementBounds("header-title", "Edit World", titleX, GuiLayout.TITLE_Y, Math.round(titleW), titleH));

            int labelH = font != null ? Math.round(font.getLineHeight(TextStyle.BODY)) : 10;
            elements.add(new ElementBounds("lbl-name", "World Name", wes.getFormX(), wes.getNameLabelY(), 100, labelH));

            elements.add(new ElementBounds("world-card", "World Info Card", wes.getFormX(), wes.getCardY(), wes.getFormW(), wes.getCardH()));
        }

        // 3. Containment check: x >= 0, y >= 0, x + w <= screenW, y + h <= screenH
        for (ElementBounds elem : elements) {
            if (elem.x < 0 || elem.y < 0 || elem.right() > screenW || elem.bottom() > screenH) {
                violations.add(new Violation(
                        screenName,
                        "OUT_OF_BOUNDS",
                        "Element " + elem.id + " ('" + elem.label + "') [" + elem.x + "," + elem.y + " " + elem.width + "x" + elem.height + "] exceeds screen bounds [" + screenW + "x" + screenH + "]",
                        elem,
                        null
                ));
            }
        }

        // 4. Pairwise intersection check
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                ElementBounds a = elements.get(i);
                ElementBounds b = elements.get(j);
                if (a.intersects(b)) {
                    violations.add(new Violation(
                            screenName,
                            "OVERLAP",
                            "Element " + a.id + " ('" + a.label + "') [" + a.x + "," + a.y + " " + a.width + "x" + a.height + "] overlaps "
                                    + b.id + " ('" + b.label + "') [" + b.x + "," + b.y + " " + b.width + "x" + b.height + "]",
                            a,
                            b
                    ));
                }
            }
        }

        if (ENABLED && !violations.isEmpty()) {
            for (Violation v : violations) {
                System.err.println("[GuiDiagnostics] " + v);
            }
        }

        return violations;
    }
}
