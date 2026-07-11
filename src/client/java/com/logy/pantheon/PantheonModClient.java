package com.logy.pantheon;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.FastWarpModule;
import com.logy.pantheon.features.clientcommands.ClientCommandManager;
import com.logy.pantheon.features.FastWarpModule;
import com.logy.pantheon.features.clientcommands.ClientCommandManager;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.DatabaseManager;
import com.logy.pantheon.utils.TPSMonitor;
import net.fabricmc.api.ClientModInitializer;

import com.logy.pantheon.features.Experiments;

public class PantheonModClient implements ClientModInitializer {

    private boolean initialized = false;


    private boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if(initialized) return;
        PantheonConfig.load();
        Experiments.init();
        ChatUtils.init();
        CommandManager.init();
        ClientCommandManager.init();
        ClientCommandManager.init();
        DatabaseManager.init();
        Economy.init();
        TPSMonitor.init();
        FastWarpModule.init();
        initialized = true;
        FastWarpModule.init();
        initialized = true;
    }
}
