package com.mcjournal.client.gui;

public class Button {
    private final int id;
    private String text;
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean isHovered;
    private final Runnable onClick;

    public Button(int id, String text, float x, float y, float width, float height, Runnable onClick) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }

    public void update(double mouseX, double mouseY) {
        this.isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void render(GuiRenderer gui, FontRenderer font) {
        int bg = isHovered ? 0x6e6e6e : 0x4a4a4a;
        int borderLight = isHovered ? 0xffffff : 0x8e8e8e;
        int borderDark = isHovered ? 0x333333 : 0x222222;

        gui.drawBevelBox(x, y, width, height, bg, borderLight, borderDark);

        float textW = font.getStringWidth(text, 1.0f);
        float textX = x + (width - textW) / 2.0f;
        float textY = y + (height - 18) / 2.0f;

        int textColor = isHovered ? 0xffffa0 : 0xe0e0e0;
        font.drawString(gui, text, textX, textY, 1.0f, textColor, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
