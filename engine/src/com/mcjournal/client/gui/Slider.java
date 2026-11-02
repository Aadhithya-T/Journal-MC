package com.mcjournal.client.gui;

import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.Function;

/**
 * Reusable slider widget supporting continuous or stepped values, mouse dragging,
 * keyboard adjustments, and precise rounding without floating-point accumulation drift.
 */
public class Slider extends Widget {
    private final int id;
    private final String prefix;
    private final double min;
    private final double max;
    private final double step;
    private double value;

    private final Function<Double, String> formatter;
    private final DoubleConsumer onValueChanged;

    private boolean isDragging = false;

    public Slider(int id, String prefix, float x, float y, float width, float height,
                  double min, double max, double step, double initialValue,
                  Function<Double, String> formatter, DoubleConsumer onValueChanged) {
        super(x, y, width, height);
        this.id = id;
        this.prefix = (prefix != null) ? prefix : "";
        this.min = min;
        this.max = Math.max(min, max);
        this.step = (step > 0) ? step : 1.0;
        this.formatter = (formatter != null) ? formatter : (v -> String.format("%.0f", v));
        this.onValueChanged = onValueChanged;
        setValue(initialValue, false);
    }

    public void setValue(double newValue) {
        setValue(newValue, true);
    }

    public void setValue(double newValue, boolean notify) {
        double clamped = Math.clamp(newValue, min, max);
        if (step > 0) {
            double steps = Math.round((clamped - min) / step);
            clamped = min + steps * step;
            // Round to 4 decimal places to prevent floating point inaccuracies
            clamped = Math.round(clamped * 10000.0) / 10000.0;
            clamped = Math.clamp(clamped, min, max);
        }

        if (Double.compare(this.value, clamped) != 0) {
            this.value = clamped;
            if (notify && onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }

    public double getValue() {
        return value;
    }

    public double getRatio() {
        if (max <= min) return 0.0;
        return (value - min) / (max - min);
    }

    private void updateFromMouse(double mouseX) {
        if (!isEnabled || width <= 0) return;
        double knobWidth = 8.0;
        double trackWidth = width - knobWidth;
        double relX = Math.clamp(mouseX - (x + knobWidth / 2.0), 0.0, trackWidth);
        double ratio = relX / trackWidth;
        double targetVal = min + ratio * (max - min);
        setValue(targetVal, true);
    }

    @Override
    public void render(GuiRenderer gui, FontRenderer font, double mouseX, double mouseY, float deltaTime) {
        if (!isVisible) return;

        // 1. Draw Slider Track (Darkened recessed bevel box)
        gui.drawBevelBox(x, y, width, height, 0x181818, 0x080808, 0x303030);

        // 2. Focus Outline
        if (isFocused && isEnabled) {
            gui.drawRect(x - 1, y - 1, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y + height, width + 2, 1, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x - 1, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
            gui.drawRect(x + width, y, 1, height, 1.0f, 1.0f, 1.0f, 0.85f);
        }

        // 3. Draw Slider Knob (Minecraft button styled knob)
        float knobWidth = 8.0f;
        float trackWidth = width - knobWidth;
        float knobX = x + (float) (getRatio() * trackWidth);
        boolean knobHovered = isHovered || isDragging;
        gui.drawMinecraftButton(knobX, y, knobWidth, height, knobHovered, isEnabled);

        // 4. Centered Display Label
        String label = prefix + formatter.apply(value);
        float scale = height >= 28 ? 0.65f : 0.50f;
        float textW = font.getStringWidth(label, scale);
        float textX = x + (width - textW) / 2.0f;
        float textY = y + (height - font.getFontHeight(scale)) / 2.0f;

        int textColor = !isEnabled ? 0xa0a0a0 : (isHovered ? 0xffffa0 : 0xe0e0e0);
        font.drawString(gui, label, textX, textY, scale, textColor, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isEnabled && isHovered) {
            isDragging = true;
            updateFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDragging && isEnabled) {
            updateFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int action, int mods) {
        if (!isFocused || !isEnabled || (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT)) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            setValue(value - step, true);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            setValue(value + step, true);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            setValue(min, true);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            setValue(max, true);
            return true;
        }
        return false;
    }

    public int getId() {
        return id;
    }
}
