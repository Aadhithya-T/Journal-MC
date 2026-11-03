package com.mcjournal.client.gui;

/**
 * Reusable, lightweight layout helpers for positioning GUI widgets
 * and screen elements within logical coordinates.
 */
public final class GuiLayout {
    private GuiLayout() {}

    // Standard GUI layout metrics matching Minecraft interface specifications
    public static final int ROW_HEIGHT = 20;
    public static final int GUTTER = 4;
    public static final int MARGIN = 12;
    public static final int HEADER_HEIGHT = 32;
    public static final int TITLE_Y = 8;
    public static final int BOTTOM_MARGIN = 28;

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
     * An individual slot in a distributed button row.
     */
    public record ButtonSlot(int x, int width) {
        public int right() {
            return x + width;
        }
    }

    /**
     * Distributes totalWidth across count buttons separated by gutter,
     * spreading any integer division remainder across the slots so that the row
     * exactly spans from startX to startX + totalWidth without rounding drift or manual adjustment.
     */
    public static ButtonSlot[] distributeRow(int startX, int totalWidth, int count, int gutter) {
        if (count <= 0) return new ButtonSlot[0];
        ButtonSlot[] slots = new ButtonSlot[count];
        int availableWidth = totalWidth - (count - 1) * gutter;
        int baseWidth = availableWidth / count;
        int remainder = availableWidth % count;

        int currentX = startX;
        for (int i = 0; i < count; i++) {
            int slotWidth = baseWidth + (i < remainder ? 1 : 0);
            slots[i] = new ButtonSlot(currentX, slotWidth);
            currentX += slotWidth + gutter;
        }
        return slots;
    }

    /**
     * Calculates the width of each button in an equal-width row sharing a uniform gutter.
     */
    public static int buttonWidth(int totalWidth, int count, int gutter) {
        if (count <= 0) return 0;
        return (totalWidth - (count - 1) * gutter) / count;
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
     * Total width of a horizontal row of count items of itemWidth separated by gap.
     */
    public static int rowTotalWidth(int count, int itemWidth, int gap) {
        if (count <= 0) return 0;
        return count * itemWidth + (count - 1) * gap;
    }

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
}
