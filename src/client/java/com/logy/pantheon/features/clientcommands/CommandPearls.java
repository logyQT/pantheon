package com.logy.pantheon.features.clientcommands;

import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.utils.NumberUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

public class CommandPearls extends ClientCommandBase {

    public static void register() {
        ModuleRegistry.registerModule("auto_pearls", "Auto Pearls", "Main", "java", false);
        ModuleRegistry.registerSetting("auto_pearls", SettingDefinition.bool("enabled", "Enabled", true));
    }

    private static final ModuleConfig CONFIG = ModuleConfig.get("auto_pearls");

    private static final int TARGET_PEARLS = 16;

    private static final String[] messages = {"Starting in 3 seconds.", "Starting in 2 seconds.", "Starting in 1 second."};
    private static int current_message_index = 0;
    private static long canAutoBuyTime = 0;
    private static final long autoBuyTimeoutMs = 5000;

    public static void check(String message){
        if(!canAutoBuy()) return;
        if(!CONFIG.getBool("enabled")) return;
        if(!message.startsWith(messages[current_message_index])) return;
        current_message_index = NumberUtils.getRandomNumber(0, messages.length-1);
        canAutoBuyTime = now() + autoBuyTimeoutMs;
        buyPearls();
    }

    private static boolean canAutoBuy(){
        return now() >= canAutoBuyTime;
    }

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

    private static void buyPearls() {

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