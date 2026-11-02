package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable cycle button widget that cycles through a predefined list of options.
 * Supports forward cycling (Left-click / Enter / Space / Right-arrow) and
 * reverse cycling (Right-click / Left-arrow / Shift+click).
 */
public class CycleButton<T> extends Widget {
    private final int id;
    private final String prefix;
    private final List<T> options = new ArrayList<>();
    private final Function<T, String> formatter;
    private final Consumer<T> onValueChanged;

    private int currentIndex = 0;

    public CycleButton(int id, String prefix, float x, float y, float width, float height,
                       List<T> options, int initialIndex,
                       Function<T, String> formatter, Consumer<T> onValueChanged) {
        super(x, y, width, height);
        this.id = id;
        this.prefix = (prefix != null) ? prefix : "";
        if (options != null && !options.isEmpty()) {
            this.options.addAll(options);
        }
        this.formatter = (formatter != null) ? formatter : String::valueOf;
        this.onValueChanged = onValueChanged;
        this.currentIndex = Math.clamp(initialIndex, 0, Math.max(0, this.options.size() - 1));
    }

    public void cycle(int direction) {
        if (!isEnabled || options.isEmpty()) return;
        int size = options.size();
        currentIndex = (currentIndex + direction) % size;
        if (currentIndex < 0) {
            currentIndex += size;
        }
        T selected = getSelectedValue();
        if (onValueChanged != null) {
            onValueChanged.accept(selected);
        }
    }

    public T getSelectedValue() {
        if (options.isEmpty()) return null;
        return options.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        if (options.isEmpty()) return;
        this.currentIndex = Math.clamp(index, 0, options.size() - 1);
    }

    public String getDisplayString() {
        T val = getSelectedValue();
        String valStr = (val != null) ? formatter.apply(val) : "None";
        return prefix + valStr;
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (!isVisible) return;

        gui.drawMinecraftButton(x, y, width, height, isHovered, isEnabled);

        // Focus Highlight Border
        if (isFocused && isEnabled) {
            gui.drawRect(x - 1, y - 1, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y + height, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x + width, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
        }

        String label = getDisplayString();
        float maxAllowedW = width - 14.0f;
        float desiredScale = height >= 38 ? 0.88f : (height >= 28 ? 0.65f : 0.50f);
        float textW = font.getStringWidth(label, desiredScale);
        float scale = desiredScale;
        if (textW > maxAllowedW && textW > 0) {
            scale = desiredScale * (maxAllowedW / textW);
            textW = font.getStringWidth(label, scale);
        }

        float textX = x + (width - textW) / 2.0f;
        float fontH = font.getFontHeight(scale);
        float textY = y + (height - fontH) / 2.0f;

        int textColor = !isEnabled ? 0xa0a0a0 : (isHovered ? 0xffffa0 : 0xe0e0e0);
        font.drawString(gui, label, textX, textY, scale, textColor, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled || !isHovered) return false;

        if (button == 0) {
            // Left click: forward cycle
            cycle(1);
            return true;
        } else if (button == 1) {
            // Right click: reverse cycle
            cycle(-1);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (!isFocused || !isEnabled || (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT)) {
            return false;
        }

        boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            cycle(isShift ? -1 : 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            cycle(1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            cycle(-1);
            return true;
        }

        return false;
    }

    public int getId() {
        return id;
    }
}
