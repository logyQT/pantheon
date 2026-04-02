package com.logy.pantheon.features.commands.wordchain;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandWordChain extends BaseCommand {
    private final WordChainGame game = new WordChainGame();

    public CommandWordChain() {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "wordchain"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}