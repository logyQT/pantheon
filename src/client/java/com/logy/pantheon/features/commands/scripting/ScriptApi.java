package com.logy.pantheon.features.commands.scripting;

import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.ICommand;
import com.logy.pantheon.utils.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import org.mozilla.javascript.ClassShutter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptApi {

    static final ClassShutter CLASS_SHUTTER = className -> {
        // Block reflection & dangerous classes first
        if (className.startsWith("java.lang.reflect")) return false;
        if (className.equals("java.lang.Class")) return false;
        if (className.equals("java.lang.Runtime")) return false;
        if (className.equals("java.lang.ProcessBuilder")) return false;
        if (className.startsWith("java.lang.invoke")) return false;
        if (className.startsWith("java.lang.management")) return false;
        if (className.startsWith("java.lang.module")) return false;
        return className.startsWith("com.logy.pantheon.features.commands.scripting.ScriptApi")
            || className.startsWith("com.logy.pantheon.utils.ChatUtils")
            || className.startsWith("com.logy.pantheon.features.commands.economy.Economy")
            || className.startsWith("com.logy.pantheon.config.ModuleConfig")
            || className.startsWith("org.mozilla.javascript")
            || className.startsWith("java.lang");
    };

    final String moduleId;
    final File moduleDir;
    final Scriptable scope;
    Runnable stopCallback;
    final List<String> registeredCommands = new ArrayList<>();
    final List<String> registeredButtons = new ArrayList<>();
    final Map<String, DelayedTask> timerTasks = new HashMap<>();
    int nextTimerId = 0;
    String category;
    String displayName;
    String commandName;
    String commandDescription;
    final Map<String, Function> callbacks = new HashMap<>();
    final List<Function> chatHandlers = new ArrayList<>();
    final Map<String, Function> chatPatternHandlers = new HashMap<>();
    ModuleInstance gameInstance;
    String lastCommandSender;

    public final ChatApi chat = new ChatApi();
    public final EconomyApi economy = new EconomyApi();
    public final SettingsApi settings = new SettingsApi();
    public final CommandsApi commands = new CommandsApi();
    public final GuiApi gui = new GuiApi();
    public final TimerApi timers = new TimerApi();
    public final AudioApi audio = new AudioApi();
    public final GameLifecycle game = new GameLifecycle();
    public final CommandLifecycle command = new CommandLifecycle();

    public ScriptApi(String moduleId, File moduleDir, Scriptable scope) {
        this.moduleId = moduleId;
        this.moduleDir = moduleDir;
        this.scope = scope;
    }

    // ── Internal (Java-only) ──

    void setStopCallback(Runnable callback) {
        this.stopCallback = callback;
    }

    public List<String> getRegisteredCommands() {
        return registeredCommands;
    }

    public List<String> getRegisteredButtons() {
        return registeredButtons;
    }

    void cleanup() {
        clearRegisteredCommands();
        ModuleRegistry.removeButtons(moduleId);
        registeredButtons.clear();
        timerTasks.clear();
        callbacks.clear();
        chatHandlers.clear();
        chatPatternHandlers.clear();
        commandName = null;
        commandDescription = null;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    String getModuleId() {
        return moduleId;
    }

    void tickTimers() {
        long now = System.currentTimeMillis();
        var snapshot = new ArrayList<>(timerTasks.entrySet());
        for (var entry : snapshot) {
            if (now >= entry.getValue().scheduledTime) {
                timerTasks.remove(entry.getKey());
                try {
                    // Timer must run inside a Rhino Context (S8 fix)
                    Context ctx = Context.getCurrentContext() == null ? Context.enter() : null;
                    try {
                        entry.getValue().runnable.run();
                    } finally {
                        if (ctx != null) Context.exit();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ── Helpers ──

    private String getStringProp(Scriptable obj, String key) {
        Object val = obj.get(key, obj);
        return val instanceof String ? (String) val : "";
    }

    private int getNumberProp(Scriptable obj, String key, int fallback) {
        Object val = obj.get(key, obj);
        return val instanceof Number ? ((Number) val).intValue() : fallback;
    }

    private float getFloatProp(Scriptable obj, String key, float fallback) {
        Object val = obj.get(key, obj);
        return val instanceof Number ? ((Number) val).floatValue() : fallback;
    }

    private boolean getBoolProp(Scriptable obj, String key, boolean fallback) {
        Object val = obj.get(key, obj);
        return val instanceof Boolean ? (Boolean) val : fallback;
    }

    private void clearRegisteredCommands() {
        for (String name : registeredCommands) {
            CommandManager.unregister(name);
        }
        registeredCommands.clear();
    }

    // ── Lifecycle callback discovery ──

    boolean hasCallback(String name) {
        return callbacks.containsKey(name);
    }

    String getCommandName() {
        return commandName;
    }

    String getCommandDescription() {
        return commandDescription;
    }

    void invokeCallback(String name, Object... args) {
        Function fn = callbacks.get(name);
        if (fn == null) return;
        Context ctx = Context.getCurrentContext();
        boolean ownContext = false;
        if (ctx == null) {
            ctx = Context.enter();
            ctx.setOptimizationLevel(-1);
            ctx.setClassShutter(CLASS_SHUTTER);
            ownContext = true;
        }
        try {
            fn.call(ctx, scope, scope, args);
        } finally {
            if (ownContext) Context.exit();
        }
    }

    // ── Root-level JS API ──

    public void log(String message) {
        System.out.println("[Script:" + moduleId + "] " + message);
    }

    public String getUsername() {
        return ChatUtils.getUsername();
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

    public String readFile(String path) {
        try {
            Path resolved = moduleDir.toPath().resolve(path).normalize();
            Path scriptsRoot = moduleDir.toPath().getParent().normalize();

            if (!resolved.startsWith(scriptsRoot)) return null;
            if (!Files.exists(resolved) || !Files.isReadable(resolved)) return null;
            return Files.readString(resolved, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    // ── Namespace inner classes ──

    public class ChatApi {
        public void party(String message) {
            ChatUtils.sendPartyMessage(message);
        }

        public void guild(String message) {
            ChatUtils.sendGuildMessage(message);
        }

        public void all(String message) {
            ChatUtils.sendAllMessage(message);
        }

        public void command(String command) {
            ChatUtils.sendCommand(command);
        }

        public void feedback(String message) {
            ChatUtils.sendFeedback(message);
        }

        public void onMessage(Function callback) {
            chatHandlers.add(callback);
        }

        public void onMessage(String pattern, Function callback) {
            chatPatternHandlers.put(pattern.toLowerCase(), callback);
        }
    }

    public class EconomyApi {
        public void add(String player, int amount) {
            Economy.addMoney(player, amount);
        }

        public boolean take(String player, int amount) {
            return Economy.takeMoney(player, amount);
        }

        public void set(String player, int amount) {
            Economy.set(player, amount);
        }

        public int balance(String player) {
            return Economy.getCurrentBalance(player).get();
        }

        public boolean has(String player, int amount) {
            return Economy.hasEnough(player, amount);
        }
    }

    public class SettingsApi {
        public void setCategory(String category) {
            ScriptApi.this.category = category;
        }

        public void setDisplayName(String displayName) {
            ScriptApi.this.displayName = displayName;
        }

        public void addSlider(Scriptable options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            int defaultValue = getNumberProp(options, "default", 0);
            int min = getNumberProp(options, "min", 0);
            int max = getNumberProp(options, "max", 100);
            int step = getNumberProp(options, "step", 1);
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.slider(id, display, min, max, step, defaultValue));
        }

        public void addToggle(Scriptable options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            boolean defaultValue = getBoolProp(options, "default", false);
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.bool(id, display, defaultValue));
        }

        public void addText(Scriptable options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            String defaultValue = getStringProp(options, "default");
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.text(id, display, defaultValue));
        }

        public int get(String settingId) {
            return ModuleConfig.get(moduleId).getInt(settingId);
        }

        public boolean getBool(String settingId) {
            return ModuleConfig.get(moduleId).getBool(settingId);
        }

        public String getString(String settingId) {
            return ModuleConfig.get(moduleId).getString(settingId);
        }

        public void set(String settingId, Object value) {
            ModuleConfig.get(moduleId).set(settingId, value);
        }
    }

    public class CommandsApi {
        public void register(String name, Object callbackObj) {
            if (!(callbackObj instanceof Function)) return;
            Function fn = (Function) callbackObj;
            CommandManager.register(new ICommand() {
                @Override
                public String getName() { return name; }

                @Override
                public void execute(String sender, String[] args) {
                    if (!ModuleRegistry.isEnabled(moduleId)) {
                        ChatUtils.sendFeedback("§cModule '" + moduleId + "' is disabled!");
                        return;
                    }
                    lastCommandSender = sender;
                    Context ctx = Context.enter();
                    try {
                        ctx.setOptimizationLevel(-1);
                        ctx.setClassShutter(className ->
                            className.startsWith("com.logy.pantheon.features.commands.scripting.ScriptApi")
                            || className.startsWith("com.logy.pantheon.utils.ChatUtils")
                            || className.startsWith("com.logy.pantheon.features.commands.economy.Economy")
                            || className.startsWith("com.logy.pantheon.config.ModuleConfig")
                            || className.startsWith("org.mozilla.javascript")
                            || className.startsWith("java.lang")
                        );
                        fn.call(ctx, scope, scope, new Object[]{sender, args});
                    } finally {
                        Context.exit();
                    }
                }
            }, moduleId);
            registeredCommands.add(name);
        }

        public void unregister(String name) {
            CommandManager.unregister(name);
            registeredCommands.remove(name);
        }
    }

    public class GuiApi {
        public void register(String id, String name, Runnable callback) {
            ModuleRegistry.registerButton(moduleId, id, name, callback);
            registeredButtons.add(id);
        }

        public void unregister(String id) {
            ModuleRegistry.removeButton(moduleId, id);
            registeredButtons.remove(id);
        }

        public void openFolder() {
            File dir = FabricLoader.getInstance().getConfigDir().resolve("pantheon").toFile();
            dir.mkdirs();
            try {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "explorer", dir.getAbsolutePath()});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class AudioApi {
        public void playSound(Scriptable options) {
            String soundName = getStringProp(options, "sound");
            if (soundName.isEmpty()) return;
            float volume = getFloatProp(options, "volume", 1.0f);
            float pitch = getFloatProp(options, "pitch", 1.0f);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Identifier id = Identifier.parse(soundName);
            mc.player.playSound(SoundEvent.createVariableRangeEvent(id), volume, pitch);
        }
    }

    public class TimerApi {
        public int after(int ms, Runnable callback) {
            int id = nextTimerId++;
            timerTasks.put(String.valueOf(id), new DelayedTask(callback, ms));
            return id;
        }

        public void cancel(int id) {
            timerTasks.remove(String.valueOf(id));
        }
    }

    public class GameLifecycle {
        public void start() {
            if (gameInstance == null || lastCommandSender == null) return;
            CommandManager.tryStartGame(moduleId, lastCommandSender, gameInstance::start);
        }

        public void stop() {
            if (stopCallback != null) stopCallback.run();
        }

        public void onStart(Function callback) { callbacks.put("onStart", callback); }

        public void onStop(Function callback) { callbacks.put("onStop", callback); }

        public void onChat(Function callback) { callbacks.put("onChat", callback); }

        public void onTick(Function callback) { callbacks.put("onTick", callback); }
    }

    public class CommandLifecycle {
        public void onCommand(Function callback) { callbacks.put("onCommand", callback); }

        public void name(String name) { commandName = name; }

        public void description(String desc) { commandDescription = desc; }
    }

    static class DelayedTask {
        final Runnable runnable;
        final long delayMs;
        long scheduledTime;

        DelayedTask(Runnable runnable, long delayMs) {
            this.runnable = runnable;
            this.delayMs = delayMs;
            this.scheduledTime = System.currentTimeMillis() + delayMs;
        }
    }
}
