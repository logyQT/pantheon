package com.logy.pantheon.features.commands;

import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.utils.ChatUtils;

public class CommandPantheon extends BaseCommand {
    @Override
    public String getName() { return "pantheon"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        String commands = String.join(", ", CommandManager.getEnabledCommands());

        ChatUtils.sendPartyMessage("Available commands: " + commands);
    }
}
