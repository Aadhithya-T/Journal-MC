package com.mcjournal.client.gui;

import com.mcjournal.Item;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.client.Player;
import com.mcjournal.client.TextureAtlas;

/**
 * Dedicated HUD rendering system for Journal-MC.
 * Renders in virtual logical coordinates (respecting GuiScaleManager)
 * and batches all draw calls through GuiRenderer.
 *
 * Implements:
 * - Original Journal-MC crosshair with interaction-state targeting feedback
 * - 9-slot hotbar with active slot highlight, item tiles, and stack counts
 * - Health indicators with damage hurt-flash
 * - Hunger indicators
 * - Cached, low-allocation world info panel (coordinates, biome, time, FPS)
 * - Contextual target feedback (block name and mining progress)
 * - Damage vignette
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

    private String cachedTargetBlockName = "";
    private String cachedTargetBlockId = "";

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
        cachedTargetBlockId = "";
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

        // 1. Crosshair (Centered)
        if (ctx.settings.showCrosshair) {
            renderCrosshair(gui, width, height, ctx.hasTarget());
        }

        // 2. Contextual Interaction Feedback (Below crosshair)
        if (ctx.hasTarget()) {
            renderContextualTarget(gui, font, ctx, width, height);
        }

        // 3. Hotbar & Selected Item (Bottom Center)
        if (ctx.hasPlayer()) {
            renderHotbar(gui, font, ctx, width, height);
            renderHealthAndHunger(gui, ctx, width, height);
        }

        // 4. World Information Panel (Top Left)
        if (ctx.settings.showCoordinates || ctx.settings.showFps) {
            renderWorldInfo(gui, font, ctx, width, height);
        }

        // 5. Red Damage Hurt Vignette
        if (ctx.hasPlayer() && ctx.player.hurtTime > 0) {
            renderHurtVignette(gui, ctx, width, height);
        }
    }

    // -------------------------------------------------------------------------
    // A. Crosshair
    // -------------------------------------------------------------------------
    private void renderCrosshair(GuiRenderer gui, int width, int height, boolean hasTarget) {
        int cx = width / 2;
        int cy = height / 2;

        // 1. Black outline / drop shadow for high-contrast visibility against any backdrop
        gui.drawRect(cx - 7, cy - 1, 15, 3, 0.05f, 0.05f, 0.08f, 0.75f);
        gui.drawRect(cx - 1, cy - 7, 3, 15, 0.05f, 0.05f, 0.08f, 0.75f);

        // 2. Reticle Arms (Light high-contrast bars)
        float armR = 0.92f, armG = 0.92f, armB = 0.95f, armA = 0.95f;
        // Left arm
        gui.drawRect(cx - 6, cy, 4, 1, armR, armG, armB, armA);
        // Right arm
        gui.drawRect(cx + 3, cy, 4, 1, armR, armG, armB, armA);
        // Top arm
        gui.drawRect(cx, cy - 6, 1, 4, armR, armG, armB, armA);
        // Bottom arm
        gui.drawRect(cx, cy + 3, 1, 4, armR, armG, armB, armA);

        // 3. Center Target Dot
        if (hasTarget) {
            // Subtle warm amber target highlight when pointing at an interactable block
            gui.drawRect(cx, cy, 1, 1, 1.0f, 0.75f, 0.20f, 1.0f);
        } else {
            // Standard center pixel
            gui.drawRect(cx, cy, 1, 1, armR, armG, armB, armA);
        }
    }

    // -------------------------------------------------------------------------
    // B. Hotbar (9 Slots)
    // -------------------------------------------------------------------------
    private void renderHotbar(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        int slotCount = 9;
        int slotSize = 20;
        int totalHotbarW = slotCount * slotSize + 2; // 182px
        int hx = GuiLayout.centerX(width, totalHotbarW);
        int hy = height - slotSize - 3;

        // Hotbar Outer Foundation Panel
        gui.drawBevelBox(hx, hy, totalHotbarW, slotSize + 2, 0x141418, 0x3c3c44, 0x101014);

        int selected = Math.clamp(player.selectedSlot, 0, 8);

        for (int i = 0; i < slotCount; i++) {
            int sx = hx + 1 + i * slotSize;
            int sy = hy + 1;

            // Recessed Slot Cell
            gui.drawRect(sx, sy, slotSize, slotSize, 0.10f, 0.10f, 0.12f, 0.90f);
            gui.drawRect(sx + 1, sy + 1, slotSize - 2, slotSize - 2, 0.06f, 0.06f, 0.08f, 0.95f);

            // Slot Item / Block Content
            byte itemId = player.hotbarBlocks[i];
            int count = player.hotbarCounts[i];

            if (itemId != 0 && count > 0) {
                renderHotbarItem(gui, ctx, itemId, sx + 2, sy + 2, slotSize - 4);

                // Stack Count text (if > 1)
                if (count > 1) {
                    String countStr = String.valueOf(count);
                    float scale = 0.45f;
                    float countW = font.getStringWidth(countStr, scale);
                    float tx = sx + slotSize - countW - 1;
                    float ty = sy + slotSize - font.getFontHeight(scale) - 1;
                    font.drawString(gui, countStr, tx, ty, scale, 0xffffff, true);
                }
            }

            // Selected Slot Golden Accent Frame
            if (i == selected) {
                renderSelectedSlotFrame(gui, sx - 1, sy - 1, slotSize + 2, slotSize + 2);
            }
        }
    }

    private void renderSelectedSlotFrame(GuiRenderer gui, int x, int y, int w, int h) {
        // Bright golden border
        float r = 1.0f, g = 0.82f, b = 0.25f, a = 1.0f;
        gui.drawRect(x, y, w, 1, r, g, b, a);             // Top
        gui.drawRect(x, y + h - 1, w, 1, r, g, b, a);     // Bottom
        gui.drawRect(x, y, 1, h, r, g, b, a);             // Left
        gui.drawRect(x + w - 1, y, 1, h, r, g, b, a);     // Right

        // Inner subtle golden bevel
        gui.drawRect(x + 1, y + 1, w - 2, 1, 0.85f, 0.65f, 0.18f, 0.60f);
    }

    private void renderHotbarItem(GuiRenderer gui, HudContext ctx, byte itemId, int x, int y, int size) {
        if (ctx.atlasTextureId == 0) {
            // Procedural fallback rectangle if atlas not loaded
            gui.drawRect(x, y, size, size, 0.6f, 0.6f, 0.7f, 1.0f);
            return;
        }

        if (Item.isTool(itemId)) {
            // Render 2D tool sprite from atlas
            int tile = Item.getItemTile(itemId);
            float[] uv = TextureAtlas.getTileUV(tile);
            gui.drawTexturedQuad(ctx.atlasTextureId, x, y, size, size, uv[0], uv[3], uv[1], uv[2], 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            // Render block face tile from atlas
            BlockType blockType = BlockStateRegistry.getBlockType(itemId);
            if (blockType != null) {
                int tile = blockType.getDisplayFaceTile();
                float[] uv = TextureAtlas.getTileUV(tile);
                gui.drawTexturedQuad(ctx.atlasTextureId, x, y, size, size, uv[0], uv[3], uv[1], uv[2], 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // C. Health & Hunger Indicators
    // -------------------------------------------------------------------------
    private void renderHealthAndHunger(GuiRenderer gui, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        int hotbarW = 182;
        int hx = GuiLayout.centerX(width, hotbarW);
        int indicatorsY = height - 20 - 14;

        // 1. Health Bar (Left side, 10 Shield/Gem Pips = 20 HP)
        int hp = Math.clamp(player.health, 0, 20);
        boolean isHurt = player.hurtTime > 0;
        int pipW = 8;
        int pipH = 8;
        int gap = 1;

        for (int i = 0; i < 10; i++) {
            int px = hx + i * (pipW + gap);
            int pipValue = hp - (i * 2);

            // Dark stone container background
            gui.drawRect(px, indicatorsY, pipW, pipH, 0.12f, 0.12f, 0.14f, 0.90f);
            gui.drawRect(px + 1, indicatorsY + 1, pipW - 2, pipH - 2, 0.06f, 0.06f, 0.08f, 0.95f);

            // Hardcore gold top accent
            gui.drawRect(px + 1, indicatorsY, pipW - 2, 1, 0.85f, 0.70f, 0.25f, 0.80f);

            if (pipValue >= 2) {
                // Full Pip (Ruby Red or Hurt White Flash)
                if (isHurt) {
                    gui.drawRect(px + 1, indicatorsY + 1, pipW - 2, pipH - 2, 1.0f, 0.90f, 0.90f, 1.0f);
                } else {
                    gui.drawRect(px + 1, indicatorsY + 1, pipW - 2, pipH - 2, 0.85f, 0.12f, 0.18f, 1.0f);
                    // Specular highlight dot
                    gui.drawRect(px + 2, indicatorsY + 2, 2, 2, 1.0f, 0.40f, 0.45f, 1.0f);
                }
            } else if (pipValue == 1) {
                // Half Pip (Left Half Red, Right Half Empty)
                int halfW = (pipW - 2) / 2;
                if (isHurt) {
                    gui.drawRect(px + 1, indicatorsY + 1, halfW, pipH - 2, 1.0f, 0.90f, 0.90f, 1.0f);
                } else {
                    gui.drawRect(px + 1, indicatorsY + 1, halfW, pipH - 2, 0.85f, 0.12f, 0.18f, 1.0f);
                }
            }
        }

        // 2. Hunger Bar (Right side, 10 Amber Pips = 20 Hunger)
        int hunger = Math.clamp(player.hunger, 0, 20);
        int hungerStartX = hx + hotbarW - (10 * (pipW + gap) - gap);

        for (int i = 0; i < 10; i++) {
            // Right-to-left layout: Pip 9 is furthest right
            int px = hungerStartX + (9 - i) * (pipW + gap);
            int pipValue = hunger - (i * 2);

            // Dark stone container background
            gui.drawRect(px, indicatorsY, pipW, pipH, 0.12f, 0.12f, 0.14f, 0.90f);
            gui.drawRect(px + 1, indicatorsY + 1, pipW - 2, pipH - 2, 0.06f, 0.06f, 0.08f, 0.95f);

            // Warm amber top accent
            gui.drawRect(px + 1, indicatorsY, pipW - 2, 1, 0.75f, 0.50f, 0.15f, 0.70f);

            if (pipValue >= 2) {
                // Full Hunger Pip (Amber/Gold)
                gui.drawRect(px + 1, indicatorsY + 1, pipW - 2, pipH - 2, 0.88f, 0.52f, 0.12f, 1.0f);
                gui.drawRect(px + 2, indicatorsY + 2, 2, 2, 1.0f, 0.70f, 0.25f, 1.0f);
            } else if (pipValue == 1) {
                // Half Hunger Pip
                int halfW = (pipW - 2) / 2;
                gui.drawRect(px + 1, indicatorsY + 1, halfW, pipH - 2, 0.88f, 0.52f, 0.12f, 1.0f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // D. World Information Panel (Top Left)
    // -------------------------------------------------------------------------
    private void renderWorldInfo(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        int x = 6;
        int y = 6;
        float fontScale = 0.45f;
        int lineH = 9;

        // 1. FPS & Rendered Chunks Line (if enabled)
        if (ctx.settings.showFps) {
            if (ctx.currentFps != cachedFps || ctx.renderedChunks != cachedRenderedChunks || ctx.loadedChunks != cachedLoadedChunks) {
                cachedFps = ctx.currentFps;
                cachedRenderedChunks = ctx.renderedChunks;
                cachedLoadedChunks = ctx.loadedChunks;
                cachedFpsStr = "FPS: " + cachedFps + " (" + cachedRenderedChunks + "/" + cachedLoadedChunks + " chunks)";
            }
            font.drawString(gui, cachedFpsStr, x, y, fontScale, 0xdddddd, true);
            y += lineH;
        }

        // 2. Coordinates Line (if enabled)
        if (ctx.settings.showCoordinates && ctx.hasPlayer()) {
            Player p = ctx.player;
            int px = (int) Math.floor(p.getX());
            int py = (int) Math.floor(p.getY());
            int pz = (int) Math.floor(p.getZ());

            if (px != cachedCoordX || py != cachedCoordY || pz != cachedCoordZ) {
                cachedCoordX = px;
                cachedCoordY = py;
                cachedCoordZ = pz;
                cachedCoordStr = "XYZ: " + px + " / " + py + " / " + pz;

                int seaDelta = py - 64;
                cachedElevationStr = "Elevation: " + (seaDelta >= 0 ? "+" : "") + seaDelta + "m";
            }

            font.drawString(gui, cachedCoordStr, x, y, fontScale, 0xffffff, true);
            y += lineH;

            // Biome Line
            if (!ctx.currentBiome.equals(cachedBiome)) {
                cachedBiome = ctx.currentBiome;
                cachedBiomeStr = "Biome: " + cachedBiome;
            }
            font.drawString(gui, cachedBiomeStr, x, y, fontScale, 0xaaaaaa, true);
            y += lineH;

            // Solar Time Line
            if (ctx.timeSystem != null) {
                double ticks = ctx.timeSystem.getWorldTimeTicks();
                long day = (long) (ticks / 24000.0);
                boolean isDay = ctx.timeSystem.isDay();

                if (day != cachedDay || isDay != cachedIsDay) {
                    cachedDay = day;
                    cachedIsDay = isDay;
                    cachedSolarTimeStr = "Day " + day + " (" + (isDay ? "Daylight" : "Nightfall") + ")";
                }
                font.drawString(gui, cachedSolarTimeStr, x, y, fontScale, 0xccccaa, true);
                y += lineH;
            }
        }
    }

    // -------------------------------------------------------------------------
    // E. Contextual Target Feedback (Below Crosshair)
    // -------------------------------------------------------------------------
    private void renderContextualTarget(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        if (!ctx.hasTarget() || ctx.targetedBlock.state == null) {
            return;
        }

        BlockType block = ctx.targetedBlock.state.getBlock();
        String blockId = (block != null) ? block.getId() : "";
        if (!blockId.equals(cachedTargetBlockId)) {
            cachedTargetBlockId = blockId;
            cachedTargetBlockName = (block != null) ? block.getName() : "Block";
        }

        float textScale = 0.45f;
        float textW = font.getStringWidth(cachedTargetBlockName, textScale);
        float pillW = textW + 12;
        float pillH = 12;
        float pillX = GuiLayout.centerX(width, (int) pillW);
        float pillY = height / 2.0f + 14;

        // Translucent pill backdrop
        gui.drawRect(pillX, pillY, pillW, pillH, 0.08f, 0.08f, 0.10f, 0.65f);
        gui.drawRect(pillX, pillY, pillW, 1, 0.35f, 0.35f, 0.40f, 0.70f);

        // Block Name Text
        font.drawString(gui, cachedTargetBlockName, pillX + 6, pillY + 2, textScale, 0xffffff, true);

        // Mining Break Progress Bar (if in progress)
        if (ctx.breakProgress > 0.0f) {
            float barW = pillW;
            float barH = 2;
            float barY = pillY + pillH + 2;

            gui.drawRect(pillX, barY, barW, barH, 0.05f, 0.05f, 0.08f, 0.80f);
            float progressW = Math.clamp(ctx.breakProgress, 0.0f, 1.0f) * barW;
            gui.drawRect(pillX, barY, progressW, barH, 1.0f, 0.75f, 0.20f, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // F. Red Hurt Vignette
    // -------------------------------------------------------------------------
    private void renderHurtVignette(GuiRenderer gui, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        float hurtFraction = Math.clamp(((float) player.hurtTime - ctx.partialTick) / (float) player.maxHurtTime, 0.0f, 1.0f);
        if (hurtFraction > 0.0f) {
            float alpha = hurtFraction * 0.32f;
            gui.drawRect(0, 0, width, height, 0.90f, 0.05f, 0.05f, alpha);
        }
    }

    public void cleanup() {
        resetStringCaches();
    }
}
