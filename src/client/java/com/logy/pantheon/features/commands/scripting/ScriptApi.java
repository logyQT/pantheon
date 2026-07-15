package com.logy.pantheon.features.commands.scripting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.features.clientcommands.ClientCommandManager;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.ICommand;
import com.logy.pantheon.utils.ChatUtils;
import graal.graalvm.polyglot.Context;
import graal.graalvm.polyglot.Value;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptApi {

    final String moduleId;
    final File moduleDir;
    public Context context;
    Runnable stopCallback;
    final List<String> registeredButtons = new ArrayList<>();
    final Map<String, DelayedTask> timerTasks = new HashMap<>();
    int nextTimerId = 0;
    String category;
    String displayName;
    final Map<String, Value> callbacks = new HashMap<>();
    final List<Value> chatHandlers = new ArrayList<>();
    final Map<String, Value> chatPatternHandlers = new HashMap<>();
    ModuleInstance gameInstance;
    String lastCommandSender;
    final Map<String, CommandRegistration> commandRegistrations = new HashMap<>();
    int nextChangeListenerId = 0;
    final Map<Integer, String> changeListenerIds = new HashMap<>();

    final ConcurrentLinkedQueue<Runnable> pendingCallbacks = new ConcurrentLinkedQueue<>();

    public final ChatApi chat = new ChatApi();
    public final EconomyApi economy = new EconomyApi();
    public final SettingsApi settings = new SettingsApi();
    public final GuiApi gui = new GuiApi();
    public final AudioApi audio = new AudioApi();
    public final GameLifecycle game = new GameLifecycle();
    public final CommandLifecycle command = new CommandLifecycle();
    public final CoreApi core = new CoreApi();

    public ScriptApi(String moduleId, File moduleDir) {
        this.moduleId = moduleId;
        this.moduleDir = moduleDir;
    }

    // ── Internal (Java-only) ──

    void setStopCallback(Runnable callback) {
        this.stopCallback = callback;
    }

    public List<String> getRegisteredButtons() {
        return registeredButtons;
    }

    void cleanup() {
        for (String invoker : new ArrayList<>(commandRegistrations.keySet())) {
            command.off(invoker);
        }
        commandRegistrations.clear();
        changeListenerIds.clear();
        ModuleRegistry.removeButtons(moduleId);
        registeredButtons.clear();
        timerTasks.clear();
        pendingCallbacks.clear();
        callbacks.clear();
        chatHandlers.clear();
        chatPatternHandlers.clear();
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
                    entry.getValue().runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Runnable cb;
        while ((cb = pendingCallbacks.poll()) != null) {
            try {
                cb.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ── Helpers ──

    private String getStringProp(Value obj, String key) {
        if (obj == null || !obj.hasMember(key)) return "";
        Value val = obj.getMember(key);
        return val.isString() ? val.asString() : "";
    }

    private int getNumberProp(Value obj, String key, int fallback) {
        if (obj == null || !obj.hasMember(key)) return fallback;
        Value val = obj.getMember(key);
        return val.isNumber() ? val.asInt() : fallback;
    }

    private float getFloatProp(Value obj, String key, float fallback) {
        if (obj == null || !obj.hasMember(key)) return fallback;
        Value val = obj.getMember(key);
        return val.isNumber() ? val.asFloat() : fallback;
    }

    private boolean getBoolProp(Value obj, String key, boolean fallback) {
        if (obj == null || !obj.hasMember(key)) return fallback;
        Value val = obj.getMember(key);
        return val.isBoolean() ? val.asBoolean() : fallback;
    }

    // ── Lifecycle callback discovery ──

    boolean hasCallback(String name) {
        Value fn = callbacks.get(name);
        return fn != null && fn.canExecute();
    }

    void invokeCallback(String name, Object... args) {
        Value fn = callbacks.get(name);
        if (fn == null || !fn.canExecute()) return;
        try {
            fn.execute(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── File API (exposed via core.readFile) ──

    String readFile(String path) {
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

    // ── Timer API (exposed as setTimeout/clearTimeout globals) ──

    public int setTimeout(Value callback, int ms) {
        if (callback == null || !callback.canExecute()) return -1;
        int id = nextTimerId++;
        timerTasks.put(String.valueOf(id), new DelayedTask(() -> callback.execute(), ms));
        return id;
    }

    public void clearTimeout(int id) {
        timerTasks.remove(String.valueOf(id));
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

        public void client(String message) {
            ChatUtils.sendFeedback(message);
        }

        public void raw(String payload) {
            ChatUtils.sendRawMessage(payload);
        }

        public void feedback(String message) {
            ChatUtils.sendFeedback(message);
        }

        public void onMessage(Value callback) {
            if (callback != null && callback.canExecute()) {
                chatHandlers.add(callback);
            }
        }

        public void onMessage(String pattern, Value callback) {
            if (callback != null && callback.canExecute()) {
                chatPatternHandlers.put(pattern.toLowerCase(), callback);
            }
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
        public void category(String category) {
            ScriptApi.this.category = category;
        }

        public void displayName(String displayName) {
            ScriptApi.this.displayName = displayName;
        }

        public void slider(Value options) {
            String id = getStringProp(options, "id");
            int defaultValue = getNumberProp(options, "default", 0);
            int min = getNumberProp(options, "min", 0);
            int max = getNumberProp(options, "max", 100);
            int step = getNumberProp(options, "step", 1);
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.slider(id, null, min, max, step, defaultValue));
        }

        public void toggle(Value options) {
            String id = getStringProp(options, "id");
            boolean defaultValue = getBoolProp(options, "default", false);
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.bool(id, null, defaultValue));
        }

        public void text(Value options) {
            String id = getStringProp(options, "id");
            String defaultValue = getStringProp(options, "default");
            ModuleRegistry.registerSetting(moduleId, SettingDefinition.text(id, null, defaultValue));
        }

        public Object get(String settingId) {
            return ModuleConfig.get(moduleId).getRaw(settingId);
        }

        public void set(String settingId, Object value) {
            ModuleConfig.get(moduleId).set(settingId, value);
        }

        public Value onChange(String id, Value callback) {
            if (id == null || id.isEmpty() || callback == null || !callback.canExecute()) {
                return context.eval("js", "() => {}");
            }
            int lid = nextChangeListenerId++;
            changeListenerIds.put(lid, id);
            ModuleConfig.get(moduleId).addChangeListener(id, (newVal, oldVal) -> {
                if (callback.canExecute()) callback.execute(newVal, oldVal);
            });
            return context.eval("js", "(function() { settings._removeChangeListener(" + lid + "); })");
        }

        public void _removeChangeListener(int listenerId) {
            String key = changeListenerIds.remove(listenerId);
            if (key != null) {
                ModuleConfig.get(moduleId).removeChangeListener(key, listenerId);
            }
        }
    }

    public class GuiApi {
        public void button(Value options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            Value cb = options.getMember("callback");
            Runnable callback = () -> {
                if (cb != null && cb.canExecute()) cb.execute();
            };
            if (id.isEmpty() || display.isEmpty()) return;
            ModuleRegistry.registerButton(moduleId, id, display, callback);
            registeredButtons.add(id);
        }

        public void slider(Value options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            if (id.isEmpty()) return;
            ModuleRegistry.setWidgetDisplay(moduleId, id, display.isEmpty() ? id : display);
        }

        public void toggle(Value options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            if (id.isEmpty()) return;
            ModuleRegistry.setWidgetDisplay(moduleId, id, display.isEmpty() ? id : display);
        }

        public void text(Value options) {
            String id = getStringProp(options, "id");
            String display = getStringProp(options, "display");
            if (id.isEmpty()) return;
            ModuleRegistry.setWidgetDisplay(moduleId, id, display.isEmpty() ? id : display);
        }

        public void unregister(String id) {
            ModuleRegistry.removeButton(moduleId, id);
            registeredButtons.remove(id);
        }

        public void openFolder() {
            openFolder(null);
        }

        public void openFolder(Value options) {
            File base = FabricLoader.getInstance().getConfigDir().resolve("pantheon").toFile();

            if (options != null && options.hasMember("path")) {
                String rawPath = getStringProp(options, "path");
                if (!rawPath.isEmpty()) {
                    Path resolved = moduleDir.toPath().resolve(rawPath).normalize();
                    Path moduleRoot = moduleDir.toPath().normalize();

                    if (resolved.startsWith(moduleRoot)) {
                        base = resolved.toFile();
                    }
                }
            }

            base.mkdirs();
            try {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "explorer", base.getAbsolutePath()});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class AudioApi {
        public void playSound(Value options) {
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

    public class GameLifecycle {
        public void start() {
            if (gameInstance == null || lastCommandSender == null) return;
            CommandManager.tryStartGame(moduleId, lastCommandSender, gameInstance::start);
        }

        public void stop() {
            if (stopCallback != null) stopCallback.run();
        }

        public void onStart(Value callback) { if (callback != null && callback.canExecute()) callbacks.put("onStart", callback); }

        public void onStop(Value callback) { if (callback != null && callback.canExecute()) callbacks.put("onStop", callback); }

        public void onChat(Value callback) { if (callback != null && callback.canExecute()) callbacks.put("onChat", callback); }

        public void onTick(Value callback) { if (callback != null && callback.canExecute()) callbacks.put("onTick", callback); }
    }

    public class CommandLifecycle {
        public void onCommand(Value options) {
            if (options == null || !options.hasMember("invoker")) return;
            String invoker = getStringProp(options, "invoker");
            String description = getStringProp(options, "description");
            String type = getStringProp(options, "type");
            if (type.isEmpty()) type = "chat";
            Value cb = options.getMember("callback");
            if (invoker.isEmpty() || cb == null || !cb.canExecute()) return;

            if ("client".equals(type)) {
                // Build arg definitions from opts
                List<Map<String, Object>> argDefs = new ArrayList<>();
                Value argsVal = options.getMember("args");
                if (argsVal != null && argsVal.hasArrayElements()) {
                    for (long i = 0; i < argsVal.getArraySize(); i++) {
                        Value item = argsVal.getArrayElement(i);
                        if (!item.hasMember("name")) continue;
                        Map<String, Object> def = new HashMap<>();
                        def.put("name", getStringProp(item, "name"));
                        def.put("type", getStringProp(item, "type"));
                        def.put("optional", item.hasMember("optional") && item.getMember("optional").asBoolean());
                        argDefs.add(def);
                    }
                }
                ClientCommandManager.registerScriptCommand(moduleId, invoker, description, argDefs, cb);
                commandRegistrations.put(invoker, new CommandRegistration(invoker, "client", cb));
            } else {
                CommandManager.register(new ICommand() {
                    @Override
                    public String getName() { return invoker; }

                    @Override
                    public void execute(String sender, String[] args) {
                        if (!ModuleRegistry.isEnabled(moduleId)) return;
                        lastCommandSender = sender;
                        cb.execute(new Object[]{sender, args});
                    }
                }, moduleId);
                commandRegistrations.put(invoker, new CommandRegistration(invoker, "chat", cb));
            }
        }

        public void off(String invoker) {
            CommandRegistration reg = commandRegistrations.remove(invoker);
            if (reg == null) return;
            if ("client".equals(reg.type)) {
                ClientCommandManager.unregisterScriptCommand(invoker, moduleId);
            } else {
                CommandManager.unregister(invoker, moduleId);
            }
        }
    }

    public class CoreApi {
        public String readFile(String path) {
            return ScriptApi.this.readFile(path);
        }

        public void httpRequestAsync(String method, String url, String body, String headersJson, Value onComplete) {
            try {
                var client = HttpClient.newHttpClient();
                var reqBuilder = HttpRequest.newBuilder().uri(URI.create(url));

                if (headersJson != null && !headersJson.isEmpty()) {
                    Map<String, String> headers = new Gson().fromJson(headersJson,
                        new TypeToken<Map<String, String>>(){}.getType());
                    if (headers != null) {
                        for (var entry : headers.entrySet()) {
                            reqBuilder.header(entry.getKey(), entry.getValue());
                        }
                    }
                }

                switch (method.toUpperCase()) {
                    case "POST" -> reqBuilder.POST(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                    case "PUT" -> reqBuilder.PUT(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                    case "DELETE" -> reqBuilder.DELETE();
                    default -> reqBuilder.GET();
                }

                var capturedCallback = onComplete;
                client.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        var result = new HttpResult(response.statusCode(), response.body());
                        pendingCallbacks.add(() -> capturedCallback.execute(result));
                    })
                    .exceptionally(ex -> {
                        pendingCallbacks.add(() -> capturedCallback.execute(
                            new HttpResult(0, null, ex.getMessage())));
                        return null;
                    });
            } catch (Exception e) {
                pendingCallbacks.add(() -> onComplete.execute(
                    new HttpResult(0, null, e.getMessage())));
            }
        }

        public HttpResult httpRequest(String method, String url, String body, String headersJson) {
            try {
                var client = HttpClient.newHttpClient();
                var reqBuilder = HttpRequest.newBuilder().uri(URI.create(url));

                if (headersJson != null && !headersJson.isEmpty()) {
                    Map<String, String> headers = new Gson().fromJson(headersJson,
                        new TypeToken<Map<String, String>>(){}.getType());
                    if (headers != null) {
                        for (var entry : headers.entrySet()) {
                            reqBuilder.header(entry.getKey(), entry.getValue());
                        }
                    }
                }

                switch (method.toUpperCase()) {
                    case "POST" -> reqBuilder.POST(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                    case "PUT" -> reqBuilder.PUT(body != null
                        ? HttpRequest.BodyPublishers.ofString(body)
                        : HttpRequest.BodyPublishers.noBody());
                    case "DELETE" -> reqBuilder.DELETE();
                    default -> reqBuilder.GET();
                }

                var response = client.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

                return new HttpResult(response.statusCode(), response.body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class HttpResult {
        public final int status;
        public final String body;
        public final String errorMessage;

        public HttpResult(int status, String body) {
            this(status, body, null);
        }

        public HttpResult(int status, String body, String errorMessage) {
            this.status = status;
            this.body = body;
            this.errorMessage = errorMessage;
        }

        public boolean isOk() {
            return status >= 200 && status < 300;
        }

        public boolean hasError() {
            return errorMessage != null;
        }
    }

    static class CommandRegistration {
        final String invoker;
        final String type;
        final Value callback;

        CommandRegistration(String invoker, String type, Value callback) {
            this.invoker = invoker;
            this.type = type;
            this.callback = callback;
        }
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
