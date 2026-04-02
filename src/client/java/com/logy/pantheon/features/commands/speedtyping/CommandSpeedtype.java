package com.logy.pantheon.features.commands.speedtyping;

import com.logy.pantheon.features.commands.main.AutoRegister;
import com.logy.pantheon.features.commands.main.BaseCommand;
import com.logy.pantheon.features.commands.main.CommandManager;

@AutoRegister
public class CommandSpeedtype extends BaseCommand {
    SpeedTypingGame game = new SpeedTypingGame();

    public CommandSpeedtype(){
        CommandManager.registerGame(game);
    }

    @Override
    public String getName() { return "speedtype"; }

    @Override
    protected void onRun(String sender, String target, String[] args) {
        CommandManager.tryStartGame(game::start);
    }
}
