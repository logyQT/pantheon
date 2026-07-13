package com.logy.pantheon.config.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class SettingWidget<T> {
    protected final String name;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    protected float lastX, lastY;
    protected boolean listening;

    public SettingWidget(String name, Supplier<T> getter, Consumer<T> setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
    }

    public abstract void render(GuiGraphicsExtractor g, Font font, float x, float y, float mouseX, float mouseY);
    public abstract float getHeight();

    public boolean mouseClicked(float mouseX, float mouseY, MouseButtonEvent event) { return false; }
    public void mouseReleased(MouseButtonEvent event) {}
    public boolean keyTyped(CharacterEvent event) { return false; }
    public boolean keyPressed(KeyEvent event) { return false; }
}
