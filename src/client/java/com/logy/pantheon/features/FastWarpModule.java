package com.logy.pantheon.features;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FastWarpModule {

    private static long canWarpTime = 0;

    private static final PantheonConfig CONFIG = PantheonConfig.get();
    private static KeyMapping warpKey;
    public static final KeyMapping.Category PANTHEON_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pantheon", "modules"));

    public static void init() {
        warpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pantheon.fastwarp",
                GLFW.GLFW_KEY_V,
                PANTHEON_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) return;
            while (warpKey.consumeClick()) {
                if(canWarp()) handleWarp();
            }
        });
        canWarpTime = now();
    }

    private static boolean canWarp(){
        if(CONFIG.FASTWARP_PTC_PROTECTION_MS == 0) return true;
        return now() >= canWarpTime;
    }

    private static void handleWarp() {
        if (ScoreboardAreaMatcher.isInArea(CONFIG.FASTWARP_AREA_ONE)) {
            ChatUtils.sendCommand(CONFIG.FASTWARP_CMD_ONE);
        } else if (ScoreboardAreaMatcher.isInArea(CONFIG.FASTWARP_AREA_TWO)) {
            ChatUtils.sendCommand(CONFIG.FASTWARP_CMD_TWO);
        }
        canWarpTime = now() + CONFIG.FASTWARP_PTC_PROTECTION_MS;
    }

    private static long now(){
        return System.currentTimeMillis();
    }
}
