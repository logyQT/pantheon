package com.logy.pantheon.features.commands.main;

import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.utils.ChatUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import com.logy.pantheon.features.commands.CommandPantheon;
import com.logy.pantheon.features.commands.scripting.ModuleInstance;

import java.util.*;
import java.util.function.Consumer;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class CommandManager {
    private static final Map<String, Map<String, ICommand>> commands = new HashMap<>();
    private static final List<GameInstance> REGISTERED_GAMES = new ArrayList<>();
    public static final String TOKEN = "!";

    private static void registerAll() {
        register(new CommandPantheon(), "built-in");
    }

    public static void registerGame(GameInstance game) {
        REGISTERED_GAMES.add(game);
    }

    public static List<String> getCommands() {
        return commands.keySet().stream()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public static List<String> getEnabledCommands() {
        return commands.entrySet().stream()
                .filter(entry -> entry.getValue().keySet().stream()
                    .anyMatch(source -> "built-in".equals(source) || ModuleRegistry.isEnabled(source)))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public static boolean isGameRunning() {
        return REGISTERED_GAMES.stream().anyMatch(GameInstance::isActive);
    }

    public static Optional<GameInstance> getActiveGame() {
        return REGISTERED_GAMES.stream().filter(GameInstance::isActive).findFirst();
    }

    public static void sendGameRunningError() {
        ChatUtils.sendPartyMessage("A game is already in progress! Finish it or wait for timeout.");
    }

    public static boolean isGameEnabled(String gameName) {
        return ModuleRegistry.isEnabled(gameName.toLowerCase());
    }

    public static <T> void tryStartGame(String gameName, T context, Consumer<T> gameStartLogic) {
        if (!isGameEnabled(gameName)) {
            ChatUtils.sendPartyMessage("That game is currently disabled!");
            return;
        }
        if (isGameRunning()) {
            sendGameRunningError();
            return;
        }
        gameStartLogic.accept(context);
    }

    public static void tryStartGame(String gameName, Runnable gameStartLogic) {
        if (!isGameEnabled(gameName)) {
            ChatUtils.sendPartyMessage("That game is currently disabled!");
            return;
        }
        if (isGameRunning()) {
            sendGameRunningError();
            return;
        }
        gameStartLogic.run();
    }

    public static void update() {
        REGISTERED_GAMES.forEach(GameInstance::update);
    }

    public static void init(){
        registerAll();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CommandManager.getActiveGame().ifPresent(game -> {
                LOGGER.info("[Pantheon] Game stopped due to disconnect.");
                if (game instanceof ModuleInstance mi) {
                    mi.stopForDisconnect();
                } else {
                    game.stop();
                }
            });
        });
    }

    public static void register(ICommand cmd) {
        register(cmd, "unknown");
    }

    public static void register(ICommand cmd, String source) {
        String key = TOKEN + cmd.getName().toLowerCase();
        commands.computeIfAbsent(key, k -> new HashMap<>()).put(source, cmd);
    }

    public static void unregister(String name) {
        String key = TOKEN + name.toLowerCase();
        commands.remove(key);
    }

    public static void unregister(String name, String source) {
        String key = TOKEN + name.toLowerCase();
        Map<String, ICommand> handlers = commands.get(key);
        if (handlers != null) {
            handlers.remove(source);
            if (handlers.isEmpty()) commands.remove(key);
        }
    }

    public static void handle(String sender, String content) {
        String[] args = content.split(" ");
        Map<String, ICommand> handlers = commands.get(args[0].toLowerCase());

        if (handlers != null) {
            String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
            for (var entry : handlers.entrySet()) {
                String source = entry.getKey();
                ICommand cmd = entry.getValue();
                if ("built-in".equals(source) || ModuleRegistry.isEnabled(source)) {
                    cmd.execute(sender, cmdArgs);
                }
            }
        }
    }
}
