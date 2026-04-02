package com.logy.pantheon.features.commands.whoami;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

//@AutoRegister
public class CommandWhoAmI extends BaseCommand {

    private static final WhoAmIGame game = new WhoAmIGame();

    static {
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "whoami"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(sender, game::start);
    }
}