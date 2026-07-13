package com.logy.pantheon.config.gui.util;

public class ColorUtil {
    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int brighter(int color, float factor) {
        int r = (int) Math.min(255, ((color >> 16) & 0xFF) * factor);
        int g = (int) Math.min(255, ((color >> 8) & 0xFF) * factor);
        int b = (int) Math.min(255, (color & 0xFF) * factor);
        return rgba(r, g, b, (color >> 24) & 0xFF);
    }

    public static final int WHITE = rgb(255, 255, 255);
    public static final int LIGHT_GRAY = rgb(200, 200, 200);
    public static final int GRAY = rgb(128, 128, 128);
    public static final int DARK_GRAY = rgb(60, 60, 60);
    public static final int VERY_DARK = rgb(26, 26, 26);
    public static final int DARK_PANEL = rgb(38, 38, 38);
    public static final int BLACK = rgb(0, 0, 0);
    public static final int HIGHLIGHT = rgb(50, 150, 220);
}
