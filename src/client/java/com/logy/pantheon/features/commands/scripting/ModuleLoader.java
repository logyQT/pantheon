package com.logy.pantheon.features.commands.scripting;

import com.google.gson.Gson;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.ICommand;
import com.logy.pantheon.utils.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.mozilla.javascript.*;

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
        String[] exampleModules = {"number_guesser", "ascii_pictures", "meow"};
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

        // Sort: module.js last, rest alphabetical
        Arrays.sort(jsFiles, (a, b) -> {
            if (a.getName().equals("module.js")) return 1;
            if (b.getName().equals("module.js")) return -1;
            return a.getName().compareTo(b.getName());
        });

        Scriptable scope = createScope();
        ScriptApi api = new ScriptApi(moduleName, moduleDir, scope);
        ModuleData data = new ModuleData(moduleName, moduleDir, scope, api);

        // Inject pantheon global
        {
            Context ctx = Context.enter();
            try {
                ctx.setOptimizationLevel(-1);
                Object wrappedApi = Context.javaToJS(api, scope);
                ScriptableObject.putProperty(scope, "pantheon", wrappedApi);
            } finally {
                Context.exit();
            }
        }

        // Inject require() function for this module
        injectRequire(scope, api, data);

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
                evaluateInScope(scope, source, moduleName + "/" + file.getName());
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

    private void injectRequire(Scriptable scope, ScriptApi api, ModuleData data) {
        Map<String, Object> requireCache = new HashMap<>();
        ScriptableObject.putProperty(scope, "require", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new RuntimeException("require(): path required");
                }
                String path = Context.toString(args[0]);
                if (!path.endsWith(".js")) path += ".js";
                Path resolved = data.moduleDir.toPath().resolve(path).normalize();
                if (!resolved.startsWith(SCRIPTS_DIR.toPath())) {
                    throw new RuntimeException("require(): path outside scripts directory");
                }
                if (Files.isDirectory(resolved)) {
                    resolved = resolved.resolve("module.js");
                }
                String cacheKey = resolved.toString();
                if (requireCache.containsKey(cacheKey)) {
                    return requireCache.get(cacheKey);
                }
                try {
                    if (!Files.exists(resolved)) {
                        throw new RuntimeException("Module not found: " + path);
                    }
                    long size = Files.size(resolved);
                    if (size > MAX_SCRIPT_SIZE) {
                        throw new RuntimeException("Script too large: " + path);
                    }
                    String source = Files.readString(resolved, StandardCharsets.UTF_8);
                    if (source.length() > 0 && source.charAt(0) == '\uFEFF') {
                        source = source.substring(1);
                    }
                    Scriptable exports = cx.newObject(scope);
                    ScriptableObject.putProperty(exports, "exports", exports);
                    Scriptable sandbox = cx.newObject(scope);
                    sandbox.setPrototype(scope);
                    sandbox.setParentScope(null);
                    ScriptableObject.putProperty(sandbox, "exports", exports);
                    ScriptableObject.putProperty(sandbox, "require", this);
                    cx.evaluateString(sandbox, source, resolved.toString(), 1, null);
                    requireCache.put(cacheKey, exports);
                    return exports;
                } catch (IOException e) {
                    throw new RuntimeException("Error loading module: " + path, e);
                }
            }

            @Override
            public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
                return null;
            }
        });
    }

    private void registerModule(String name, ModuleData data) {
        ScriptApi api = data.api;

        // Read metadata
        String displayName = api.getDisplayName();
        if (displayName == null || displayName.isEmpty()) displayName = formatName(name);
        String category = api.getCategory();
        if (category == null || category.isEmpty()) category = "Scripted Modules";

        // Register in ModuleRegistry with final metadata
        ModuleRegistry.registerModule(name, displayName, category, "script", true);

        boolean hasEnabled = ModuleRegistry.getModule(name)
                .map(m -> m.settings.stream().anyMatch(s -> "enabled".equals(s.id)))
                .orElse(false);
        if (!hasEnabled) {
            ModuleRegistry.registerSetting(name, SettingDefinition.bool("enabled", "Enabled", true));
        }

        // Check for command hook
        if (api.hasCallback("onCommand") && api.getCommandName() != null) {
            String cmdName = api.getCommandName();
            CommandManager.register(new ICommand() {
                @Override
                public String getName() { return cmdName; }

                @Override
                public void execute(String sender, String[] args) {
                    if (!ModuleRegistry.isEnabled(name)) {
                        ChatUtils.sendFeedback("§cModule '" + name + "' is disabled!");
                        return;
                    }
                    api.lastCommandSender = sender;
                    api.invokeCallback("onCommand", sender, args);
                }
            }, name);
            api.getRegisteredCommands().add(cmdName);
            LOGGER.info("[ModuleLoader] Registered command '!{}' for module '{}'", cmdName, name);
        }

        // Check for game hooks
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
            for (Function fn : api.chatHandlers) {
                try {
                    invokeChatFn(data.scope, fn, new Object[]{rawText});
                } catch (Exception e) {
                    LOGGER.error("[ModuleLoader] Chat handler error in {}", data.name, e);
                }
            }
            for (var entry : api.chatPatternHandlers.entrySet()) {
                if (clean.contains(entry.getKey())) {
                    try {
                        invokeChatFn(data.scope, entry.getValue(), new Object[]{rawText});
                    } catch (Exception e) {
                        LOGGER.error("[ModuleLoader] Chat pattern handler error in {}", data.name, e);
                    }
                }
            }
        }
    }

    private void invokeChatFn(Scriptable scope, Function fn, Object[] args) {
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            ctx.setClassShutter(ScriptApi.CLASS_SHUTTER);
            fn.call(ctx, scope, scope, args);
        } finally {
            Context.exit();
        }
    }

    // ── Ticking ──

    public void tickAll() {
        for (ModuleInstance inst : gameModules.values()) {
            inst.update();
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
        }
        loadModule(new File(SCRIPTS_DIR, moduleName));
    }

    // ── Scope helpers ──

    private Scriptable createScope() {
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            ctx.setClassShutter(ScriptApi.CLASS_SHUTTER);
            Scriptable scope = ctx.initStandardObjects();
            ScriptableObject.deleteProperty(scope, "Packages");
            ScriptableObject.deleteProperty(scope, "java");
            ScriptableObject.deleteProperty(scope, "javax");
            ScriptableObject.deleteProperty(scope, "com");
            ScriptableObject.deleteProperty(scope, "edu");
            ScriptableObject.deleteProperty(scope, "org");
            ScriptableObject.deleteProperty(scope, "net");
            return scope;
        } finally {
            Context.exit();
        }
    }

    private void evaluateInScope(Scriptable scope, String source, String filename) {
        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            ctx.setClassShutter(ScriptApi.CLASS_SHUTTER);
            ctx.evaluateString(scope, source, filename, 1, null);
        } finally {
            Context.exit();
        }
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
        final Scriptable scope;
        final ScriptApi api;

        ModuleData(String name, File moduleDir, Scriptable scope, ScriptApi api) {
            this.name = name;
            this.moduleDir = moduleDir;
            this.scope = scope;
            this.api = api;
        }
    }
}
