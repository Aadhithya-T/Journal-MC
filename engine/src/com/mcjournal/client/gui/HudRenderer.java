package com.mcjournal.client.gui;

import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Player;
import com.mcjournal.client.TextureAtlas;

/**
 * Dedicated HUD rendering system for Journal-MC.
 * Renders in virtual logical coordinates (respecting GuiScaleManager)
 * and batches all draw calls through GuiRenderer.
 *
 * Implements:
 * - 9-slot exploration hotbar with dark stone foundation, recessed slots, and luminous gold selected frame
 * - Structured World Information Panel with frosted slate card styling and string caching
 * - Centered crosshair with target dot and connected contextual target block tooltip
 * - Active held-item rendering and count badges
 */
public class HudRenderer {
    private final HudContext context = new HudContext();

    // Cached string fields to avoid per-frame string allocations
    private int cachedFps = -1;
    private String cachedFpsStr = "";
    private int cachedRenderedChunks = -1;
    private int cachedLoadedChunks = -1;
    private String cachedChunkStr = "";

    private int cachedCoordX = Integer.MIN_VALUE;
    private int cachedCoordY = Integer.MIN_VALUE;
    private int cachedCoordZ = Integer.MIN_VALUE;
    private String cachedCoordStr = "";
    private String cachedElevationStr = "";

    private String cachedBiome = "";
    private String cachedBiomeStr = "";

    private long cachedDay = -1;
    private boolean cachedIsDay = true;
    private String cachedSolarTimeStr = "";

    // Ephemeral in-game notifications (e.g. Screenshot capture toast)
    private String notificationText = null;
    private float notificationTimer = 0.0f;

    public void setNotification(String text) {
        this.notificationText = text;
        this.notificationTimer = 3.0f;
    }

    public void update(float deltaTime) {
        if (notificationTimer > 0.0f) {
            notificationTimer -= deltaTime;
            if (notificationTimer <= 0.0f) {
                notificationText = null;
            }
        }
    }

    public HudRenderer() {}

    public HudContext getContext() {
        return context;
    }

    public void init() {
        // Idempotent initialization
        resetStringCaches();
    }

    public void resetStringCaches() {
        cachedFps = -1;
        cachedRenderedChunks = -1;
        cachedLoadedChunks = -1;
        cachedCoordX = Integer.MIN_VALUE;
        cachedBiome = "";
        cachedDay = -1;
    }

    /**
     * Main HUD rendering entry point.
     * Expects GuiRenderer to be active (within begin/end).
     */
    public void render(GuiRenderer gui, FontRenderer font, HudContext ctx) {
        if (ctx == null || ctx.settings == null || !ctx.settings.showHud) {
            return;
        }

        int width = ctx.screenWidth;
        int height = ctx.screenHeight;

        // 1. Crosshair & Target Feedback (Center)
        if (ctx.settings.showCrosshair) {
            renderCrosshair(gui, width, height, ctx.hasTarget());
        }

        // 2. Hotbar (Bottom Center)
        if (ctx.hasPlayer()) {
            renderHotbar(gui, font, ctx, width, height);
        }

        // 3. World Information Panel (Top Left)
        if (ctx.settings.showCoordinates || ctx.settings.showFps) {
            renderWorldInfo(gui, font, ctx, width, height);
        }

        // 4. Ephemeral In-Game Notifications (e.g. Screenshot capture toast)
        if (notificationText != null && notificationTimer > 0.0f) {
            renderNotification(gui, font, width, height);
        }
    }

    // -------------------------------------------------------------------------
    // A. Crosshair & Visual Target Connection
    // -------------------------------------------------------------------------
    private void renderCrosshair(GuiRenderer gui, int width, int height, boolean hasTarget) {
        int cx = width / 2;
        int cy = height / 2;

        // Inverted blend crosshair arms (Classic vanilla behavior: inverts cleanly against any backdrop)
        gui.setInvertedBlend(true);
        gui.drawRect(cx - 5, cy, 3, 1, 1.0f, 1.0f, 1.0f, 1.0f); // Left arm
        gui.drawRect(cx + 3, cy, 3, 1, 1.0f, 1.0f, 1.0f, 1.0f); // Right arm
        gui.drawRect(cx, cy - 5, 1, 3, 1.0f, 1.0f, 1.0f, 1.0f); // Top arm
        gui.drawRect(cx, cy + 3, 1, 3, 1.0f, 1.0f, 1.0f, 1.0f); // Bottom arm
        if (!hasTarget) {
            gui.drawRect(cx, cy, 1, 1, 1.0f, 1.0f, 1.0f, 1.0f); // Center pip
        }
        gui.setInvertedBlend(false);

        // Center Target Pip when targeting an interactable block (Vibrant gold highlight)
        if (hasTarget) {
            gui.drawRect(cx, cy, 1, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // B. Hotbar (9 Slots with Light Grey Stone Foundation & Gold Selection)
    // -------------------------------------------------------------------------
    private void renderHotbar(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        int slotCount = HudTheme.SLOT_COUNT;
        int slotSize = HudTheme.SLOT_SIZE;
        int totalHotbarW = HudTheme.HOTBAR_WIDTH; // 202px
        int totalHotbarH = HudTheme.HOTBAR_HEIGHT; // 26px
        int hx = GuiLayout.centerX(width, totalHotbarW);
        int hy = height - totalHotbarH - 3;

        // 1. Light Grey Stone / Iron Foundation Panel with 3D outer bevels
        gui.drawRect(hx, hy, totalHotbarW, totalHotbarH, 0.54f, 0.54f, 0.58f, 0.88f); // Light grey base fill
        gui.drawRect(hx, hy, totalHotbarW, 1, 0.84f, 0.84f, 0.88f, 0.95f);             // Top highlight border
        gui.drawRect(hx, hy, 1, totalHotbarH, 0.84f, 0.84f, 0.88f, 0.95f);             // Left highlight border
        gui.drawRect(hx, hy + totalHotbarH - 1, totalHotbarW, 1, 0.22f, 0.22f, 0.25f, 0.95f); // Bottom shadow border
        gui.drawRect(hx + totalHotbarW - 1, hy, 1, totalHotbarH, 0.22f, 0.22f, 0.25f, 0.95f); // Right shadow border

        int selected = Math.clamp(player.selectedSlot, 0, 8);

        for (int i = 0; i < slotCount; i++) {
            int sx = hx + HudTheme.HOTBAR_PADDING + i * slotSize;
            int sy = hy + HudTheme.HOTBAR_PADDING;

            // Recessed Slot Cavity with 3D inset bevels:
            gui.drawRect(sx, sy, slotSize, slotSize, 0.26f, 0.26f, 0.30f, 0.90f); // Slot outer divider border
            gui.drawRect(sx + 1, sy + 1, slotSize - 2, slotSize - 2, 0.36f, 0.36f, 0.40f, 0.82f); // Mid-grey cavity well
            gui.drawRect(sx + 1, sy + 1, slotSize - 2, 1, 0.14f, 0.14f, 0.18f, 0.90f); // Top inner drop shadow
            gui.drawRect(sx + 1, sy + 1, 1, slotSize - 2, 0.14f, 0.14f, 0.18f, 0.90f); // Left inner drop shadow
            gui.drawRect(sx + 1, sy + slotSize - 2, slotSize - 2, 1, 0.50f, 0.50f, 0.55f, 0.75f); // Bottom inner highlight rim
            gui.drawRect(sx + slotSize - 2, sy + 1, 1, slotSize - 2, 0.50f, 0.50f, 0.55f, 0.75f); // Right inner highlight rim

            // Slot Item / Block Content
            byte itemId = player.hotbarBlocks[i];
            int count = player.hotbarCounts[i];

            if (itemId != 0 && count > 0) {
                renderHotbarItem(gui, ctx, itemId, sx + 3, sy + 3, 16);

                // Stack Count text (if > 1)
                if (count > 1) {
                    String countStr = String.valueOf(count);
                    float scale = 0.46f;
                    float countW = font.getStringWidth(countStr, scale);
                    float tx = sx + slotSize - countW - 1;
                    float ty = sy + slotSize - font.getFontHeight(scale) - 1;
                    font.drawString(gui, countStr, tx, ty, scale, 0xffffff, true);
                }
            }

            // Selected Slot Golden Accent Frame
            if (i == selected) {
                renderSelectedSlotFrame(gui, sx, sy, slotSize, slotSize);
            }
        }
    }

    private void renderSelectedSlotFrame(GuiRenderer gui, int x, int y, int w, int h) {
        int fx = x - 2;
        int fy = y - 2;
        int fw = w + 4;
        int fh = h + 4;

        // Outer dark shadow
        gui.drawRect(fx - 1, fy - 1, fw + 2, fh + 2, 0.03f, 0.03f, 0.05f, 0.80f);

        // Luminous gold border (2px thick on edges)
        float gr = HudTheme.GOLD_R, gg = HudTheme.GOLD_G, gb = HudTheme.GOLD_B;
        gui.drawRect(fx, fy, fw, 2, gr, gg, gb, 1.0f);                 // Top bar
        gui.drawRect(fx, fy + fh - 2, fw, 2, gr, gg, gb, 1.0f);         // Bottom bar
        gui.drawRect(fx, fy, 2, fh, gr, gg, gb, 1.0f);                 // Left bar
        gui.drawRect(fx + fw - 2, fy, 2, fh, gr, gg, gb, 1.0f);         // Right bar

        // Top gold accent gleam
        gui.drawRect(fx + 4, fy, fw - 8, 1, 1.0f, 0.92f, 0.55f, 1.0f);
        // Subtle amber glow
        gui.drawRect(fx + 2, fy + 2, fw - 4, 1, gr, gg, gb, 0.35f);
    }

    private void renderHotbarItem(GuiRenderer gui, HudContext ctx, byte itemId, int x, int y, int size) {
        if (ctx.atlasTextureId == 0) {
            gui.drawRect(x, y, size, size, 0.6f, 0.6f, 0.7f, 1.0f);
            return;
        }

        // Render block face tile from atlas
        BlockType blockType = BlockStateRegistry.getBlockType(itemId);
        if (blockType != null && blockType != Blocks.AIR) {
            int tile = blockType.getDisplayFaceTile();
            float[] uv = TextureAtlas.getTileUV(tile);
            gui.drawTexturedQuad(ctx.atlasTextureId, x, y, size, size, uv[0], uv[3], uv[1], uv[2], 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // C. World Information Overlay (Authentic Vanilla F3 Style Lines)
    // -------------------------------------------------------------------------
    private void renderWorldInfo(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        int x = 2;
        int curY = 2;
        int lineH = 10;

        // 1. Primary Line: Coordinates & Elevation
        if (ctx.settings.showCoordinates && ctx.hasPlayer()) {
            Player p = ctx.player;
            int px = (int) Math.floor(p.getX());
            int py = (int) Math.floor(p.getY());
            int pz = (int) Math.floor(p.getZ());

            if (px != cachedCoordX || py != cachedCoordY || pz != cachedCoordZ) {
                cachedCoordX = px;
                cachedCoordY = py;
                cachedCoordZ = pz;
                int seaDelta = py - 64;
                String seaSign = seaDelta >= 0 ? "+" : "";
                cachedCoordStr = "XYZ: " + px + ", " + py + ", " + pz + " (" + seaSign + seaDelta + "m)";
            }
            renderOverlayLine(gui, font, cachedCoordStr, x, curY, 0xe0e0e0);
            curY += lineH;

            // 2. Primary Line: Biome & World Name
            if (!ctx.currentBiome.equals(cachedBiome)) {
                cachedBiome = ctx.currentBiome;
                cachedBiomeStr = "Biome: " + cachedBiome;
            }
            renderOverlayLine(gui, font, cachedBiomeStr, x, curY, 0xe0e0e0);
            curY += lineH;

            // 3. Secondary Line: Solar Day & Clock
            if (ctx.timeSystem != null) {
                double ticks = ctx.timeSystem.getWorldTimeTicks();
                long day = (long) (ticks / 24000.0);
                boolean isDay = ctx.timeSystem.isDay();

                if (day != cachedDay || isDay != cachedIsDay) {
                    cachedDay = day;
                    cachedIsDay = isDay;
                    cachedSolarTimeStr = "Time: Day " + day + " \u00B7 " + (isDay ? "Daylight" : "Nightfall");
                }
                renderOverlayLine(gui, font, cachedSolarTimeStr, x, curY, 0xd0d0d8);
                curY += lineH;
            }
        }

        // 4. Secondary Line: FPS & Streamed Chunks
        if (ctx.settings.showFps) {
            if (ctx.currentFps != cachedFps || ctx.renderedChunks != cachedRenderedChunks || ctx.loadedChunks != cachedLoadedChunks) {
                cachedFps = ctx.currentFps;
                cachedRenderedChunks = ctx.renderedChunks;
                cachedLoadedChunks = ctx.loadedChunks;
                cachedFpsStr = cachedFps + " FPS \u00B7 Chunks " + cachedRenderedChunks + "/" + cachedLoadedChunks;
            }
            int fpsColor = cachedFps >= 50 ? 0xa0e8a0 : (cachedFps >= 30 ? 0xf0c060 : 0xf07070);
            renderOverlayLine(gui, font, cachedFpsStr, x, curY, fpsColor);
        }
    }

    private void renderOverlayLine(GuiRenderer gui, FontRenderer font, String text, int x, int y, int color) {
        float textW = font.getStringWidth(text, 1);
        gui.drawRect(x - 1, y - 1, textW + 2, 10, 0.0f, 0.0f, 0.0f, 0.40f);
        font.drawString(gui, text, x, y, 1, color, true);
    }

    // -------------------------------------------------------------------------
    // D. In-Game Notifications
    // -------------------------------------------------------------------------
    private void renderNotification(GuiRenderer gui, FontRenderer font, int width, int height) {
        float scale = 0.46f;
        float textW = font.getStringWidth(notificationText, scale);
        float padX = 8;
        float padY = 4;
        float panelW = textW + padX * 2;
        float panelH = font.getFontHeight(scale) + padY * 2;
        float panelX = GuiLayout.centerX(width, (int) panelW);
        float panelY = 24;

        float alpha = Math.clamp(notificationTimer * 2.0f, 0.0f, 1.0f);
        gui.drawRect(panelX, panelY, panelW, panelH, 0.08f, 0.08f, 0.11f, 0.85f * alpha);
        gui.drawRect(panelX, panelY, panelW, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 0.90f * alpha);
        font.drawString(gui, notificationText, panelX + padX, panelY + padY, scale, 0xffffff, true);
    }

    public void cleanup() {
        resetStringCaches();
    }
}
