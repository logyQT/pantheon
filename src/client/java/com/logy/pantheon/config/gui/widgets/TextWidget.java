package com.logy.pantheon.config.gui.widgets;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextWidget extends SettingWidget<String> {
    private boolean focused = false;
    private final StringBuilder text = new StringBuilder();
    private int cursorPos = 0;

    public TextWidget(String name, Supplier<String> getter, Consumer<String> setter) {
        super(name, getter, setter);
        text.append(getter.get());
        cursorPos = text.length();
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, float x, float y, float mouseX, float mouseY) {
        lastX = x;
        lastY = y;
        int height = (int) getHeight();

        g.text(font, name, (int) x + 4, (int) y + 3, ColorUtil.WHITE);

        int fieldX = (int) x + 4;
        int fieldY = (int) y + 14;
        int fieldW = Category.WIDTH - 8;
        int fieldH = 14;

        g.fill(fieldX, fieldY, fieldX + fieldW, fieldY + fieldH, ColorUtil.rgba(30, 30, 30, 255));
        g.outline(fieldX, fieldY, fieldW, fieldH, focused ? ColorUtil.HIGHLIGHT : ColorUtil.DARK_GRAY);

        String display = text.toString();
        g.text(font, display, fieldX + 3, fieldY + 3, ColorUtil.WHITE);

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = fieldX + 3 + font.width(display.substring(0, Math.min(cursorPos, display.length())));
            g.verticalLine(caretX, fieldY + 2, fieldY + fieldH - 2, ColorUtil.WHITE);
        }
    }

    @Override
    public float getHeight() {
        return 30;
    }

    @Override
    public boolean mouseClicked(float mouseX, float mouseY, MouseButtonEvent event) {
        if (event.button() != 0) return false;
        int fieldX = (int) lastX + 4;
        int fieldY = (int) lastY + 14;
        int fieldW = Category.WIDTH - 8;
        focused = mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= fieldY && mouseY <= fieldY + 14;
        if (focused) {
            cursorPos = text.length();
        }
        return focused;
    }

    @Override
    public boolean keyTyped(CharacterEvent event) {
        if (!focused) return false;
        char ch = (char) event.codepoint();
        if (ch >= 32 && ch < 127) {
            text.insert(cursorPos, ch);
            cursorPos++;
            setter.accept(text.toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!focused) return false;
        switch (event.key()) {
            case 259 -> { // backspace
                if (cursorPos > 0) {
                    text.deleteCharAt(cursorPos - 1);
                    cursorPos--;
                    setter.accept(text.toString());
                }
                return true;
            }
            case 261 -> { // delete
                if (cursorPos < text.length()) {
                    text.deleteCharAt(cursorPos);
                    setter.accept(text.toString());
                }
                return true;
            }
            case 263 -> { // left arrow
                if (cursorPos > 0) cursorPos--;
                return true;
            }
            case 262 -> { // right arrow
                if (cursorPos < text.length()) cursorPos++;
                return true;
            }
            case 257, 335 -> { // enter
                focused = false;
                return true;
            }
        }
        return false;
    }
}
