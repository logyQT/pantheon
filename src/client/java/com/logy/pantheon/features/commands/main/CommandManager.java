package com.logy.pantheon.features.commands.main;


import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.commands.ascii.AsciiArt;
import com.logy.pantheon.features.commands.hangman.WordLoader;
import com.logy.pantheon.features.commands.wheelgame.WheelWordLoader;
import com.logy.pantheon.features.commands.wordchain.WordChainLoader;
import com.logy.pantheon.utils.ChatUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.*;
import java.util.function.Consumer;
import org.reflections.Reflections;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class CommandManager {
    private static final Map<String, ICommand> commands = new HashMap<>();
    private static final List<GameInstance> REGISTERED_GAMES = new ArrayList<>();
    public static final String TOKEN = "!";

    private static void registerAll() {
        Reflections reflections = new Reflections("com.logy.pantheon.features.commands");
        reflections.getTypesAnnotatedWith(AutoRegister.class).forEach(clazz -> {
            try {
                register((ICommand) clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                LOGGER.error("[Pantheon] Failed to register command: " + clazz.getSimpleName(), e);
            }
        });
    }

    public static void registerGame(GameInstance game) {
        REGISTERED_GAMES.add(game);
    }

    public static List<String> getCommands() {
        return commands.keySet().stream()
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
        PantheonConfig c = PantheonConfig.get();
        return switch (gameName.toLowerCase()) {
            case "blackjack" -> c.BLACKJACK_ENABLED;
            case "hangman" -> c.HANGMAN_ENABLED;
            case "hack" -> c.HACK_ENABLED;
            case "wordchain" -> c.WORDCHAIN_ENABLED;
            case "roulette" -> c.ROULETTE_ENABLED;
            case "whoami" -> c.WHOAMI_ENABLED;
            case "math" -> c.MATH_ENABLED;
            case "speedtype" -> c.SPEEDTYPE_ENABLED;
            case "guess" -> c.GUESS_ENABLED;
            case "rguess" -> c.RGUESS_ENABLED;
            case "wheel" -> c.WHEEL_ENABLED;
            default -> true;
        };
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
        AsciiArt.init();

        WordLoader.loadWords();
        WheelWordLoader.loadPhrases();
        WordChainLoader.loadWords();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.info("[Pantheon] Game stopped due to disconnect.");
            CommandManager.getActiveGame().ifPresent(GameInstance::stop);
        });
    }

    public static void register(ICommand cmd) {
        commands.put(TOKEN + cmd.getName().toLowerCase(), cmd);
    }

    public static void unregister(String name) {
        commands.remove(TOKEN + name.toLowerCase());
    }

    public static void handle(String sender, String content) {
        String[] args = content.split(" ");
        ICommand cmd = commands.get(args[0].toLowerCase());

        if (cmd != null) {
            String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
            cmd.execute(sender, cmdArgs);
        }
    }
}
