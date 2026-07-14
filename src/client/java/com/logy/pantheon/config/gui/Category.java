package com.logy.pantheon.config.gui;

import com.logy.pantheon.config.gui.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public class Category {
    public static final int WIDTH = 170;
    public static final int HEADER_HEIGHT = 20;

    public int x, y;
    public final String name;
    private final List<Module> modules = new ArrayList<>();
    private boolean dragging = false;
    private int dragOffX, dragOffY;
    private float scrollOffset = 0;

    public Category(String name, int startX, int startY) {
        this.name = name;
        this.x = startX;
        this.y = startY;
    }

    public Category add(Module module) {
        modules.add(module);
        return this;
    }

    public boolean isEmpty() {
        return modules.isEmpty();
    }

    private float contentHeight() {
        float h = HEADER_HEIGHT;
        for (Module m : modules) h += m.getHeight();
        return h;
    }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        float ch = contentHeight();
        int panelH = Math.max(HEADER_HEIGHT, (int) ch);

        g.fill(x, y, x + WIDTH, y + panelH, ColorUtil.rgba(26, 26, 26, 230));
        g.outline(x, y, WIDTH, panelH, ColorUtil.DARK_GRAY);
        g.fill(x, y, x + WIDTH, y + HEADER_HEIGHT, ColorUtil.rgba(40, 40, 40, 255));
        g.centeredText(font, name, x + WIDTH / 2, y + 6, ColorUtil.WHITE);

        if (scrollOffset > 0) scrollOffset = 0;

        g.enableScissor(x, y + HEADER_HEIGHT, x + WIDTH, y + panelH);
        float drawY = y + HEADER_HEIGHT + scrollOffset;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            m.render(g, font, x, (int) drawY, mouseX, mouseY);
            drawY += m.getHeight();

            if (i < modules.size() - 1) {
                g.fill(x, (int) drawY, x + WIDTH, (int) drawY + 1, ColorUtil.DARK_GRAY);
            }
        }
        g.disableScissor();
    }

    public boolean mouseClicked(int mouseX, int mouseY, MouseButtonEvent event) {
        if (mouseX >= x && mouseX <= x + WIDTH &&
            mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            if (event.button() == 0) {
                dragging = true;
                dragOffX = mouseX - x;
                dragOffY = mouseY - y;
                return true;
            }
        }

        float drawY = y + HEADER_HEIGHT + scrollOffset;
        for (Module m : modules) {
            float moduleH = m.getHeight();
            if (mouseY >= drawY && mouseY <= drawY + moduleH) {
                if (m.mouseClicked(mouseX, mouseY, x, (int) drawY, event)) return true;
            }
            drawY += moduleH;
        }
        return false;
    }

    public void mouseReleased(MouseButtonEvent event) {
        dragging = false;
        for (Module m : modules) m.mouseReleased(event);
    }

    public boolean isDragging() { return dragging; }

    public void tickDrag(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffX;
            y = mouseY - dragOffY;
        }
    }

    public boolean charTyped(CharacterEvent event) {
        for (Module m : modules) if (m.charTyped(event)) return true;
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        for (Module m : modules) if (m.keyPressed(event)) return true;
        return false;
    }
}
