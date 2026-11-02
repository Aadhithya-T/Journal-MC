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
 * - Distinct, recognizable 9x9 pixel-art symbols for Health (Ruby Shield) and Hunger (Amber Loaf)
 * - 9-slot hotbar with dark stone foundation, recessed slots, and luminous gold selected frame
 * - Structured World Information Panel with frosted slate card styling and string caching
 * - Centered crosshair with target dot and connected contextual target block tooltip
 * - Mining break progress bar
 * - Damage hurt vignette
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

        // 1. Crosshair & Contextual Target Feedback (Center)
        if (ctx.settings.showCrosshair) {
            renderCrosshair(gui, width, height, ctx.hasTarget());
        }

        if (ctx.hasTarget()) {
            renderContextualTarget(gui, font, ctx, width, height);
        }

        // 2. Hotbar & Health/Hunger Indicators (Bottom Center)
        if (ctx.hasPlayer()) {
            renderHotbar(gui, font, ctx, width, height);
            renderHealthAndHunger(gui, ctx, width, height);
        }

        // 3. World Information Panel (Top Left)
        if (ctx.settings.showCoordinates || ctx.settings.showFps) {
            renderWorldInfo(gui, font, ctx, width, height);
        }

        // 4. Red Damage Hurt Vignette
        if (ctx.hasPlayer() && ctx.player.hurtTime > 0) {
            renderHurtVignette(gui, ctx, width, height);
        }
    }

    // -------------------------------------------------------------------------
    // A. Crosshair & Visual Target Connection
    // -------------------------------------------------------------------------
    private void renderCrosshair(GuiRenderer gui, int width, int height, boolean hasTarget) {
        int cx = width / 2;
        int cy = height / 2;

        // 1. Deep shadow backing for crisp contrast against bright sky, snow, or dark caves
        gui.drawRect(cx - 6, cy - 1, 13, 3, 0.03f, 0.03f, 0.05f, 0.80f);
        gui.drawRect(cx - 1, cy - 6, 3, 13, 0.03f, 0.03f, 0.05f, 0.80f);

        // 2. Reticle Arms (Light high-contrast bars)
        float r = 0.94f, g = 0.94f, b = 0.97f, a = 0.95f;
        gui.drawRect(cx - 5, cy, 3, 1, r, g, b, a); // Left
        gui.drawRect(cx + 3, cy, 3, 1, r, g, b, a); // Right
        gui.drawRect(cx, cy - 5, 1, 3, r, g, b, a); // Top
        gui.drawRect(cx, cy + 3, 1, 3, r, g, b, a); // Bottom

        // 3. Center Target Pip
        if (hasTarget) {
            // Luminous gold dot when targeting an interactable block
            gui.drawRect(cx, cy, 1, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 1.0f);
        } else {
            gui.drawRect(cx, cy, 1, 1, r, g, b, 0.70f);
        }
    }

    // -------------------------------------------------------------------------
    // B. Hotbar (9 Slots with Stone Foundation & Luminous Gold Selection)
    // -------------------------------------------------------------------------
    private void renderHotbar(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        int slotCount = HudTheme.SLOT_COUNT;
        int slotSize = HudTheme.SLOT_SIZE;
        int totalHotbarW = HudTheme.HOTBAR_WIDTH; // 202px
        int totalHotbarH = HudTheme.HOTBAR_HEIGHT; // 26px
        int hx = GuiLayout.centerX(width, totalHotbarW);
        int hy = height - totalHotbarH - 3;

        // 1. Dark Stone Foundation Panel with subtle outer bevel
        gui.drawRect(hx, hy, totalHotbarW, totalHotbarH, 0.07f, 0.07f, 0.09f, HudTheme.PANEL_ALPHA);
        gui.drawRect(hx, hy, totalHotbarW, 1, 0.24f, 0.24f, 0.28f, 0.85f);             // Top border
        gui.drawRect(hx, hy + totalHotbarH - 1, totalHotbarW, 1, 0.04f, 0.04f, 0.06f, 0.90f); // Bottom border
        gui.drawRect(hx, hy, 1, totalHotbarH, 0.24f, 0.24f, 0.28f, 0.85f);             // Left border
        gui.drawRect(hx + totalHotbarW - 1, hy, 1, totalHotbarH, 0.04f, 0.04f, 0.06f, 0.90f); // Right border

        int selected = Math.clamp(player.selectedSlot, 0, 8);

        for (int i = 0; i < slotCount; i++) {
            int sx = hx + HudTheme.HOTBAR_PADDING + i * slotSize;
            int sy = hy + HudTheme.HOTBAR_PADDING;

            // Recessed Slot Cavity
            gui.drawRect(sx, sy, slotSize, slotSize, 0.05f, 0.05f, 0.07f, 0.95f);
            gui.drawRect(sx, sy, slotSize, 1, 0.02f, 0.02f, 0.03f, 0.85f); // Inner top drop shadow
            gui.drawRect(sx, sy, 1, slotSize, 0.02f, 0.02f, 0.03f, 0.85f); // Inner left drop shadow
            gui.drawRect(sx + slotSize - 1, sy, 1, slotSize, 0.16f, 0.16f, 0.20f, 0.60f); // Divider

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
    // C. Health (Ruby Shield) & Hunger (Amber Loaf) Indicators
    // -------------------------------------------------------------------------
    private void renderHealthAndHunger(GuiRenderer gui, HudContext ctx, int width, int height) {
        Player player = ctx.player;
        int hotbarW = HudTheme.HOTBAR_WIDTH;
        int hx = GuiLayout.centerX(width, hotbarW);
        int indicatorsY = height - HudTheme.HOTBAR_HEIGHT - 15;

        // 1. Health Bar (Left: 10 Ruby Shields = 20 HP)
        int hp = Math.clamp(player.health, 0, 20);
        boolean isHurt = player.hurtTime > 0;
        int iconSize = HudTheme.ICON_SIZE;
        int gap = HudTheme.ICON_GAP;

        for (int i = 0; i < 10; i++) {
            int px = hx + 1 + i * (iconSize + gap);
            int pipValue = hp - (i * 2);
            renderHealthGem(gui, px, indicatorsY, pipValue, isHurt);
        }

        // 2. Hunger Bar (Right: 10 Amber Loaves = 20 Hunger)
        int hunger = Math.clamp(player.hunger, 0, 20);
        int hungerStartX = hx + hotbarW - HudTheme.STATUS_BAR_WIDTH - 1;

        for (int i = 0; i < 10; i++) {
            // Right-to-left layout: Pip 9 is furthest right
            int px = hungerStartX + (9 - i) * (iconSize + gap);
            int pipValue = hunger - (i * 2);
            renderHungerLoaf(gui, px, indicatorsY, pipValue);
        }
    }

    private void renderHealthGem(GuiRenderer gui, int x, int y, int pipValue, boolean isHurt) {
        // 9x9 Health Crest / Shield Gem
        float bgR = 0.08f, bgG = 0.08f, bgB = 0.10f, bgA = 0.85f;

        // Top gold crest accent
        gui.drawRect(x + 1, y, 2, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 0.90f);
        gui.drawRect(x + 5, y, 2, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 0.90f);

        // Stone boundary frame
        gui.drawRect(x, y + 1, 1, 4, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 8, y + 1, 1, 4, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 4, y + 1, 1, 2, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 1, y + 5, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 7, y + 5, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 2, y + 6, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 6, y + 6, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 3, y + 7, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 5, y + 7, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 4, y + 8, 1, 1, bgR, bgG, bgB, bgA);

        // Background container fill (dark hollow stone cavity)
        float emptyR = 0.15f, emptyG = 0.15f, emptyB = 0.18f, emptyA = 0.90f;
        gui.drawRect(x + 1, y + 1, 3, 4, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 5, y + 1, 3, 4, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 4, y + 3, 1, 2, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 2, y + 5, 5, 1, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 3, y + 6, 3, 1, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 4, y + 7, 1, 1, emptyR, emptyG, emptyB, emptyA);

        if (pipValue <= 0) return;

        // Ruby Gem Fill (Full or Half)
        float fillR = isHurt ? 1.0f : 0.86f;
        float fillG = isHurt ? 0.94f : 0.12f;
        float fillB = isHurt ? 0.90f : 0.16f;
        float fillA = 1.0f;

        if (pipValue >= 2) {
            // Full Ruby Gem
            gui.drawRect(x + 1, y + 1, 3, 4, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 5, y + 1, 3, 4, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 4, y + 3, 1, 2, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 2, y + 5, 5, 1, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 3, y + 6, 3, 1, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 4, y + 7, 1, 1, fillR, fillG, fillB, fillA);

            if (!isHurt) {
                // Specular gleam highlight on top-left lobe
                gui.drawRect(x + 2, y + 2, 1, 1, 1.0f, 0.45f, 0.52f, 1.0f);
                // Subtle dark ruby shade along bottom facets
                gui.drawRect(x + 3, y + 6, 3, 1, 0.55f, 0.06f, 0.10f, 1.0f);
                gui.drawRect(x + 4, y + 7, 1, 1, 0.45f, 0.05f, 0.08f, 1.0f);
            }
        } else {
            // Half Ruby Gem (Left lobe filled, right lobe hollow)
            gui.drawRect(x + 1, y + 1, 3, 4, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 2, y + 5, 3, 1, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 3, y + 6, 1, 1, fillR, fillG, fillB, fillA);

            if (!isHurt) {
                gui.drawRect(x + 2, y + 2, 1, 1, 1.0f, 0.45f, 0.52f, 1.0f);
            }
        }
    }

    private void renderHungerLoaf(GuiRenderer gui, int x, int y, int pipValue) {
        // 9x9 Hunger Amber Loaf / Drumstick Crest
        float bgR = 0.12f, bgG = 0.08f, bgB = 0.05f, bgA = 0.85f;

        // Outline silhouette
        gui.drawRect(x + 3, y, 4, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 2, y + 1, 1, 2, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 7, y + 1, 1, 4, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 1, y + 3, 1, 3, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 2, y + 6, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 6, y + 5, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 5, y + 6, 1, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 3, y + 7, 2, 1, bgR, bgG, bgB, bgA);
        gui.drawRect(x + 2, y + 8, 1, 1, bgR, bgG, bgB, bgA);

        // Hollow container fill
        float emptyR = 0.18f, emptyG = 0.14f, emptyB = 0.10f, emptyA = 0.90f;
        gui.drawRect(x + 3, y + 1, 4, 2, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 2, y + 3, 5, 2, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 2, y + 5, 4, 1, emptyR, emptyG, emptyB, emptyA);
        gui.drawRect(x + 3, y + 6, 2, 1, emptyR, emptyG, emptyB, emptyA);

        if (pipValue <= 0) return;

        float fillR = 0.88f, fillG = 0.52f, fillB = 0.10f, fillA = 1.0f;

        if (pipValue >= 2) {
            // Full Amber Roast Loaf
            gui.drawRect(x + 3, y + 1, 4, 2, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 2, y + 3, 5, 2, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 2, y + 5, 4, 1, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 3, y + 6, 2, 1, fillR, fillG, fillB, fillA);

            // Caramel glaze top highlight
            gui.drawRect(x + 4, y + 1, 2, 1, 1.0f, 0.74f, 0.28f, 1.0f);
            gui.drawRect(x + 5, y + 2, 1, 1, 1.0f, 0.74f, 0.28f, 1.0f);
            // Rich roasted shadow along bottom
            gui.drawRect(x + 2, y + 5, 4, 1, 0.55f, 0.30f, 0.05f, 1.0f);
        } else {
            // Half Amber Loaf (Right portion filled, left portion hollow)
            gui.drawRect(x + 4, y + 1, 3, 2, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 4, y + 3, 3, 2, fillR, fillG, fillB, fillA);
            gui.drawRect(x + 4, y + 5, 2, 1, fillR, fillG, fillB, fillA);

            gui.drawRect(x + 5, y + 1, 1, 1, 1.0f, 0.74f, 0.28f, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // D. World Information Panel (Structured Frosted Slate Card)
    // -------------------------------------------------------------------------
    private void renderWorldInfo(GuiRenderer gui, FontRenderer font, HudContext ctx, int width, int height) {
        int x = 6;
        int y = 6;
        float fontScale = 0.45f;
        int lineH = 10;
        int padX = 8;
        int padY = 6;

        // Determine active line count
        int lines = 0;
        if (ctx.settings.showCoordinates && ctx.hasPlayer()) lines += 3;
        if (ctx.settings.showFps) lines += 1;
        if (lines == 0) return;

        int panelW = 188;
        int panelH = padY * 2 + lines * lineH - 1;

        // Frosted Charcoal Slate Panel with subtle border
        gui.drawRect(x, y, panelW, panelH, 0.07f, 0.07f, 0.09f, 0.82f);
        gui.drawRect(x, y, panelW, 1, 0.22f, 0.22f, 0.28f, 0.80f);             // Top border
        gui.drawRect(x, y + panelH - 1, panelW, 1, 0.05f, 0.05f, 0.07f, 0.90f); // Bottom border
        gui.drawRect(x, y, 1, panelH, 0.22f, 0.22f, 0.28f, 0.80f);             // Left border
        gui.drawRect(x + panelW - 1, y, 1, panelH, 0.05f, 0.05f, 0.07f, 0.90f); // Right border

        // Signature Journal-MC Gold Accent Notch on top-left of panel
        gui.drawRect(x, y, 20, 2, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 1.0f);

        int curY = y + padY;

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
                cachedCoordStr = "XYZ: " + px + " " + py + " " + pz + " (" + seaSign + seaDelta + "m)";
            }
            font.drawString(gui, "§6XYZ:§r " + cachedCoordX + ", " + cachedCoordY + ", " + cachedCoordZ + " §7(" + (cachedCoordY - 64 >= 0 ? "+" : "") + (cachedCoordY - 64) + "m)§r", x + padX, curY, fontScale, 0xffffff, true);
            curY += lineH;

            // 2. Primary Line: Biome & World Name
            if (!ctx.currentBiome.equals(cachedBiome)) {
                cachedBiome = ctx.currentBiome;
                cachedBiomeStr = "Biome: " + cachedBiome;
            }
            font.drawString(gui, "§7Biome:§r " + cachedBiome, x + padX, curY, fontScale, 0xe0e0e8, true);
            curY += lineH;

            // 3. Secondary Line: Solar Day & Clock
            if (ctx.timeSystem != null) {
                double ticks = ctx.timeSystem.getWorldTimeTicks();
                long day = (long) (ticks / 24000.0);
                boolean isDay = ctx.timeSystem.isDay();

                if (day != cachedDay || isDay != cachedIsDay) {
                    cachedDay = day;
                    cachedIsDay = isDay;
                    cachedSolarTimeStr = "Day " + day + "  •  " + (isDay ? "Daylight" : "Nightfall");
                }
                font.drawString(gui, "§7Time:§r Day " + day + " §8•§r " + (isDay ? "§eDaylight§r" : "§9Nightfall§r"), x + padX, curY, fontScale, 0xd0d0d8, true);
                curY += lineH;
            }
        }

        // 4. Secondary Line: FPS & Streamed Chunks
        if (ctx.settings.showFps) {
            if (ctx.currentFps != cachedFps || ctx.renderedChunks != cachedRenderedChunks || ctx.loadedChunks != cachedLoadedChunks) {
                cachedFps = ctx.currentFps;
                cachedRenderedChunks = ctx.renderedChunks;
                cachedLoadedChunks = ctx.loadedChunks;
                cachedFpsStr = cachedFps + " FPS  •  " + cachedRenderedChunks + "/" + cachedLoadedChunks + " chunks";
            }
            int fpsColor = cachedFps >= 50 ? 0x90e090 : (cachedFps >= 30 ? 0xf0c060 : 0xf07070);
            font.drawString(gui, "§7Perf:§r " + cachedFps + " FPS §8•§r " + cachedRenderedChunks + "/" + cachedLoadedChunks + " chk", x + padX, curY, fontScale, fpsColor, true);
        }
    }

    // -------------------------------------------------------------------------
    // E. Contextual Target Feedback (Connected to Crosshair Reticle)
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

        float textScale = 0.46f;
        float textW = font.getStringWidth(cachedTargetBlockName, textScale);
        float pillW = Math.max(textW + 16, 44);
        float pillH = 14;
        int cx = width / 2;
        int cy = height / 2;
        float pillX = GuiLayout.centerX(width, (int) pillW);
        float pillY = cy + 10;

        // Visual connector from crosshair bottom to target card
        gui.drawRect(cx, cy + 6, 1, 4, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 0.65f);

        // Translucent frosted stone capsule
        gui.drawRect(pillX, pillY, pillW, pillH, 0.08f, 0.08f, 0.11f, 0.85f);
        gui.drawRect(pillX, pillY, pillW, 1, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 0.90f); // Top gold accent
        gui.drawRect(pillX, pillY + pillH - 1, pillW, 1, 0.04f, 0.04f, 0.06f, 0.90f);                   // Bottom border
        gui.drawRect(pillX, pillY, 1, pillH, 0.25f, 0.25f, 0.30f, 0.80f);                               // Left border
        gui.drawRect(pillX + pillW - 1, pillY, 1, pillH, 0.04f, 0.04f, 0.06f, 0.90f);                   // Right border

        // Centered block name
        float nameX = pillX + (pillW - textW) / 2.0f;
        font.drawString(gui, cachedTargetBlockName, nameX, pillY + 3, textScale, 0xffffff, true);

        // Mining Progress Bar (Directly below capsule)
        if (ctx.breakProgress > 0.0f) {
            float barW = pillW;
            float barH = 2;
            float barY = pillY + pillH + 2;

            // Recessed track
            gui.drawRect(pillX, barY, barW, barH, 0.04f, 0.04f, 0.06f, 0.90f);

            // Radiant gold-to-emerald progress fill
            float progress = Math.clamp(ctx.breakProgress, 0.0f, 1.0f);
            float progressW = progress * barW;
            gui.drawRect(pillX, barY, progressW, barH, HudTheme.GOLD_R, HudTheme.GOLD_G, HudTheme.GOLD_B, 1.0f);
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
