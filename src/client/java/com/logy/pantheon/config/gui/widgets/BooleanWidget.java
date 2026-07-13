package com.logy.pantheon.config.gui.widgets;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanWidget extends SettingWidget<Boolean> {

    public BooleanWidget(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(name, getter, setter);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, float x, float y, float mouseX, float mouseY) {
        lastX = x;
        lastY = y;
        int height = (int) getHeight();
        int toggleW = 28;
        int toggleH = 14;
        int toggleX = (int) x + Category.WIDTH - toggleW - 8;
        int toggleY = (int) y + 2;
        boolean value = getter.get();

        g.text(font, name, (int) x + 4, (int) y + 5, ColorUtil.WHITE);

        int bgColor = ColorUtil.rgba(38, 38, 38, 255);
        int fillColor = value ? ColorUtil.HIGHLIGHT : ColorUtil.DARK_GRAY;
        g.outline(toggleX, toggleY, toggleW, toggleH, ColorUtil.DARK_GRAY);
        g.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgColor);
        g.fill(toggleX + 2, toggleY + 2, toggleX + (value ? toggleW - 2 : toggleW / 2), toggleY + toggleH - 2, fillColor);
    }

    @Override
    public float getHeight() {
        return 18;
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, MouseButtonEvent event) {
        if (event.button() != 0) return false;
        int toggleW = 28;
        int toggleH = 14;
        int toggleX = (int) lastX + Category.WIDTH - toggleW - 8;
        int toggleY = (int) lastY + 2;
        if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH) {
            setter.accept(!getter.get());
            return true;
        }
        return false;
    }
}
