package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import com.mcjournal.client.WorldSaveManager;

public class WorldEditScreen extends Screen {
    private final WorldSaveManager.SavedWorld world;
    private String worldName;
    private float cursorTimer = 0.0f;

    public WorldEditScreen(MCJournalApp app, WorldSaveManager.SavedWorld world) {
        super(app);
        this.world = world;
        this.worldName = (world != null && world.name != null) ? world.name : "New Adventure";
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int totalBtnW = 400;
        int centerX = (width - totalBtnW) / 2;
        int btnW = (totalBtnW - 8) / 2; // 196px each
        int btnH = 38;
        int bottomY = height - 56;

        // 1. Save World Button (Left)
        buttons.add(new Button(1, "Save", centerX, bottomY, btnW, btnH, this::saveAndExit));

        // 2. Cancel Button (Right)
        buttons.add(new Button(2, "Cancel", centerX + btnW + 8, bottomY, btnW, btnH, () -> {
            app.setScreen(new WorldSelectScreen(app));
        }));
    }

    private void saveAndExit() {
        String finalName = worldName.trim().isEmpty() ? "New Adventure" : worldName.trim();
        WorldSaveManager.renameWorld(finalName);
        app.setScreen(new WorldSelectScreen(app));
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        cursorTimer += deltaTime * 2.0f;

        // 1. Dark translucent slate background with header/footer strips
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.40f);
        gui.drawMenuHeaderFooterStrips(width, height, 64, 82);

        // 2. Header Title
        String title = "Edit World";
        float titleW = font.getStringWidth(title, 1.20f);
        font.drawString(gui, title, (width - titleW) / 2.0f, 16, 1.20f, 0xffffff, true);

        // 3. Form Cards Container (500px wide, centered)
        int boxW = Math.min(500, width - 48);
        int centerX = (width - boxW) / 2;
        int startY = 96;

        // --- SECTION 1: World Name Input Box ---
        font.drawString(gui, "World Name", centerX, startY, 0.80f, 0xaaaaaa, true);
        int nameInputY = startY + 18;
        int nameInputH = 34;

        gui.drawRect(centerX - 1, nameInputY - 1, boxW + 2, nameInputH + 2, 0.65f, 0.65f, 0.65f, 1.0f);
        gui.drawRect(centerX, nameInputY, boxW, nameInputH, 0.0f, 0.0f, 0.0f, 1.0f);

        String displayName = worldName + ((int) cursorTimer % 2 == 0 ? "_" : "");
        float nameScale = 0.85f;
        float nameFontH = font.getFontHeight(nameScale);
        float nameTextY = nameInputY + (nameInputH - nameFontH) / 2.0f;
        font.drawString(gui, displayName, centerX + 10, nameTextY, nameScale, 0xffffff, false);

        // --- SECTION 2: World Info Card ---
        if (world != null) {
            int infoCardY = nameInputY + nameInputH + 16;
            int infoCardH = 56;

            gui.drawBevelBox(centerX, infoCardY, boxW, infoCardH, 0x18181c, 0x585860, 0x222226);
            font.drawString(gui, "⚔ Hardcore World (" + world.biome + ")", centerX + 14, infoCardY + 11, 0.84f, 0xdddddd, true);
            font.drawString(gui, "Seed: " + world.seed + " | Created: " + world.createdAt, centerX + 14, infoCardY + 31, 0.70f, 0xaaaaaa, false);
        }

        // 4. Render Action Buttons
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void charTyped(char character) {
        if (worldName.length() < 24 && character >= 32 && character <= 126) {
            worldName += character;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS || action == org.lwjgl.glfw.GLFW.GLFW_REPEAT) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !worldName.isEmpty()) {
                worldName = worldName.substring(0, worldName.length() - 1);
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                saveAndExit();
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                app.setScreen(new WorldSelectScreen(app));
            }
        }
    }
}
