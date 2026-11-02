package com.mcjournal.client.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages keyboard and mouse focus across GUI widgets.
 * Enforces single-focus ownership, predictable focus cycling (Tab / Shift+Tab),
 * and focus clearing on outside clicks or widget removal.
 */
public class FocusManager {
    private Widget focusedWidget = null;

    public Widget getFocusedWidget() {
        return focusedWidget;
    }

    public boolean hasFocus() {
        return focusedWidget != null;
    }

    public void setFocus(Widget widget) {
        if (focusedWidget == widget) return;

        if (focusedWidget != null) {
            focusedWidget.onFocusLost();
        }

        if (widget != null && widget.isFocusable()) {
            focusedWidget = widget;
            focusedWidget.onFocusGained();
        } else {
            focusedWidget = null;
        }
    }

    public void clearFocus() {
        setFocus(null);
    }

    /**
     * Cycles focus forward (Tab) or backward (Shift+Tab) among visible, enabled, focusable widgets.
     */
    public boolean cycleFocus(List<Widget> widgets, boolean reverse) {
        if (widgets == null || widgets.isEmpty()) {
            clearFocus();
            return false;
        }

        List<Widget> eligible = new ArrayList<>();
        for (Widget w : widgets) {
            if (w.isFocusable()) {
                eligible.add(w);
            }
        }

        if (eligible.isEmpty()) {
            clearFocus();
            return false;
        }

        int currentIndex = eligible.indexOf(focusedWidget);
        int nextIndex;

        if (currentIndex == -1) {
            nextIndex = reverse ? eligible.size() - 1 : 0;
        } else {
            nextIndex = reverse ? (currentIndex - 1 + eligible.size()) % eligible.size()
                                : (currentIndex + 1) % eligible.size();
        }

        setFocus(eligible.get(nextIndex));
        return true;
    }

    /**
     * Updates focus based on mouse click position.
     * Focuses the clicked focusable widget, or clears focus if clicking empty area.
     */
    public void handleClick(List<Widget> widgets, double mouseX, double mouseY) {
        if (widgets == null) {
            clearFocus();
            return;
        }

        Widget clicked = null;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            Widget w = widgets.get(i);
            if (w.isMouseOver(mouseX, mouseY)) {
                clicked = w;
                break;
            }
        }

        if (clicked != null && clicked.isFocusable()) {
            setFocus(clicked);
        } else if (clicked == null) {
            clearFocus();
        }
    }

    /**
     * Ensures focus is cleared if the currently focused widget is removed or disabled.
     */
    public void onWidgetRemoved(Widget widget) {
        if (focusedWidget == widget) {
            clearFocus();
        }
    }
}
