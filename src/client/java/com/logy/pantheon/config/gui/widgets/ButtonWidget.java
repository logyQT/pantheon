package com.logy.pantheon.config.gui.widgets;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class ButtonWidget extends SettingWidget<Boolean> {

    private final Runnable action;
    private boolean hovered;

    public ButtonWidget(String name, Runnable action) {
        super(name, () -> false, v -> action.run());
        this.action = action;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, float x, float y, float mouseX, float mouseY) {
        lastX = x;
        lastY = y;
        hovered = mouseX >= x && mouseX <= x + Category.WIDTH && mouseY >= y && mouseY <= y + getHeight();
        int bg = hovered ? ColorUtil.rgba(60, 60, 60, 255) : ColorUtil.rgba(45, 45, 45, 255);
        g.fill((int) x, (int) y, (int) x + Category.WIDTH, (int) y + (int) getHeight(), bg);
        g.text(font, name, (int) x + 4, (int) y + 5, hovered ? ColorUtil.HIGHLIGHT : ColorUtil.LIGHT_GRAY);
    }

    @Override
    public float getHeight() {
        return 18;
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, MouseButtonEvent event) {
        if (event.button() != 0) return false;
        if (mouseX >= lastX && mouseX <= lastX + Category.WIDTH && mouseY >= lastY && mouseY <= lastY + getHeight()) {
            action.run();
            return true;
        }
        return false;
    }
}
