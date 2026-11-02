package com.mcjournal.client.gui;

/**
 * Reusable, lightweight, allocation-free layout helpers for positioning GUI widgets
 * and screen elements within logical coordinates.
 */
public final class GuiLayout {
    private GuiLayout() {}

    // Standard GUI layout metrics matching Minecraft interface specifications
    public static final int ROW_HEIGHT = 20;
    public static final int GUTTER = 4;
    public static final int MARGIN = 12;

    /**
     * Immutable content column geometry holding X coordinate and width.
     */
    public static class Column {
        public final int x;
        public final int width;

        public Column(int x, int width) {
            this.x = x;
            this.width = width;
        }

        public int getRight() {
            return x + width;
        }
    }

    /**
     * Computes a centered content column constrained by maxWidth and side margins.
     */
    public static Column contentColumn(int screenWidth, int maxWidth, int margin) {
        int w = Math.min(maxWidth, screenWidth - margin * 2);
        int x = (screenWidth - w) / 2;
        return new Column(x, w);
    }

    /**
     * Calculates the width of each button in an equal-width row sharing a uniform gutter.
     */
    public static int buttonWidth(int totalWidth, int count, int gutter) {
        if (count <= 0) return 0;
        return (totalWidth - (count - 1) * gutter) / count;
    }

    /**
     * Calculates the X coordinate of an item at index in a row with uniform gutter.
     */
    public static int buttonX(int startX, int index, int buttonWidth, int gutter) {
        return startX + index * (buttonWidth + gutter);
    }

    /**
     * Calculates the centered X coordinate for an element of width elementWidth
     * inside a container of width containerWidth.
     */
    public static int centerX(int containerWidth, int elementWidth) {
        return (containerWidth - elementWidth) / 2;
    }

    /**
     * Calculates the centered Y coordinate for an element of height elementHeight
     * inside a container of height containerHeight.
     */
    public static int centerY(int containerHeight, int elementHeight) {
        return (containerHeight - elementHeight) / 2;
    }

    /**
     * Anchors an element to the left edge with a given margin.
     */
    public static int anchorLeft(int margin) {
        return margin;
    }

    /**
     * Anchors an element to the right edge with a given margin.
     */
    public static int anchorRight(int containerWidth, int elementWidth, int margin) {
        return containerWidth - elementWidth - margin;
    }

    /**
     * Anchors an element to the top edge with a given margin.
     */
    public static int anchorTop(int margin) {
        return margin;
    }

    /**
     * Anchors an element to the bottom edge with a given margin.
     */
    public static int anchorBottom(int containerHeight, int elementHeight, int margin) {
        return containerHeight - elementHeight - margin;
    }

    /**
     * Calculates screen-relative horizontal position based on a fractional factor (0.0 to 1.0).
     */
    public static int relativeX(int containerWidth, float fraction, int elementWidth) {
        return (int) (containerWidth * fraction) - elementWidth / 2;
    }

    /**
     * Calculates screen-relative vertical position based on a fractional factor (0.0 to 1.0).
     */
    public static int relativeY(int containerHeight, float fraction, int elementHeight) {
        return (int) (containerHeight * fraction) - elementHeight / 2;
    }

    /**
     * Total width of a horizontal row of count items of itemWidth separated by gap.
     */
    public static int rowTotalWidth(int count, int itemWidth, int gap) {
        if (count <= 0) return 0;
        return count * itemWidth + (count - 1) * gap;
    }

    /**
     * Total height of a vertical column of count items of itemHeight separated by gap.
     */
    public static int columnTotalHeight(int count, int itemHeight, int gap) {
        if (count <= 0) return 0;
        return count * itemHeight + (count - 1) * gap;
    }

    /**
     * Calculates the X coordinate of an item at index in a horizontal row.
     */
    public static int rowItemX(int startX, int itemWidth, int gap, int index) {
        return startX + index * (itemWidth + gap);
    }

    /**
     * Calculates the Y coordinate of an item at index in a vertical column.
     */
    public static int columnItemY(int startY, int itemHeight, int gap, int index) {
        return startY + index * (itemHeight + gap);
    }

    /**
     * Calculates the start X coordinate for a horizontal row of items centered in a container.
     */
    public static int centeredRowStartX(int containerWidth, int count, int itemWidth, int gap) {
        int totalWidth = rowTotalWidth(count, itemWidth, gap);
        return centerX(containerWidth, totalWidth);
    }

    /**
     * Calculates the start Y coordinate for a vertical column of items centered in a container.
     */
    public static int centeredColumnStartY(int containerHeight, int count, int itemHeight, int gap) {
        int totalHeight = columnTotalHeight(count, itemHeight, gap);
        return centerY(containerHeight, totalHeight);
    }

    /**
     * Splits a given total row width into equal button widths across columns separated by gap.
     */
    public static int splitWidth(int totalWidth, int gap, int columns) {
        if (columns <= 1) return totalWidth;
        return (totalWidth - (columns - 1) * gap) / columns;
    }
}
