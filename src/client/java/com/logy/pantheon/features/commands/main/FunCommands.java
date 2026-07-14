package com.logy.pantheon.features.commands.main;

import com.logy.pantheon.features.clientcommands.CommandPearls;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.TPSMonitor;

public class FunCommands {

    public FunCommands() {}

    public static void processMessage(String rawText){
        String cleanText = ChatUtils.stripFormatting(rawText);

        // Autopearls module trigger.
        CommandPearls.check(cleanText);

        if (!cleanText.contains(":") || !cleanText.startsWith("Party")) return;

        String[] parts = cleanText.split(":", 2);
        String senderName = parts[0].substring(parts[0].lastIndexOf(" ") + 1);
        String content = parts[1].trim();

        if (content.startsWith(CommandManager.TOKEN)) {
            CommandManager.handle(senderName, content);
            return;
        }

        CommandManager.getActiveGame().ifPresent(game -> {
            game.handleChat(senderName, content);
        });
    }
}
