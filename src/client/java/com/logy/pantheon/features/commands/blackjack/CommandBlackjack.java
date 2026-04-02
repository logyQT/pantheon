package com.logy.pantheon.features.commands.blackjack;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandBlackjack extends BaseCommand {
    private final BlackjackGame game = new BlackjackGame();

    public CommandBlackjack() {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "blackjack"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}