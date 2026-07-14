package com.logy.pantheon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleConfig {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pantheon/config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static Map<String, Map<String, Object>> ALL_DATA = new HashMap<>();
    private static final Map<String, ModuleConfig> INSTANCES = new ConcurrentHashMap<>();

    private final String moduleId;

    private ModuleConfig(String moduleId) {
        this.moduleId = moduleId;
    }

    @SuppressWarnings("unchecked")
    public static void load() {
        CONFIG_FILE.getParentFile().mkdirs();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Map<String, Object> root = GSON.fromJson(reader, MAP_TYPE);
                if (root != null) {
                    Object s = root.get("settings");
                    if (s instanceof Map) {
                        ALL_DATA = (Map<String, Map<String, Object>>) s;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        Map<String, Object> root = new HashMap<>();
        root.put("settings", ALL_DATA);
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAll() {
        save();
    }

    private Map<String, Object> data() {
        return ALL_DATA.computeIfAbsent(moduleId, k -> new HashMap<>());
    }

    public static ModuleConfig get(String moduleId) {
        return INSTANCES.computeIfAbsent(moduleId, ModuleConfig::new);
    }

    public boolean getBool(String key) {
        Object v = data().get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return false;
    }

    public int getInt(String key) {
        Object v = data().get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    public String getString(String key) {
        Object v = data().get(key);
        if (v instanceof String) return (String) v;
        return "";
    }

    public void set(String key, Object value) {
        data().put(key, value);
        save();
    }

    public boolean has(String key) {
        return data().containsKey(key);
    }

    public void setDefaults(Map<String, Object> defaults) {
        Map<String, Object> d = data();
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!d.containsKey(entry.getKey())) {
                d.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) save();
    }
}
