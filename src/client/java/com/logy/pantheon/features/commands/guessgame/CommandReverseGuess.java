package com.logy.pantheon.features.commands.guessgame;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandReverseGuess extends BaseCommand {
    private final ReverseGuessingGame game = new ReverseGuessingGame();

    public CommandReverseGuess() {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "rguess"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(sender, game::start);
    }
}
