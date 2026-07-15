package com.logy.pantheon.config.gui;

public class SettingDefinition {
    public enum SettingType {
        BOOLEAN, SLIDER, TEXT, BUTTON
    }

    public final String id;
    public String name;
    public final SettingType type;
    public final Object defaultValue;
    public final int min;
    public final int max;
    public final int step;

    public SettingDefinition(String id, String name, SettingType type, Object defaultValue, int min, int max, int step) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public static SettingDefinition bool(String id, String name, boolean defaultValue) {
        return new SettingDefinition(id, name, SettingType.BOOLEAN, defaultValue, 0, 0, 0);
    }

    public static SettingDefinition slider(String id, String name, int min, int max, int step, int defaultValue) {
        return new SettingDefinition(id, name, SettingType.SLIDER, defaultValue, min, max, step);
    }

    public static SettingDefinition text(String id, String name, String defaultValue) {
        return new SettingDefinition(id, name, SettingType.TEXT, defaultValue, 0, 0, 0);
    }

    public static SettingDefinition button(String id, String name) {
        return new SettingDefinition(id, name, SettingType.BUTTON, null, 0, 0, 0);
    }
}
