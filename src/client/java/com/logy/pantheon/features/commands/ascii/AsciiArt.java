package com.logy.pantheon.features.commands.ascii;

import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.ICommand;
import com.logy.pantheon.utils.ChatUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AsciiArt {

    private static final Map<String, String[]> ARTS = new LinkedHashMap<>();
    private static final Set<String> registeredNames = new HashSet<>();

    public static void init() {
        ARTS.clear();
        registeredNames.clear();
        PictureCommandLoader.load().forEach(AsciiArt::register);
        registerCommands();
    }

    public static void refresh() {
        registeredNames.forEach(CommandManager::unregister);
        registeredNames.clear();
        ARTS.clear();
        PictureCommandLoader.load().forEach(AsciiArt::register);
        registerCommands();
    }

    private static void register(String name, String art) {
        ARTS.put(name, art.split("\n"));
    }

    private static void registerCommands() {
        ARTS.forEach((name, lines) -> {
            CommandManager.register(new ICommand() {
                @Override
                public String getName() { return name; }

                @Override
                public void execute(String sender, String[] args) {
                    for (String line : lines) {
                        ChatUtils.sendPartyMessage(line);
                    }
                }
            });
            registeredNames.add(name);
        });
    }
}
