package com.logy.pantheon.features.commands.wheelgame;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandWheel extends BaseCommand {

    WheelGame game = new WheelGame();

    public CommandWheel(){
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "wheel"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}