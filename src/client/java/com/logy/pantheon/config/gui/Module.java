package com.logy.pantheon.config.gui;

import com.logy.pantheon.config.gui.util.ColorUtil;
import com.logy.pantheon.config.gui.widgets.SettingWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Module {
    public static final int HEADER_HEIGHT = 18;
    public static final int INDENT = 4;

    public final String name;
    private final List<SettingWidget<?>> widgets = new ArrayList<>();
    private boolean expanded = false;
    private final Supplier<Boolean> toggleGetter;
    private final Consumer<Boolean> toggleSetter;
    private boolean internalActive = false;

    public Module(String name) {
        this.name = name;
        this.toggleGetter = null;
        this.toggleSetter = null;
    }

    public Module(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.name = name;
        this.toggleGetter = getter;
        this.toggleSetter = setter;
    }

    public Module add(SettingWidget<?> widget) {
        widgets.add(widget);
        return this;
    }

    private boolean isActive() {
        return toggleGetter != null ? toggleGetter.get() : internalActive;
    }

    private void setActive(boolean v) {
        if (toggleSetter != null) toggleSetter.accept(v);
        else internalActive = v;
    }

    public float getHeight() {
        if (!expanded) return HEADER_HEIGHT;
        float h = HEADER_HEIGHT;
        for (SettingWidget<?> w : widgets) h += w.getHeight();
        return h;
    }

    public void render(GuiGraphicsExtractor g, Font font, int x, int y, float mouseX, float mouseY) {
        int bgColor = isActive() ? ColorUtil.HIGHLIGHT : ColorUtil.rgba(45, 45, 45, 255);
        g.fill(x, y, x + Category.WIDTH, y + HEADER_HEIGHT, bgColor);
        g.text(font, name, x + INDENT, y + 5, ColorUtil.WHITE);

        if (expanded) {
            float drawY = y + HEADER_HEIGHT;
            for (SettingWidget<?> w : widgets) {
                w.render(g, font, x + INDENT, drawY, mouseX, mouseY);
                drawY += w.getHeight();
            }
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int x, int y, MouseButtonEvent event) {
        if (mouseX >= x && mouseX <= x + Category.WIDTH &&
            mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            if (event.button() == 0) {
                setActive(!isActive());
                return true;
            } else if (event.button() == 1) {
                expanded = !expanded;
                return true;
            }
        }

        if (expanded) {
            float drawY = y + HEADER_HEIGHT;
            for (SettingWidget<?> w : widgets) {
                if (mouseY >= drawY && mouseY <= drawY + w.getHeight()) {
                    if (w.mouseClicked(mouseX, mouseY, event)) return true;
                }
                drawY += w.getHeight();
            }
        }
        return false;
    }

    public void mouseReleased(MouseButtonEvent event) {
        for (SettingWidget<?> w : widgets) w.mouseReleased(event);
    }

    public boolean charTyped(CharacterEvent event) {
        if (!expanded) return false;
        for (SettingWidget<?> w : widgets) if (w.keyTyped(event)) return true;
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!expanded) return false;
        for (SettingWidget<?> w : widgets) if (w.keyPressed(event)) return true;
        return false;
    }
}
