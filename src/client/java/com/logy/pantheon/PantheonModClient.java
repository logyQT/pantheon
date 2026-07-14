package com.logy.pantheon;

import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.features.FastWarpModule;
import com.logy.pantheon.features.clientcommands.ClientCommandManager;
import com.logy.pantheon.features.clientcommands.CommandPearls;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.scripting.ModuleLoader;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.DatabaseManager;
import com.logy.pantheon.utils.TPSMonitor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class PantheonModClient implements ClientModInitializer {

    private boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if(initialized) return;
        ModuleRegistry.init();

        // Register all module schemas
        ChatUtils.register();
        FastWarpModule.register();
        CommandPearls.register();

        ChatUtils.init();
        CommandManager.init();
        new ModuleLoader().init();
        ClientCommandManager.init();
        DatabaseManager.init();
        Economy.init();
        TPSMonitor.init();
        FastWarpModule.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            DatabaseManager.close();
            ModuleConfig.saveAll();
        });
        initialized = true;
    }
}
