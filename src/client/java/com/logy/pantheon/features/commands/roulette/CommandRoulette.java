package com.logy.pantheon.features.commands.roulette;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandRoulette extends BaseCommand {
    private final RouletteGame game = new RouletteGame();

    public CommandRoulette() {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "roulette"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}