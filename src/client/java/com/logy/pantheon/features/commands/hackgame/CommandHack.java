package com.logy.pantheon.features.commands.hackgame;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandHack extends BaseCommand {
    private final HackGame game = new HackGame();

    public CommandHack() {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "hack"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}