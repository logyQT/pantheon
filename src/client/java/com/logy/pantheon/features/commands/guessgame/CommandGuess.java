package com.logy.pantheon.features.commands.guessgame;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandGuess extends BaseCommand {
    GuessingGame game = new GuessingGame();

    public CommandGuess(){
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "guess"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(sender, game::start);
    }
}
