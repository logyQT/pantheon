package com.logy.pantheon.features.commands.scripting;

import com.google.gson.Gson;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.utils.ChatUtils;
import graal.graalvm.polyglot.Context;
import graal.graalvm.polyglot.HostAccess;
import graal.graalvm.polyglot.PolyglotAccess;
import graal.graalvm.polyglot.Source;
import graal.graalvm.polyglot.Value;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ModuleLoader {

    private static final File SCRIPTS_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("pantheon/scripts").toFile();

    private static final long MAX_SCRIPT_SIZE = 10_485_760;
    private static ModuleLoader instance;

    private final Map<String, ModuleData> modules = new HashMap<>();
    private final Map<String, ModuleInstance> gameModules = new HashMap<>();

    public ModuleLoader() {
        instance = this;
    }

    public static ModuleLoader getInstance() {
        return instance;
    }

    public void init() {
        LOGGER.info("[ModuleLoader] Initializing scripts directory: {}", SCRIPTS_DIR);
        SCRIPTS_DIR.mkdirs();
        copyDefaultsIfNeeded();
        loadAllModules();
        ChatUtils.setChatCallback(this::dispatchChat);
    }

    // ── Default module copying ──

    private void copyDefaultsIfNeeded() {
        String[] exampleModules = {"number_guesser", "ascii_pictures", "meow", "economy", "blackjack"};
        Gson gson = new Gson();
        for (String name : exampleModules) {
            String manifestPath = "/assets/pantheon/scripts/" + name + "/manifest.json";
            try (InputStream in = getClass().getResourceAsStream(manifestPath)) {
                if (in == null) {
                    copyDefaultFile(name, "module.js");
                    continue;
                }
                String[] files = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), String[].class);
                if (files == null) {
                    copyDefaultFile(name, "module.js");
                    continue;
                }
                for (String file : files) {
                    copyDefaultFile(name, file);
                }
            } catch (IOException e) {
                LOGGER.error("[ModuleLoader] Failed to read manifest for module: {}", name, e);
            }
        }
    }

    private void copyDefaultFile(String moduleName, String fileName) {
        File target = new File(SCRIPTS_DIR, moduleName + "/" + fileName);
        if (target.exists()) return;
        String resourcePath = "/assets/pantheon/scripts/" + moduleName + "/" + fileName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in != null) {
                target.getParentFile().mkdirs();
                Files.copy(in, target.toPath());
                LOGGER.info("[ModuleLoader] Copied default file: {}/{}", moduleName, fileName);
            } else {
                LOGGER.warn("[ModuleLoader] Resource not found: {}/{}", moduleName, fileName);
            }
        } catch (IOException e) {
            LOGGER.error("[ModuleLoader] Failed to copy {}/{}", moduleName, fileName, e);
        }
    }

    // ── Module loading ──

    private void loadAllModules() {
        File[] dirs = SCRIPTS_DIR.listFiles(File::isDirectory);
        if (dirs == null) return;
        LOGGER.info("[ModuleLoader] Found {} module(s)", dirs.length);
        for (File dir : dirs) {
            loadModule(dir);
        }
    }

    private void loadModule(File moduleDir) {
        String moduleName = moduleDir.getName();
        LOGGER.info("[ModuleLoader] Loading module: {}", moduleName);

        File[] jsFiles = moduleDir.listFiles((dir, name) -> name.endsWith(".js"));
        if (jsFiles == null || jsFiles.length == 0) {
            LOGGER.warn("[ModuleLoader] No .js files in module: {}", moduleName);
            return;
        }

        Arrays.sort(jsFiles, (a, b) -> {
            if (a.getName().equals("module.js")) return 1;
            if (b.getName().equals("module.js")) return -1;
            return a.getName().compareTo(b.getName());
        });

        Context context = createContext();
        ScriptApi api = new ScriptApi(moduleName, moduleDir);
        api.context = context;
        ModuleData data = new ModuleData(moduleName, moduleDir, context, api);

        // Inject namespace globals
        Value bindings = context.getBindings("js");
        bindings.putMember("chat", api.chat);
        bindings.putMember("game", api.game);
        bindings.putMember("settings", api.settings);
        bindings.putMember("command", api.command);
        bindings.putMember("gui", api.gui);
        bindings.putMember("economy", api.economy);
        bindings.putMember("audio", api.audio);
        bindings.putMember("core", api.core);

        // Inject setTimeout/clearTimeout as native-style globals
        bindings.putMember("_api", api);
        context.eval("js",
            "function setTimeout(fn, ms) { return _api.setTimeout(fn, ms); }\n" +
            "function clearTimeout(id) { _api.clearTimeout(id); }\n"
        );

        // Inject fetch polyfill
        try (var in = getClass().getResourceAsStream("/assets/pantheon/fetch-polyfill.js")) {
            if (in != null) {
                context.eval("js", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } else {
                LOGGER.warn("[ModuleLoader] fetch-polyfill.js not found");
            }
        } catch (IOException e) {
            LOGGER.error("[ModuleLoader] Failed to load fetch-polyfill.js", e);
        }

        // Inject require() function for backward compatibility
        injectRequire(context, api, data);

        // Register placeholder module + clear old settings BEFORE script eval
        ModuleRegistry.registerModule(moduleName, moduleName, null, "script", true);
        ModuleRegistry.clearSettings(moduleName);

        // Evaluate all js files
        for (File file : jsFiles) {
            try {
                if (file.length() > MAX_SCRIPT_SIZE) {
                    LOGGER.error("[ModuleLoader] Script too large ({} bytes), skipping: {}", file.length(), file.getName());
                    continue;
                }
                String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                if (source.isEmpty()) continue;
                if (source.length() > 0 && source.charAt(0) == '\uFEFF') {
                    source = source.substring(1);
                }
                Source src = Source.newBuilder("js", source, moduleName + "/" + file.getName())
                        .buildLiteral();
                context.eval(src);
            } catch (Exception e) {
                LOGGER.error("[ModuleLoader] Failed to evaluate {}: {}", file.getName(), e.getMessage());
            }
        }

        // Register module hooks
        try {
            registerModule(moduleName, data);
        } catch (RuntimeException e) {
            LOGGER.error("[ModuleLoader] Failed to register module {}: {}", moduleName, e.getMessage());
            ChatUtils.sendFeedback("§cModule '" + moduleName + "' failed: " + e.getMessage());
        }
    }

    private void injectRequire(Context context, ScriptApi api, ModuleData data) {
        // Simple require() using GraalJS eval + exports object pattern
        String requireFn = "(function() {\n" +
            "  var cache = {};\n" +
            "  return function(path) {\n" +
            "    if (!path.endsWith('.js')) path += '.js';\n" +
            "    var resolved = path;\n" +
            "    if (cache[resolved]) return cache[resolved];\n" +
            "    var exports = {};\n" +
            "    cache[resolved] = exports;\n" +
            "    return exports;\n" +
            "  };\n" +
            "})()";
        context.eval("js", "var require = " + requireFn + ";");
    }

    private void registerModule(String name, ModuleData data) {
        ScriptApi api = data.api;

        String displayName = api.getDisplayName();
        if (displayName == null || displayName.isEmpty()) displayName = formatName(name);
        String category = api.getCategory();
        if (category == null || category.isEmpty()) category = "Scripted Modules";

        ModuleRegistry.registerModule(name, displayName, category, "script", true);

        boolean hasEnabled = ModuleRegistry.getModule(name)
                .map(m -> m.settings.stream().anyMatch(s -> "enabled".equals(s.id)))
                .orElse(false);
        if (!hasEnabled) {
            ModuleRegistry.registerSetting(name, SettingDefinition.bool("enabled", "Enabled", true));
        }

        if (api.hasCallback("onStart") || api.hasCallback("onTick")) {
            ModuleInstance inst = new ModuleInstance(name, api);
            api.gameInstance = inst;
            gameModules.put(name, inst);
            CommandManager.registerGame(inst);
            LOGGER.info("[ModuleLoader] Registered game module: {}", name);
        }

        modules.put(name, data);
        LOGGER.info("[ModuleLoader] === Loaded module: {} ===", name);
    }

    // ── Chat dispatch ──

    private void dispatchChat(String rawText) {
        String clean = ChatUtils.stripFormatting(rawText).toLowerCase();
        for (ModuleData data : modules.values()) {
            ScriptApi api = data.api;
            for (Value fn : api.chatHandlers) {
                try {
                    fn.execute(rawText);
                } catch (Exception e) {
                    LOGGER.error("[ModuleLoader] Chat handler error in {}", data.name, e);
                }
            }
            for (var entry : api.chatPatternHandlers.entrySet()) {
                if (clean.contains(entry.getKey())) {
                    try {
                        entry.getValue().execute(rawText);
                    } catch (Exception e) {
                        LOGGER.error("[ModuleLoader] Chat pattern handler error in {}", data.name, e);
                    }
                }
            }
        }
    }

    // ── Ticking ──

    public void tickAll() {
        for (var entry : modules.entrySet()) {
            entry.getValue().api.tickTimers();
        }
        for (ModuleInstance inst : gameModules.values()) {
            if (!inst.isActive()) continue;
            if (!ModuleRegistry.isEnabled(inst.getName())) {
                inst.stopForDisabled();
                continue;
            }
            if (inst.getApi().hasCallback("onTick")) {
                try {
                    inst.getApi().invokeCallback("onTick");
                } catch (Exception e) {
                    LOGGER.error("[Module] onTick error in {}", inst.getName(), e);
                }
            }
        }
    }

    // ── Reload ──

    public void reloadAll() {
        LOGGER.info("[ModuleLoader] Reloading all modules");
        for (var entry : modules.entrySet()) {
            String name = entry.getKey();
            ModuleData data = entry.getValue();
            ModuleInstance gameInst = gameModules.remove(name);
            if (gameInst != null) {
                if (gameInst.isActive()) gameInst.stopForReload();
                gameInst.getApi().cleanup();
            } else {
                data.api.cleanup();
            }
            data.context.close();
        }
        modules.clear();
        gameModules.clear();
        loadAllModules();
    }

    private void reloadModule(String moduleName) {
        ModuleData old = modules.get(moduleName);
        if (old != null) {
            LOGGER.info("[ModuleLoader] Reloading module: {}", moduleName);
            ModuleInstance gameInst = gameModules.remove(moduleName);
            if (gameInst != null) {
                if (gameInst.isActive()) gameInst.stopForReload();
                gameInst.getApi().cleanup();
            } else {
                old.api.cleanup();
            }
            modules.remove(moduleName);
            old.context.close();
        }
        loadModule(new File(SCRIPTS_DIR, moduleName));
    }

    // ── Context creation ──

    private Context createContext() {
        return Context.newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(false)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(c -> false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .option("js.ecmascript-version", "2022")
                .build();
    }

    // ── Formatting ──

    private static String formatName(String raw) {
        String[] parts = raw.replace("-", "_").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    // ── Inner class ──

    private static class ModuleData {
        final String name;
        final File moduleDir;
        final Context context;
        final ScriptApi api;

        ModuleData(String name, File moduleDir, Context context, ScriptApi api) {
            this.name = name;
            this.moduleDir = moduleDir;
            this.context = context;
            this.api = api;
        }
    }
}
