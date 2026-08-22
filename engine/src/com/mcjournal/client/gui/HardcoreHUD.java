package com.mcjournal.client.gui;

import com.mcjournal.Block;
import com.mcjournal.Item;
import com.mcjournal.client.Player;
import com.mcjournal.client.TextureAtlas;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class HardcoreHUD {
    private static final int TEX_SIZE = 64;
    private int iconTextureId = 0;

    // Sprite Slot Indices
    public static final int SPRITE_HARDCORE_HEART_CONTAINER = 0;
    public static final int SPRITE_HARDCORE_HEART_FULL = 1;
    public static final int SPRITE_HARDCORE_HEART_HALF = 2;
    public static final int SPRITE_HEART_CONTAINER = 3;
    public static final int SPRITE_HEART_FULL = 4;
    public static final int SPRITE_HEART_HALF = 5;
    public static final int SPRITE_HUNGER_CONTAINER = 6;
    public static final int SPRITE_HUNGER_FULL = 7;
    public static final int SPRITE_HUNGER_HALF = 8;
    public static final int SPRITE_IRON_AXE = 9;
    public static final int SPRITE_IRON_SHOVEL = 10;
    public static final int SPRITE_IRON_PICKAXE = 11;

    // 1. Hardcore Heart Sprites (9x9 pixels)
    private static final String[] HARDCORE_HEART_CONTAINER_ART = {
        ". # # . . . # # .",
        "# o o # . # o o #",
        "# o o o # o o o #",
        "# o o o o o o o #",
        "# o o o o o o k #",
        ". # o o o o k # .",
        ". . # o o k # . .",
        ". . . # k # . . .",
        ". . . . # . . . ."
    };

    private static final String[] HARDCORE_HEART_FULL_ART = {
        ". # # . . . # # .",
        "# W w # . # R r #",
        "# w E R # R E r #",
        "# R R R R R r r #",
        "# R R R R r r d #",
        ". # R R r r d # .",
        ". . # R r d # . .",
        ". . . # d # . . .",
        ". . . . # . . . ."
    };

    private static final String[] HARDCORE_HEART_HALF_ART = {
        ". # # . . . # # .",
        "# W w # . # o o #",
        "# w E R # o o o #",
        "# R R R # o o o #",
        "# R R R # o o k #",
        ". # R R # o k # .",
        ". . # R # k # . .",
        ". . . # k # . . .",
        ". . . . # . . . ."
    };

    // 2. Normal Heart Sprites (9x9 pixels)
    private static final String[] HEART_CONTAINER_ART = {
        ". # # . . . # # .",
        "# o o # . # o o #",
        "# o o o # o o o #",
        "# o o o o o o o #",
        "# o o o o o o k #",
        ". # o o o o k # .",
        ". . # o o k # . .",
        ". . . # k # . . .",
        ". . . . # . . . ."
    };

    private static final String[] HEART_FULL_ART = {
        ". # # . . . # # .",
        "# W w # . # R r #",
        "# w R R # R R r #",
        "# R R R R R r r #",
        "# R R R R r r d #",
        ". # R R r r d # .",
        ". . # R r d # . .",
        ". . . # d # . . .",
        ". . . . # . . . ."
    };

    private static final String[] HEART_HALF_ART = {
        ". # # . . . # # .",
        "# W w # . # o o #",
        "# w R R # o o o #",
        "# R R R # o o o #",
        "# R R R # o o k #",
        ". # R R # o k # .",
        ". . # R # k # . .",
        ". . . # k # . . .",
        ". . . . # . . . ."
    };

    // 3. Hunger Drumstick Sprites (9x9 pixels)
    private static final String[] HUNGER_CONTAINER_ART = {
        ". . . . # # # . .",
        ". . . # c c c # .",
        ". . # c c c c k #",
        ". . # c c c c k #",
        ". . # c c c k k #",
        ". . . # c k k # .",
        ". b B . # # # . .",
        "b s B b . . . . .",
        ". b b . . . . . ."
    };

    private static final String[] HUNGER_FULL_ART = {
        ". . . . # # # . .",
        ". . . # H H M # .",
        ". . # H H M M D #",
        ". . # H M M M D #",
        ". . # M M M D D #",
        ". . . # M D D # .",
        ". b B . # # # . .",
        "b s B b . . . . .",
        ". b b . . . . . ."
    };

    private static final String[] HUNGER_HALF_ART = {
        ". . . . # # # . .",
        ". . . # H # c # .",
        ". . # H H # c k #",
        ". . # H M # c k #",
        ". . # M M # k k #",
        ". . . # M # k # .",
        ". b B . # # # . .",
        "b s B b . . . . .",
        ". b b . . . . . ."
    };

    // 4. Iron Axe Sprite (16x16 pixels - Exact Replica)
    private static final String[] IRON_AXE_ART = {
        ". . . . . . . . # # # # . . . .",
        ". . . . . . . # W W w I # . . .",
        ". . . . . . # W W w I M S # . .",
        ". . . . . # W W w I M H B # . .",
        ". . . . # W W w I M H B I M # .",
        ". . . . # W w I # # H B I M # .",
        ". . . . . # # # . . H B # # . .",
        ". . . . . . . . . H B . . . . .",
        ". . . . . . . . H B . . . . . .",
        ". . . . . . . H B . . . . . . .",
        ". . . . . . H B . . . . . . . .",
        ". . . . . H B . . . . . . . . .",
        ". . . . H B . . . . . . . . . .",
        ". . . H B . . . . . . . . . . .",
        ". . K B . . . . . . . . . . . .",
        ". . . . . . . . . . . . . . . ."
    };

    // 5. Iron Shovel Sprite (16x16 pixels - Exact Replica)
    private static final String[] IRON_SHOVEL_ART = {
        ". . . . . . . . . . . # # # . .",
        ". . . . . . . . . . # W W I # .",
        ". . . . . . . . . # W W I M S #",
        ". . . . . . . . # W W I M S # .",
        ". . . . . . . # W W I M S # . .",
        ". . . . . . . # W I M S # . . .",
        ". . . . . . # . # S S # . . . .",
        ". . . . . # H . . # # . . . . .",
        ". . . . # H B . . . . . . . . .",
        ". . . # H B . . . . . . . . . .",
        ". . # H B . . . . . . . . . . .",
        ". # H B . . . . . . . . . . . .",
        "# H B . . . . . . . . . . . . .",
        "# H B . . . . . . . . . . . . .",
        "K B . . . . . . . . . . . . . .",
        ". . . . . . . . . . . . . . . ."
    };

    // 6. Iron Pickaxe Sprite (16x16 pixels - Exact Replica)
    private static final String[] IRON_PICKAXE_ART = {
        ". . . . # # # # # # # # # . . .",
        ". . . # W W W I I I I M S # . .",
        ". . # W W I I M S S S S M D # .",
        ". # W W I S S D # # # # D M # .",
        "# W I S S D # . . . . # S M # .",
        "# I S D # . . . . . H . # D # #",
        "# S D # . . . . . H B . . # # .",
        "# D # . . . . . H B . . . . . .",
        ". # . . . . . H B . . . . . . .",
        ". . . . . . H B . . . . . . . .",
        ". . . . . H B . . . . . . . . .",
        ". . . . H B . . . . . . . . . .",
        ". . . H B . . . . . . . . . . .",
        ". . H B . . . . . . . . . . . .",
        ". K B . . . . . . . . . . . . .",
        ". . . . . . . . . . . . . . . ."
    };

    public void init() {
        if (iconTextureId != 0) return;

        ByteBuffer buffer = MemoryUtil.memAlloc(TEX_SIZE * TEX_SIZE * 4);
        for (int i = 0; i < TEX_SIZE * TEX_SIZE * 4; i++) {
            buffer.put((byte) 0);
        }
        buffer.flip();

        // 1. Color Palettes
        Map<Character, Integer> hcContainerMap = new HashMap<>();
        hcContainerMap.put('.', 0x00000000);
        hcContainerMap.put('#', 0xFF000000);
        hcContainerMap.put('o', 0xFF2B1414);
        hcContainerMap.put('k', 0xFF140808);

        Map<Character, Integer> hcFullMap = new HashMap<>(hcContainerMap);
        hcFullMap.put('W', 0xFFFFFFFF);
        hcFullMap.put('w', 0xFFFF8E8E);
        hcFullMap.put('E', 0xFFFFFF66); // Golden-white hardcore eye glint
        hcFullMap.put('R', 0xFFE00808);
        hcFullMap.put('r', 0xFFB30000);
        hcFullMap.put('d', 0xFF660000);

        Map<Character, Integer> normContainerMap = new HashMap<>();
        normContainerMap.put('.', 0x00000000);
        normContainerMap.put('#', 0xFF000000);
        normContainerMap.put('o', 0xFF383838);
        normContainerMap.put('k', 0xFF1A1A1A);

        Map<Character, Integer> normFullMap = new HashMap<>(normContainerMap);
        normFullMap.put('W', 0xFFFFFFFF);
        normFullMap.put('w', 0xFFFF9E9E);
        normFullMap.put('R', 0xFFFF2222);
        normFullMap.put('r', 0xFFCC0000);
        normFullMap.put('d', 0xFF770000);

        Map<Character, Integer> hungerContainerMap = new HashMap<>();
        hungerContainerMap.put('.', 0x00000000);
        hungerContainerMap.put('#', 0xFF1A0E04);
        hungerContainerMap.put('b', 0xFF110802);
        hungerContainerMap.put('c', 0xFF362214);
        hungerContainerMap.put('k', 0xFF1A1008);
        hungerContainerMap.put('B', 0xFF2E2E2E);
        hungerContainerMap.put('s', 0xFF1A1A1A);

        Map<Character, Integer> hungerFullMap = new HashMap<>(hungerContainerMap);
        hungerFullMap.put('#', 0xFF241002);
        hungerFullMap.put('b', 0xFF110802);
        hungerFullMap.put('B', 0xFFFFFFFF);
        hungerFullMap.put('s', 0xFFB5B5B5);
        hungerFullMap.put('H', 0xFFECA642);
        hungerFullMap.put('M', 0xFFB86014);
        hungerFullMap.put('D', 0xFF7E3506);

        Map<Character, Integer> axeMap = new HashMap<>();
        axeMap.put('.', 0x00000000);
        axeMap.put('#', 0xFF2B2B2B); // Outline
        axeMap.put('W', 0xFFFFFFFF); // Highlight
        axeMap.put('w', 0xFFEDEDED); // Soft white
        axeMap.put('I', 0xFFDBDBDB); // Light iron base
        axeMap.put('M', 0xFFB0B0B0); // Midtone iron
        axeMap.put('S', 0xFF8A8A8A); // Shaded iron
        axeMap.put('D', 0xFF4D4D4D); // Deep iron shadow
        axeMap.put('H', 0xFF8E6332); // Oak stick highlight
        axeMap.put('B', 0xFF5A3716); // Oak stick shadow
        axeMap.put('K', 0xFF35200C); // Oak stick pommel base

        // Blit 9x9 sprites onto 64x64 Texture
        blitSprite(buffer, HARDCORE_HEART_CONTAINER_ART, hcContainerMap, 0, 0);
        blitSprite(buffer, HARDCORE_HEART_FULL_ART, hcFullMap, 10, 0);
        blitSprite(buffer, HARDCORE_HEART_HALF_ART, hcFullMap, 20, 0);

        blitSprite(buffer, HEART_CONTAINER_ART, normContainerMap, 30, 0);
        blitSprite(buffer, HEART_FULL_ART, normFullMap, 40, 0);
        blitSprite(buffer, HEART_HALF_ART, normFullMap, 50, 0);

        blitSprite(buffer, HUNGER_CONTAINER_ART, hungerContainerMap, 0, 10);
        blitSprite(buffer, HUNGER_FULL_ART, hungerFullMap, 10, 10);
        blitSprite(buffer, HUNGER_HALF_ART, hungerFullMap, 20, 10);

        // Blit 16x16 Iron Axe Sprite
        blitSprite(buffer, IRON_AXE_ART, axeMap, 0, 24);

        // Blit 16x16 Iron Shovel Sprite
        blitSprite(buffer, IRON_SHOVEL_ART, axeMap, 18, 24);

        // Blit 16x16 Iron Pickaxe Sprite
        blitSprite(buffer, IRON_PICKAXE_ART, axeMap, 36, 24);

        // Upload to OpenGL Texture
        iconTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, iconTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, TEX_SIZE, TEX_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void blitSprite(ByteBuffer buf, String[] art, Map<Character, Integer> colorMap, int startX, int startY) {
        for (int y = 0; y < art.length; y++) {
            String line = art[y].replace(" ", "");
            for (int x = 0; x < line.length(); x++) {
                char c = line.charAt(x);
                int argb = colorMap.getOrDefault(c, 0x00000000);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                int dstIdx = ((startY + y) * TEX_SIZE + (startX + x)) * 4;
                buf.put(dstIdx, (byte) r);
                buf.put(dstIdx + 1, (byte) g);
                buf.put(dstIdx + 2, (byte) b);
                buf.put(dstIdx + 3, (byte) a);
            }
        }
    }

    public void drawIcon(GuiRenderer gui, int spriteIndex, float x, float y, float w, float h) {
        if (iconTextureId == 0) return;

        if (spriteIndex == SPRITE_IRON_AXE) {
            float u0 = 0.0f;
            float v0 = 24.0f / (float) TEX_SIZE;
            float u1 = 16.0f / (float) TEX_SIZE;
            float v1 = 40.0f / (float) TEX_SIZE;
            gui.drawTexturedQuad(iconTextureId, x, y, w, h, u0, v0, u1, v1, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        if (spriteIndex == SPRITE_IRON_SHOVEL) {
            float u0 = 18.0f / (float) TEX_SIZE;
            float v0 = 24.0f / (float) TEX_SIZE;
            float u1 = 34.0f / (float) TEX_SIZE;
            float v1 = 40.0f / (float) TEX_SIZE;
            gui.drawTexturedQuad(iconTextureId, x, y, w, h, u0, v0, u1, v1, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        if (spriteIndex == SPRITE_IRON_PICKAXE) {
            float u0 = 36.0f / (float) TEX_SIZE;
            float v0 = 24.0f / (float) TEX_SIZE;
            float u1 = 52.0f / (float) TEX_SIZE;
            float v1 = 40.0f / (float) TEX_SIZE;
            gui.drawTexturedQuad(iconTextureId, x, y, w, h, u0, v0, u1, v1, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        int x0 = (spriteIndex % 6) * 10;
        int y0 = (spriteIndex / 6) * 10;
        float u0 = (float) x0 / (float) TEX_SIZE;
        float v0 = (float) y0 / (float) TEX_SIZE;
        float u1 = (float) (x0 + 9) / (float) TEX_SIZE;
        float v1 = (float) (y0 + 9) / (float) TEX_SIZE;

        gui.drawTexturedQuad(iconTextureId, x, y, w, h, u0, v0, u1, v1, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void render(GuiRenderer gui, FontRenderer font, Player player, int screenWidth, int screenHeight, String biomeName, int atlasTextureId) {
        int centerX = screenWidth / 2;

        // 1. Crosshair in Screen Center
        int cy = screenHeight / 2;
        gui.drawRect(centerX - 8, cy - 1, 16, 2, 1.0f, 1.0f, 1.0f, 0.85f);
        gui.drawRect(centerX - 1, cy - 8, 2, 16, 1.0f, 1.0f, 1.0f, 0.85f);

        // 2. 9-Slot Hotbar (Bottom Center)
        int slotSize = 40;
        int hotbarW = 9 * slotSize;
        int hotbarX = centerX - hotbarW / 2;
        int hotbarY = screenHeight - 52;

        gui.drawBevelBox(hotbarX - 4, hotbarY - 4, hotbarW + 8, slotSize + 8, 0x222222, 0x555555, 0x111111);

        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + i * slotSize;
            boolean isSelected = (i == player.selectedSlot);

            int bg = isSelected ? 0x4a4a4a : 0x2a2a2a;
            int borderL = isSelected ? 0xffffff : 0x555555;
            int borderD = isSelected ? 0xffffff : 0x111111;

            gui.drawBevelBox(slotX, hotbarY, slotSize - 2, slotSize - 2, bg, borderL, borderD);

            // Render Item Icon in Slot (Authentic Minecraft 3D Isometric Block, 2D Foliage, or Tool Sprite)
            byte blockType = player.hotbarBlocks[i];
            int count = player.hotbarCounts[i];

            if (blockType != Block.AIR && count > 0) {
                drawBlockItemInSlot(gui, atlasTextureId, blockType, slotX, hotbarY, slotSize);

                // Render Stack Count in bottom right (Tools like Iron Axe do not display stack numbers)
                if (count > 1 && !Item.isTool(blockType)) {
                    String countStr = String.valueOf(count);
                    float countScale = 0.70f;
                    int textW = (int) font.getStringWidth(countStr, countScale);
                    int textX = slotX + slotSize - 4 - textW;
                    int textY = hotbarY + slotSize - 14;
                    font.drawString(gui, countStr, textX, textY, countScale, 0xffffff, true);
                }
            }

            // Slot numbers (1-9) in top-left
            font.drawString(gui, String.valueOf(i + 1), slotX + 3, hotbarY + 3, 0.55f, isSelected ? 0xffffa0 : 0x777777, false);
        }

        // 3. 10 Hardcore Hearts (Left of Hotbar)
        int iconSize = 18; // 9x9 at 2x crisp scale
        int iconSpacing = 16;
        int statsY = hotbarY - 24;

        int fullHearts = player.health / 2;
        boolean halfHeart = (player.health % 2 == 1);

        for (int h = 0; h < 10; h++) {
            int heartX = hotbarX + h * iconSpacing;
            int heartY = statsY;

            // Low-health heartbeat jitter (Authentic Minecraft feel)
            if (player.health <= 4 && (h % 2 == 0)) {
                heartY += ((System.currentTimeMillis() / 80 + h) % 2 == 0) ? -2 : 0;
            }

            // Always draw dark hardcore container backing
            drawIcon(gui, SPRITE_HARDCORE_HEART_CONTAINER, heartX, heartY, iconSize, iconSize);

            if (h < fullHearts) {
                // Hardcore Crimson Full Heart
                drawIcon(gui, SPRITE_HARDCORE_HEART_FULL, heartX, heartY, iconSize, iconSize);
            } else if (h == fullHearts && halfHeart) {
                // Half Hardcore Heart
                drawIcon(gui, SPRITE_HARDCORE_HEART_HALF, heartX, heartY, iconSize, iconSize);
            }
        }

        // 4. 10 Hunger Drumsticks (Right of Hotbar, filling right-to-left)
        for (int hu = 0; hu < 10; hu++) {
            int drumX = hotbarX + hotbarW - iconSize - hu * iconSpacing;
            int drumY = statsY;

            // Low-hunger rumble jitter
            if (player.hunger <= 6 && (hu % 2 == 0)) {
                drumY += ((System.currentTimeMillis() / 100 + hu) % 2 == 0) ? -2 : 0;
            }

            int threshold = (hu + 1) * 2;

            // Always draw empty drumstick silhouette container
            drawIcon(gui, SPRITE_HUNGER_CONTAINER, drumX, drumY, iconSize, iconSize);

            if (player.hunger >= threshold) {
                // Full Roasted Minecraft Drumstick
                drawIcon(gui, SPRITE_HUNGER_FULL, drumX, drumY, iconSize, iconSize);
            } else if (player.hunger == threshold - 1) {
                // Half Roasted Drumstick
                drawIcon(gui, SPRITE_HUNGER_HALF, drumX, drumY, iconSize, iconSize);
            }
        }

        // 5. Survival HUD Stats (Top-Left)
        String posStr = String.format("XYZ: %.1f / %.1f / %.1f", player.pos.x, player.pos.y, player.pos.z);
        font.drawString(gui, posStr, 12, 12, 0.85f, 0xffffff, true);

        String modeStr = "Mode: HARDCORE | Biome: " + biomeName;
        font.drawString(gui, modeStr, 12, 32, 0.75f, 0xff5555, true);
    }

    private void drawBlockItemInSlot(GuiRenderer gui, int atlasTextureId, byte blockType, float slotX, float hotbarY, float slotSize) {
        if (blockType == Block.AIR) return;

        if (blockType == Item.IRON_AXE) {
            // Render 2D Pixel-Art Iron Axe Sprite centered in slot
            float iconSize = 26;
            float ix = slotX + (slotSize - 2 - iconSize) / 2.0f;
            float iy = hotbarY + (slotSize - 2 - iconSize) / 2.0f;
            drawIcon(gui, SPRITE_IRON_AXE, ix, iy, iconSize, iconSize);
            return;
        }

        if (blockType == Item.IRON_SHOVEL) {
            // Render 2D Pixel-Art Iron Shovel Sprite centered in slot
            float iconSize = 26;
            float ix = slotX + (slotSize - 2 - iconSize) / 2.0f;
            float iy = hotbarY + (slotSize - 2 - iconSize) / 2.0f;
            drawIcon(gui, SPRITE_IRON_SHOVEL, ix, iy, iconSize, iconSize);
            return;
        }

        if (blockType == Item.IRON_PICKAXE) {
            // Render 2D Pixel-Art Iron Pickaxe Sprite centered in slot
            float iconSize = 26;
            float ix = slotX + (slotSize - 2 - iconSize) / 2.0f;
            float iy = hotbarY + (slotSize - 2 - iconSize) / 2.0f;
            drawIcon(gui, SPRITE_IRON_PICKAXE, ix, iy, iconSize, iconSize);
            return;
        }

        if (atlasTextureId == 0) return;

        if (Block.isPlant(blockType)) {
            // Flat 2D Flower / Tall Grass icon
            int tile = Block.getDisplayFaceTile(blockType);
            float[] uv = TextureAtlas.getTileUV(tile);
            float iconSize = 26;
            float ix = slotX + (slotSize - 2 - iconSize) / 2.0f;
            float iy = hotbarY + (slotSize - 2 - iconSize) / 2.0f;
            gui.drawTexturedQuad(atlasTextureId, ix, iy, iconSize, iconSize, uv[0], uv[3], uv[1], uv[2], 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            // Authentic Minecraft 3D Isometric Cube Item Icon
            float cx = slotX + (slotSize - 2) / 2.0f;
            float cy = hotbarY + (slotSize - 2) / 2.0f - 1.0f;
            float s = 11.5f; // half-width span

            // Top Face (Rhombus) - Full 1.0 Brightness
            int topSlot = Block.getBlockFaceSlot(blockType, 2);
            float[] topUV = TextureAtlas.getTileUV(topSlot);
            gui.drawTexturedQuadArbitrary(
                atlasTextureId,
                cx, cy - s * 0.95f, topUV[0], topUV[3],       // Top: (uMin, vMax)
                cx + s, cy - s * 0.40f, topUV[1], topUV[3],   // Right: (uMax, vMax)
                cx, cy + s * 0.15f, topUV[1], topUV[2],       // Bottom: (uMax, vMin)
                cx - s, cy - s * 0.40f, topUV[0], topUV[2],   // Left: (uMin, vMin)
                1.0f, 1.0f, 1.0f, 1.0f
            );

            // Left Face (West Side) - 0.70 Shading
            int leftSlot = Block.getBlockFaceSlot(blockType, 1);
            float[] leftUV = TextureAtlas.getTileUV(leftSlot);
            gui.drawTexturedQuadArbitrary(
                atlasTextureId,
                cx - s, cy - s * 0.40f, leftUV[0], leftUV[3], // Top-Left
                cx, cy + s * 0.15f, leftUV[1], leftUV[3],     // Top-Right
                cx, cy + s * 1.15f, leftUV[1], leftUV[2],     // Bottom-Right
                cx - s, cy + s * 0.60f, leftUV[0], leftUV[2], // Bottom-Left
                0.70f, 0.70f, 0.70f, 1.0f
            );

            // Right Face (South Side) - 0.85 Shading
            int rightSlot = Block.getBlockFaceSlot(blockType, 4);
            float[] rightUV = TextureAtlas.getTileUV(rightSlot);
            gui.drawTexturedQuadArbitrary(
                atlasTextureId,
                cx, cy + s * 0.15f, rightUV[0], rightUV[3],     // Top-Left
                cx + s, cy - s * 0.40f, rightUV[1], rightUV[3], // Top-Right
                cx + s, cy + s * 0.60f, rightUV[1], rightUV[2], // Bottom-Right
                cx, cy + s * 1.15f, rightUV[0], rightUV[2],     // Bottom-Left
                0.85f, 0.85f, 0.85f, 1.0f
            );
        }
    }

    public void cleanup() {
        if (iconTextureId != 0) {
            glDeleteTextures(iconTextureId);
            iconTextureId = 0;
        }
    }
}
