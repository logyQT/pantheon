package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.utils.ChatUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public abstract class ClientCommandBase {

    public abstract String getName();

    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal(getName());
        build(command);
        dispatcher.register(command);
    }

    protected abstract void build(LiteralArgumentBuilder<FabricClientCommandSource> builder);

    protected void sendFeedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal("§6[Pantheon] §f" + message));
    }

    protected void sendCommand(String cmd){
        ChatUtils.sendCommand(cmd);
    }
}
