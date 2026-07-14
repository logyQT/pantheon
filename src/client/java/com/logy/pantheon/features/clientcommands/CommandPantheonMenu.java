package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.config.PantheonClickGui;
import com.logy.pantheon.features.commands.scripting.ModuleLoader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

public class CommandPantheonMenu extends ClientCommandBase {

    @Override
    public String getName() {
        return "pantheon";
    }

    @Override
    protected void build(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder.executes(context -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                client.setScreen(new PantheonClickGui(client.screen));
            });
            return 1;
        });
        builder.then(ClientCommands.literal("reload")
            .executes(context -> {
                sendFeedback(context.getSource(), "Reloading all modules...");
                ModuleLoader.getInstance().reloadAll();
                sendFeedback(context.getSource(), "Done.");
                return 1;
            })
        );
    }
}
