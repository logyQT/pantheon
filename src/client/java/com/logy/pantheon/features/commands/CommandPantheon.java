package com.logy.pantheon.features.commands;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.utils.ChatUtils;

@AutoRegister
public class CommandPantheon extends BaseCommand {
    @Override
    public String getName() { return "pantheon"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        String commands = String.join(", ", CommandManager.getCommands());

        ChatUtils.sendPartyMessage("Available commands: " + commands);
    }
}
