package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Robust, production-grade single-line text input field supporting Unicode character entry,
 * caret navigation, word jumping, selection, clipboard operations (Ctrl+C/V/X/A),
 * placeholder text, filtering, horizontal scrolling, and cursor blinking.
 */
public class TextField extends Widget {
    private final int id;
    private String text = "";
    private String placeholder = "";
    private int maxLength = 32;
    private Predicate<String> filter = null;
    private Consumer<String> onTextChanged = null;
    private Runnable onEnterPressed = null;

    private int caretPos = 0;
    private int selectionAnchor = -1; // -1 means no selection
    private int displayOffset = 0;    // Horizontal scroll offset in character count

    // Cursor Blink (500ms cycle)
    private float blinkTimer = 0.0f;
    private boolean isCaretVisible = true;

    private boolean isMouseSelecting = false;
    private long windowHandle = 0;

    public TextField(int id, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.id = id;
    }

    public void setWindowHandle(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        this.text = (newText != null) ? newText : "";
        if (text.length() > maxLength) {
            this.text = text.substring(0, maxLength);
        }
        this.caretPos = Math.clamp(caretPos, 0, text.length());
        this.selectionAnchor = -1;
        this.displayOffset = 0;
        resetBlink();
        if (onTextChanged != null) {
            onTextChanged.accept(this.text);
        }
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = (placeholder != null) ? placeholder : "";
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        if (text.length() > this.maxLength) {
            setText(text.substring(0, this.maxLength));
        }
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    public void setOnTextChanged(Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    public void setOnEnterPressed(Runnable onEnterPressed) {
        this.onEnterPressed = onEnterPressed;
    }

    public int getCaretPosition() {
        return caretPos;
    }

    public void setCaretPosition(int pos) {
        this.caretPos = Math.clamp(pos, 0, text.length());
        this.selectionAnchor = -1;
        resetBlink();
    }

    public boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != caretPos;
    }

    public int getSelectionStart() {
        if (!hasSelection()) return caretPos;
        return Math.min(selectionAnchor, caretPos);
    }

    public int getSelectionEnd() {
        if (!hasSelection()) return caretPos;
        return Math.max(selectionAnchor, caretPos);
    }

    public String getSelectedText() {
        if (!hasSelection()) return "";
        return text.substring(getSelectionStart(), getSelectionEnd());
    }

    public void selectAll() {
        if (text.isEmpty()) return;
        this.selectionAnchor = 0;
        this.caretPos = text.length();
        resetBlink();
    }

    public void clearSelection() {
        this.selectionAnchor = -1;
    }

    public void deleteSelection() {
        if (!hasSelection()) return;
        int start = getSelectionStart();
        int end = getSelectionEnd();
        text = text.substring(0, start) + text.substring(end);
        caretPos = start;
        selectionAnchor = -1;
        clampDisplayOffset();
        resetBlink();
        if (onTextChanged != null) {
            onTextChanged.accept(text);
        }
    }

    public void insertText(String str) {
        if (str == null || str.isEmpty() || !isEnabled) return;

        if (hasSelection()) {
            deleteSelection();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch >= 32 && ch != 127) {
                sb.append(ch);
            }
        }
        String clean = sb.toString();

        if (filter != null) {
            String testMerged = text.substring(0, caretPos) + clean + text.substring(caretPos);
            if (!filter.test(testMerged)) {
                return;
            }
        }

        int available = maxLength - text.length();
        if (available <= 0) return;
        if (clean.length() > available) {
            clean = clean.substring(0, available);
        }

        text = text.substring(0, caretPos) + clean + text.substring(caretPos);
        caretPos += clean.length();
        selectionAnchor = -1;
        clampDisplayOffset();
        resetBlink();

        if (onTextChanged != null) {
            onTextChanged.accept(text);
        }
    }

    private void resetBlink() {
        this.blinkTimer = 0.0f;
        this.isCaretVisible = true;
    }

    private void clampDisplayOffset() {
        if (caretPos < displayOffset) {
            displayOffset = caretPos;
        }
    }

    private void ensureCaretVisible(FontRenderer font, float scale, float maxW) {
        if (caretPos < displayOffset) {
            displayOffset = caretPos;
        }

        while (displayOffset < caretPos) {
            String sub = text.substring(displayOffset, caretPos);
            if (font.getStringWidth(sub, scale) <= maxW) {
                break;
            }
            displayOffset++;
        }
    }

    private int findWordBoundary(int fromPos, int direction) {
        int pos = fromPos;
        if (direction < 0) {
            if (pos <= 0) return 0;
            pos--;
            while (pos > 0 && Character.isWhitespace(text.charAt(pos))) pos--;
            while (pos > 0 && !Character.isWhitespace(text.charAt(pos - 1))) pos--;
            return pos;
        } else {
            if (pos >= text.length()) return text.length();
            while (pos < text.length() && !Character.isWhitespace(text.charAt(pos))) pos++;
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
            return pos;
        }
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (!isVisible) return;

        // 1. Inset Background Box
        gui.drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 0.85f);
        int borderColor = isFocused ? 0xFFFFFF : (isHovered ? 0xA0A0A0 : 0x404040);
        float r = ((borderColor >> 16) & 0xFF) / 255.0f;
        float g = ((borderColor >> 8) & 0xFF) / 255.0f;
        float b = (borderColor & 0xFF) / 255.0f;

        gui.drawRect(x, y, width, 1, r, g, b, 1.0f);
        gui.drawRect(x, y + height - 1, width, 1, r, g, b, 1.0f);
        gui.drawRect(x, y, 1, height, r, g, b, 1.0f);
        gui.drawRect(x + width - 1, y, 1, height, r, g, b, 1.0f);

        // 2. Text layout parameters
        float paddingX = 4.0f;
        float contentW = width - paddingX * 2.0f;
        float scale = height >= 28 ? 0.65f : 0.50f;
        float fontH = font.getFontHeight(scale);
        float textY = y + (height - fontH) / 2.0f;

        ensureCaretVisible(font, scale, contentW);

        // 3. Render Placeholder if empty and not focused
        if (text.isEmpty() && !isFocused && !placeholder.isEmpty()) {
            font.drawString(gui, placeholder, x + paddingX, textY, scale, 0x777777, false);
            return;
        }

        // 4. Calculate Visible String Slice
        String visibleSlice = "";
        int sliceEnd = displayOffset;
        while (sliceEnd < text.length()) {
            String test = text.substring(displayOffset, sliceEnd + 1);
            if (font.getStringWidth(test, scale) > contentW) {
                break;
            }
            sliceEnd++;
        }
        visibleSlice = text.substring(displayOffset, sliceEnd);

        // 5. Render Selection Box (Behind Text)
        if (hasSelection()) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            int visibleSelStart = Math.max(displayOffset, selStart);
            int visibleSelEnd = Math.min(sliceEnd, selEnd);

            if (visibleSelStart < visibleSelEnd) {
                float startX = x + paddingX + font.getStringWidth(text.substring(displayOffset, visibleSelStart), scale);
                float endX = x + paddingX + font.getStringWidth(text.substring(displayOffset, visibleSelEnd), scale);
                gui.drawRect(startX, textY - 1, endX - startX, fontH + 2, 0.20f, 0.40f, 0.85f, 0.70f);
            }
        }

        // 6. Render Visible Text
        font.drawString(gui, visibleSlice, x + paddingX, textY, scale, isEnabled ? 0xE0E0E0 : 0x707070, true);

        // 7. Render Caret
        if (isFocused && isEnabled) {
            blinkTimer += deltaTime;
            if (blinkTimer >= 0.5f) {
                isCaretVisible = !isCaretVisible;
                blinkTimer = 0.0f;
            }

            if (isCaretVisible && caretPos >= displayOffset && caretPos <= sliceEnd) {
                float caretX = x + paddingX + font.getStringWidth(text.substring(displayOffset, caretPos), scale);
                gui.drawRect(caretX, textY - 1, 1.0f, fontH + 2, 1.0f, 1.0f, 1.0f, 0.90f);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible || !isEnabled) return false;

        if (isMouseOver(mouseX, mouseY) && button == 0) {
            setFocused(true);
            isMouseSelecting = true;
            // Approximate character position at click
            float paddingX = 4.0f;
            double relX = mouseX - (x + paddingX);
            int targetPos = displayOffset;
            float scale = height >= 28 ? 0.65f : 0.50f;
            // Place caret at end by default or approximate
            targetPos = text.length();
            setCaretPosition(targetPos);
            selectionAnchor = caretPos;
            return true;
        } else if (!isMouseOver(mouseX, mouseY)) {
            setFocused(false);
            selectionAnchor = -1;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseSelecting) {
            isMouseSelecting = false;
            if (selectionAnchor == caretPos) {
                selectionAnchor = -1;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint) {
        if (!isFocused || !isEnabled) return false;

        if (codePoint >= 32 && codePoint != 127) {
            insertText(String.valueOf(codePoint));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (!isFocused || !isEnabled || (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT)) {
            return false;
        }

        boolean isCtrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_A -> {
                if (isCtrl) {
                    selectAll();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (isCtrl) {
                    if (hasSelection()) {
                        ClipboardHelper.setClipboard(windowHandle, getSelectedText());
                    }
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (isCtrl) {
                    String clip = ClipboardHelper.getClipboard(windowHandle);
                    if (clip != null && !clip.isEmpty()) {
                        insertText(clip);
                    }
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (isCtrl) {
                    if (hasSelection()) {
                        ClipboardHelper.setClipboard(windowHandle, getSelectedText());
                        deleteSelection();
                    }
                    return true;
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caretPos > 0) {
                    if (isCtrl) {
                        int wordStart = findWordBoundary(caretPos, -1);
                        text = text.substring(0, wordStart) + text.substring(caretPos);
                        caretPos = wordStart;
                    } else {
                        text = text.substring(0, caretPos - 1) + text.substring(caretPos);
                        caretPos--;
                    }
                    clampDisplayOffset();
                    resetBlink();
                    if (onTextChanged != null) onTextChanged.accept(text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (caretPos < text.length()) {
                    if (isCtrl) {
                        int wordEnd = findWordBoundary(caretPos, 1);
                        text = text.substring(0, caretPos) + text.substring(wordEnd);
                    } else {
                        text = text.substring(0, caretPos) + text.substring(caretPos + 1);
                    }
                    resetBlink();
                    if (onTextChanged != null) onTextChanged.accept(text);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                int target = isCtrl ? findWordBoundary(caretPos, -1) : Math.max(0, caretPos - 1);
                if (isShift) {
                    if (selectionAnchor == -1) selectionAnchor = caretPos;
                } else {
                    selectionAnchor = -1;
                }
                caretPos = target;
                clampDisplayOffset();
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int target = isCtrl ? findWordBoundary(caretPos, 1) : Math.min(text.length(), caretPos + 1);
                if (isShift) {
                    if (selectionAnchor == -1) selectionAnchor = caretPos;
                } else {
                    selectionAnchor = -1;
                }
                caretPos = target;
                clampDisplayOffset();
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                if (isShift) {
                    if (selectionAnchor == -1) selectionAnchor = caretPos;
                } else {
                    selectionAnchor = -1;
                }
                caretPos = 0;
                displayOffset = 0;
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                if (isShift) {
                    if (selectionAnchor == -1) selectionAnchor = caretPos;
                } else {
                    selectionAnchor = -1;
                }
                caretPos = text.length();
                resetBlink();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (onEnterPressed != null) {
                    onEnterPressed.run();
                }
                setFocused(false);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                setFocused(false);
                return true;
            }
        }

        // Consume any remaining key while focused so it never leaks into game controls
        return true;
    }

    public int getId() {
        return id;
    }
}
