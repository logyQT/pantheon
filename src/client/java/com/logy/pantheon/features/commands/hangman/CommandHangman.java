package com.logy.pantheon.features.commands.hangman;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandHangman extends BaseCommand {
    private final HangmanGame game = new HangmanGame();

    public CommandHangman(){
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "hangman"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}
