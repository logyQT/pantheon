package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.config.ModMenuIntegration;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
                client.setScreen(ModMenuIntegration.createConfigScreen(null));
            });
            return 1;
        });
    }
}
