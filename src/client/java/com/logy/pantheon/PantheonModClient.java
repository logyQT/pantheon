package com.logy.pantheon;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.DatabaseManager;
import com.logy.pantheon.utils.TPSMonitor;
import net.fabricmc.api.ClientModInitializer;

import com.logy.pantheon.features.Experiments;

public class PantheonModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //Experiments.init();
        PantheonConfig.load();
        ChatUtils.init();
        CommandManager.init();
        DatabaseManager.init();
        Economy.init();
        TPSMonitor.init();
    }
}
