package com.mcjournal.client.gui;

/**
 * Shared HUD design constants establishing a consistent, distinctive
 * Journal-MC visual identity: dark stone/charcoal foundations, refined
 * golden-amber accents, crisp pixel-art icon metrics, and clear typographic hierarchy.
 */
public final class HudTheme {
    private HudTheme() {}

    // Charcoal / Dark Stone Foundation Surfaces
    public static final int PANEL_BG = 0x121216;
    public static final float PANEL_ALPHA = 0.82f;
    public static final int PANEL_BORDER_LIGHT = 0x383844;
    public static final int PANEL_BORDER_DARK = 0x0a0a0e;
    public static final int PANEL_SLATE_FILL = 0x18181e;

    // Journal-MC Signature Gold & Amber Accents
    public static final int GOLD_ACCENT = 0xd4af37;
    public static final int GOLD_HIGHLIGHT = 0xffc83b;
    public static final int GOLD_MUTED = 0x8a7024;
    public static final float GOLD_R = 1.0f;
    public static final float GOLD_G = 0.78f;
    public static final float GOLD_B = 0.23f;

    // Health Gem / Shield Colors
    public static final int RUBY_RED = 0xd81e28;
    public static final int RUBY_DARK = 0x8a1018;
    public static final int RUBY_SHINE = 0xff6677;
    public static final int RUBY_HURT_FLASH = 0xfff2ea;
    public static final int HEALTH_EMPTY_BG = 0x24242c;

    // Hunger Loaf / Amber Roast Colors
    public static final int HUNGER_AMBER = 0xdd8218;
    public static final int HUNGER_DARK = 0x824608;
    public static final int HUNGER_SHINE = 0xffba46;
    public static final int HUNGER_EMPTY_BG = 0x28221c;

    // Hotbar Dimensions & Layout
    public static final int SLOT_SIZE = 22;
    public static final int SLOT_COUNT = 9;
    public static final int HOTBAR_PADDING = 2;
    public static final int HOTBAR_WIDTH = SLOT_COUNT * SLOT_SIZE + (HOTBAR_PADDING * 2); // 202px
    public static final int HOTBAR_HEIGHT = SLOT_SIZE + (HOTBAR_PADDING * 2);             // 26px

    // Slot Styling
    public static final int SLOT_BG = 0x161620;
    public static final float SLOT_BG_ALPHA = 0.70f;
    public static final int SLOT_BORDER_OUTER = 0x3c3c4a;
    public static final int SLOT_BORDER_INNER = 0x0c0c12;

    // Icon Metrics
    public static final int ICON_SIZE = 9;
    public static final int ICON_GAP = 1;
    public static final int STATUS_BAR_WIDTH = (10 * ICON_SIZE) + (9 * ICON_GAP); // 99px

    // Typography & Information
    public static final int TEXT_PRIMARY = 0xffffff;
    public static final int TEXT_SECONDARY = 0xd4d4dc;
    public static final int TEXT_MUTED = 0x9494a0;
    public static final int TEXT_GOLD = 0xffc83b;
}

