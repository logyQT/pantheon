package com.logy.pantheon.config.gui.widgets;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SliderWidget extends SettingWidget<Integer> {
    private final int min, max, step;
    private boolean isDragging = false;
    private String displayValue = "";

    public SliderWidget(String name, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(name, getter, setter);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, float x, float y, float mouseX, float mouseY) {
        lastX = x;
        lastY = y;
        int height = (int) getHeight();
        int value = getter.get();
        float fraction = (float) (value - min) / (max - min);
        String display = String.valueOf(value);
        int sliderY = (int) y + 16;
        int sliderX = (int) x + 4;
        int sliderW = Category.WIDTH - 8;
        int sliderH = 6;

        g.text(font, name, (int) x + 4, (int) y + 4, ColorUtil.WHITE);
        g.text(font, display, (int) x + Category.WIDTH - 6 - font.width(display), (int) y + 4, ColorUtil.LIGHT_GRAY);

        g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, ColorUtil.rgba(38, 38, 38, 255));
        int fillW = (int) (fraction * sliderW);
        if (fillW > 0) g.fill(sliderX, sliderY, sliderX + fillW, sliderY + sliderH, ColorUtil.HIGHLIGHT);
        int handleX = sliderX + (int) (fraction * sliderW) - 2;
        g.fill(handleX, sliderY - 2, handleX + 4, sliderY + sliderH + 2, ColorUtil.WHITE);

        if (listening) {
            double newFraction = Math.max(0, Math.min(1, (mouseX - sliderX) / (double) sliderW));
            int newValue = Math.round((float) (min + newFraction * (max - min)) / step) * step;
            newValue = Math.max(min, Math.min(max, newValue));
            setter.accept(newValue);
        }
    }

    @Override
    public float getHeight() {
        return 26;
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, MouseButtonEvent event) {
        if (event.button() != 0) return false;
        int sliderX = (int) lastX + 4;
        int sliderY = (int) lastY + 16;
        int sliderW = Category.WIDTH - 8;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= sliderY && mouseY <= sliderY + 6) {
            listening = true;
            isDragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(MouseButtonEvent event) {
        listening = false;
        isDragging = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isHovered()) return false;
        int value = getter.get();
        int newValue = value;
        switch (event.key()) {
            case 262 -> newValue = Math.min(max, value + step); // right
            case 263 -> newValue = Math.max(min, value - step); // left
            default -> { return false; }
        }
        setter.accept(newValue);
        return true;
    }

    private boolean isHovered() {
        return false; // simplified; mouseX/Y tracking would need screen-level state
    }
}
