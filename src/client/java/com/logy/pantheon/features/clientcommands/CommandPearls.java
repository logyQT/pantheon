package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.config.ModMenuIntegration;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public class CommandPearls extends ClientCommandBase {

    private static final int TARGET_PEARLS = 16;

    @Override
    public String getName() {
        return "pearls";
    }

    @Override
    protected void build(LiteralArgumentBuilder<FabricClientCommandSource> builder) {
        builder.executes(context -> {
            buyPearls();
            return 1;
        });
    }

    private void buyPearls() {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        int currentPearls = countPearls(mc);

        if (currentPearls % TARGET_PEARLS == 0 && currentPearls != 0) return;

        int needed = TARGET_PEARLS - (currentPearls % TARGET_PEARLS);

        mc.getConnection().sendCommand("gfs ender_pearl " + needed);
    }

    private static int countPearls(Minecraft mc) {
        if (mc.player == null) return 0;
        var inventory = mc.player.getInventory();
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getHoverName().getString().trim().equalsIgnoreCase("ender pearl")) {
                count += stack.getCount();
            }
        }
        return count;
    }
}