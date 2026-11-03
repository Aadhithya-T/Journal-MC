package com.mcjournal.client.gui;

import com.mcjournal.client.MCJournalApp;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
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

        // Content column and button row matching WorldCreateScreen & WorldEditScreen
        GuiLayout.Column col = GuiLayout.contentColumn(width, 320, GuiLayout.MARGIN);
        int btnH = GuiLayout.ROW_HEIGHT;
        int btnY = height - GuiLayout.BOTTOM_MARGIN;
        GuiLayout.ButtonSlot[] slots = GuiLayout.distributeRow(col.x, col.width, 2, 8);

        // 1. Confirm Button
        Button confirmBtn = new Button(1, confirmText, slots[0].x(), btnY, slots[0].width(), btnH, this::handleConfirm);
        addWidget(confirmBtn);

        // 2. Cancel Button
        Button cancelBtn = new Button(2, cancelText, slots[1].x(), btnY, slots[1].width(), btnH, this::handleCancel);
        addWidget(cancelBtn);

        // Focus cancel by default in dangerMode for safety; otherwise confirm
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

    private List<String> getWrappedLines(FontRenderer font, float maxLineWidth) {
        List<String> wrapped = new ArrayList<>();
        if (message == null || message.isEmpty()) return wrapped;
        String[] rawLines = message.split("\n", -1);
        for (String raw : rawLines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                wrapped.add("");
                continue;
            }
            float w = font != null ? font.getStringWidth(trimmed, 1.0f) : (trimmed.length() * 6.0f);
            if (w <= maxLineWidth) {
                wrapped.add(trimmed);
                continue;
            }
            // Word wrap at natural word boundaries
            String[] words = trimmed.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() == 0) {
                    sb.append(word);
                } else {
                    String test = sb + " " + word;
                    float testW = font != null ? font.getStringWidth(test, 1.0f) : (test.length() * 6.0f);
                    if (testW <= maxLineWidth) {
                        sb.append(" ").append(word);
                    } else {
                        wrapped.add(sb.toString());
                        sb = new StringBuilder(word);
                    }
                }
            }
            if (sb.length() > 0) {
                wrapped.add(sb.toString());
            }
        }
        return wrapped;
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        // 1. Background overlay & menu header/footer strips (consistent with all menu screens)
        gui.drawRect(0, 0, width, height, 0.0f, 0.0f, 0.0f, 0.45f);
        gui.drawMenuHeaderFooterStrips(width, height, 36, 44);

        // 2. Header Title
        font.drawCentered(gui, title, width / 2.0f, GuiLayout.TITLE_Y, TextStyle.TITLE);

        // 3. Compute dynamic wrapped lines & card geometry
        float maxAllowedTextW = Math.max(260.0f, width - 64.0f);
        List<String> lines = getWrappedLines(font, Math.min(360.0f, maxAllowedTextW));

        float maxLineW = 0.0f;
        for (String line : lines) {
            if (!line.isEmpty()) {
                maxLineW = Math.max(maxLineW, font.getStringWidth(line, 1.0f));
            }
        }

        int padX = 24;
        int cardW = Math.min(width - 24, Math.max(300, (int) Math.ceil(maxLineW) + padX * 2));
        int cardX = (width - cardW) / 2;

        int bodyH = (int) Math.round(font.getLineHeight(TextStyle.BODY));
        int padY = 12;
        int lineGap = 6;
        int cardH = padY * 2 + lines.size() * bodyH + Math.max(0, lines.size() - 1) * lineGap;
        int cardY = 36 + Math.max(8, (height - 80 - cardH) / 2);

        gui.drawBevelBox(cardX, cardY, cardW, cardH, 0x18181c, 0x484852, 0x202024);

        // 4. Message Lines
        int textY = cardY + padY;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isEmpty()) {
                if (line.startsWith("'") && (line.endsWith("'") || line.endsWith("'?") || line.contains("'"))) {
                    // Highlighted world / target entity name in soft gold
                    font.drawCentered(gui, line, width / 2.0f, textY, TextStyle.BODY.withColor(0xffe066));
                } else if (i == 0) {
                    // Primary prompt / question in clean white
                    font.drawCentered(gui, line, width / 2.0f, textY, TextStyle.BODY);
                } else if (line.toLowerCase().contains("undone") || line.toLowerCase().contains("erase") || dangerMode) {
                    // Warning / consequence details in soft danger red
                    font.drawCentered(gui, line, width / 2.0f, textY, TextStyle.CAPTION.withColor(dangerMode ? 0xff7777 : 0xaaaaaa));
                } else {
                    font.drawCentered(gui, line, width / 2.0f, textY, TextStyle.BODY.withColor(0xdddddd));
                }
            }
            textY += bodyH + lineGap;
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
