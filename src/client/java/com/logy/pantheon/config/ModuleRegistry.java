package com.logy.pantheon.config;

import com.logy.pantheon.config.gui.SettingDefinition;

import java.util.*;
import java.util.stream.Collectors;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ModuleRegistry {

    private static List<ModuleSchema> modules = new ArrayList<>();
    private static final Map<String, Map<String, Runnable>> buttonActions = new HashMap<>();
    private static boolean initialized = false;

    public static class ModuleSchema {
        public String id;
        public String name;
        public String category;
        public String type;
        public boolean economy;
        public List<SettingDefinition> settings = new ArrayList<>();
    }

    public static void init() {
        if (initialized) return;
        ModuleConfig.load();
        initialized = true;
    }

    public static void registerModule(String id, String name, String category, String type, boolean economy) {
        Optional<ModuleSchema> existing = getModule(id);
        if (existing.isPresent()) {
            ModuleSchema m = existing.get();
            m.name = name;
            m.category = category;
            m.type = type;
            m.economy = economy;
            return;
        }
        ModuleSchema schema = new ModuleSchema();
        schema.id = id;
        schema.name = name;
        schema.category = category;
        schema.type = type;
        schema.economy = economy;
        modules.add(schema);
    }

    public static void registerSetting(String moduleId, SettingDefinition def) {
        getModule(moduleId).ifPresent(m -> {
            if (m.settings.stream().noneMatch(s -> s.id.equals(def.id))) {
                m.settings.add(def);
            }
        });
        if (def.defaultValue != null) {
            ModuleConfig.get(moduleId).setDefaults(Map.of(def.id, def.defaultValue));
        }
    }

    public static void setWidgetDisplay(String moduleId, String settingId, String display) {
        getModule(moduleId).ifPresentOrElse(m -> {
            for (SettingDefinition def : m.settings) {
                if (def.id.equals(settingId)) {
                    def.name = display;
                    return;
                }
            }
            LOGGER.warn("[ModuleRegistry] gui widget '{}' has no matching setting in module '{}'", settingId, moduleId);
        }, () -> LOGGER.warn("[ModuleRegistry] Module '{}' not found for gui widget '{}'", moduleId, settingId));
    }

    public static void registerButton(String moduleId, String buttonId, String buttonName, Runnable callback) {
        registerSetting(moduleId, SettingDefinition.button(buttonId, buttonName));
        buttonActions.computeIfAbsent(moduleId, k -> new HashMap<>()).put(buttonId, callback);
    }

    public static Runnable getButtonAction(String moduleId, String buttonId) {
        Map<String, Runnable> actions = buttonActions.get(moduleId);
        return actions != null ? actions.get(buttonId) : null;
    }

    public static void removeButtons(String moduleId) {
        Map<String, Runnable> actions = buttonActions.remove(moduleId);
        getModule(moduleId).ifPresent(m -> {
            m.settings.removeIf(s -> s.type == SettingDefinition.SettingType.BUTTON);
        });
    }

    public static void removeButton(String moduleId, String buttonId) {
        Map<String, Runnable> actions = buttonActions.get(moduleId);
        if (actions != null) {
            actions.remove(buttonId);
            if (actions.isEmpty()) buttonActions.remove(moduleId);
        }
        getModule(moduleId).ifPresent(m -> {
            m.settings.removeIf(s -> s.type == SettingDefinition.SettingType.BUTTON && s.id.equals(buttonId));
        });
    }

    public static void clearSettings(String moduleId) {
        getModule(moduleId).ifPresent(m -> m.settings.clear());
    }

    public static List<ModuleSchema> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public static List<ModuleSchema> getModulesByCategory(String category) {
        return modules.stream()
                .filter(m -> m.category != null && m.category.equals(category))
                .sorted((a, b) -> {
                    int lenCmp = Integer.compare(b.name.length(), a.name.length());
                    return lenCmp != 0 ? lenCmp : a.name.compareTo(b.name);
                })
                .collect(Collectors.toList());
    }

    public static List<String> getCategories() {
        return modules.stream()
                .map(m -> m.category)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public static boolean isEnabled(String moduleId) {
        ModuleConfig cfg = ModuleConfig.get(moduleId);
        if (!cfg.has("enabled")) return true;
        return cfg.getBool("enabled");
    }

    public static void setEnabled(String moduleId, boolean enabled) {
        ModuleConfig.get(moduleId).set("enabled", enabled);
    }

    public static Optional<ModuleSchema> getModule(String moduleId) {
        return modules.stream().filter(m -> m.id.equals(moduleId)).findFirst();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
