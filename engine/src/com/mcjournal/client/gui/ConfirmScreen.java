package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Modal confirmation dialog widget screen.
 * Used for irreversible actions such as world deletion, quitting without saving, etc.
 * Supports logical coordinates, keyboard navigation (Enter = Confirm, Escape = Cancel),
 * multi-line prompt messages, and optional danger styling.
 */
public class ConfirmScreen extends Screen {
    private final String title;
    private final String message;
    private final String confirmText;
    private final String cancelText;
    private final boolean dangerMode;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmScreen(MCJournalApp app, String title, String message, Runnable onConfirm, Runnable onCancel) {
        this(app, title, message, "Confirm", "Cancel", false, onConfirm, onCancel);
    }

    public ConfirmScreen(MCJournalApp app, String title, String message, String confirmText, String cancelText,
                         Runnable onConfirm, Runnable onCancel) {
        this(app, title, message, confirmText, cancelText, false, onConfirm, onCancel);
    }

    public ConfirmScreen(MCJournalApp app, String title, String message, String confirmText, String cancelText,
                         boolean dangerMode, Runnable onConfirm, Runnable onCancel) {
        super(app);
        this.title = title != null ? title : "Are you sure?";
        this.message = message != null ? message : "";
        this.confirmText = confirmText != null ? confirmText : "Confirm";
        this.cancelText = cancelText != null ? cancelText : "Cancel";
        this.dangerMode = dangerMode;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public ConfirmScreen(MCJournalApp app, String title, String message, Consumer<Boolean> callback) {
        this(app, title, message, "Confirm", "Cancel", false,
                () -> { if (callback != null) callback.accept(true); },
                () -> { if (callback != null) callback.accept(false); });
    }

    public ConfirmScreen(MCJournalApp app, String title, String message, boolean dangerMode, Consumer<Boolean> callback) {
        this(app, title, message, dangerMode ? "Delete" : "Confirm", "Cancel", dangerMode,
                () -> { if (callback != null) callback.accept(true); },
                () -> { if (callback != null) callback.accept(false); });
    }

    @Override
    public boolean usesLogicalCoordinates() {
        return true;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isDangerMode() {
        return dangerMode;
    }

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        int dialogW = Math.min(320, width - 24);
        int dialogH = 140;
        int dialogX = GuiLayout.centerX(width, dialogW);
        int dialogY = GuiLayout.centerY(height, dialogH);

        int btnGap = 8;
        int btnW = (dialogW - 24 - btnGap) / 2;
        int btnH = 20;
        int btnY = dialogY + dialogH - btnH - 12;
        int confirmX = dialogX + 12;
        int cancelX = confirmX + btnW + btnGap;

        // 1. Confirm Button
        Button confirmBtn = new Button(1, confirmText, confirmX, btnY, btnW, btnH, this::handleConfirm);
        addWidget(confirmBtn);

        // 2. Cancel Button
        Button cancelBtn = new Button(2, cancelText, cancelX, btnY, btnW, btnH, this::handleCancel);
        addWidget(cancelBtn);

        // Focus confirm by default unless dangerMode, where Cancel is focused for safety
        if (dangerMode) {
            focusManager.setFocus(cancelBtn);
        } else {
            focusManager.setFocus(confirmBtn);
        }
    }

    public void handleConfirm() {
        if (onConfirm != null) {
            onConfirm.run();
        } else if (app != null) {
            app.popScreen();
        }
    }

    public void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        } else if (app != null) {
            app.popScreen();
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Semi-transparent dark overlay darkening the underlying screen
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.70f);

        // 2. Centered Modal Dialog Card
        int dialogW = Math.min(320, width - 24);
        int dialogH = 140;
        int dialogX = GuiLayout.centerX(width, dialogW);
        int dialogY = GuiLayout.centerY(height, dialogH);

        // Dialog background panel with beveled edge
        int borderLight = dangerMode ? 0x882222 : 0x555560;
        int borderDark = dangerMode ? 0x441111 : 0x222226;
        gui.drawBevelBox(dialogX, dialogY, dialogW, dialogH, 0x18181c, borderLight, borderDark);

        // Top accent line if danger
        if (dangerMode) {
            gui.drawRect(dialogX + 2, dialogY + 2, dialogW - 4, 2, 0.85f, 0.15f, 0.15f, 1.0f);
        }

        // 3. Title Text
        int titleColor = dangerMode ? 0xff5555 : 0xffffff;
        float titleScale = 0.80f;
        float titleW = font.getStringWidth(title, titleScale);
        font.drawString(gui, title, GuiLayout.centerX(width, (int) titleW), dialogY + 14, titleScale, titleColor, true);

        // 4. Message Text (Supports multi-line \n)
        String[] lines = message.split("\n");
        float lineY = dialogY + 36;
        float textScale = 0.58f;
        for (String line : lines) {
            float lineW = font.getStringWidth(line, textScale);
            font.drawString(gui, line, GuiLayout.centerX(width, (int) lineW), lineY, textScale, 0xcccccc, false);
            lineY += font.getFontHeight(textScale) + 3;
        }

        // 5. Render Buttons and Tooltips
        super.render(gui, font, mouseX, mouseY, deltaTime);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                // If focused widget is a button, deliver to it; otherwise handleConfirm
                Widget focused = focusManager.getFocusedWidget();
                if (focused instanceof Button btn) {
                    btn.activate();
                } else {
                    handleConfirm();
                }
                return;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                handleCancel();
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, action, mods);
    }
}
