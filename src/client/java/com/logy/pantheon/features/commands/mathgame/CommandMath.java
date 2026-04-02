package com.logy.pantheon.features.commands.mathgame;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandMath extends BaseCommand {
    MathGame game = new MathGame();

    public CommandMath(){
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "math"; }

    @Override
    protected void onRun(String sender, String target, String[] args){
        CommandManager.tryStartGame(game::start);
    }
}
