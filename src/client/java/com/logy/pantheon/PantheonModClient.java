package com.logy.pantheon;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.FastWarpModule;
import com.logy.pantheon.features.clientcommands.ClientCommandManager;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.whoami.WhoAmILoader;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.DatabaseManager;
import com.logy.pantheon.utils.TPSMonitor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.lifecycle.v1.ClientLifecycleEvents;

import com.logy.pantheon.features.Experiments;

public class PantheonModClient implements ClientModInitializer {

    private boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if(initialized) return;
        PantheonConfig.load();
        WhoAmILoader.load();
        Experiments.init();
        ChatUtils.init();
        CommandManager.init();
        ClientCommandManager.init();
        DatabaseManager.init();
        Economy.init();
        TPSMonitor.init();
        FastWarpModule.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DatabaseManager.close());
        initialized = true;
    }
}
