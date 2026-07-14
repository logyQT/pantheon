package com.logy.pantheon.features;

import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.ScoreboardAreaMatcher;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class FastWarpModule {

    private static long canWarpTime = 0;

    private static final ModuleConfig CONFIG = ModuleConfig.get("fast_teleport");
    private static KeyMapping warpKey;
    public static final KeyMapping.Category PANTHEON_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pantheon", "modules"));

    public static void register() {
        ModuleRegistry.registerModule("fast_teleport", "Fast Teleport", "Main", "java", false);
        ModuleRegistry.registerSetting("fast_teleport", SettingDefinition.slider("ptc_protection_ms", "Warp PTC Protection", 0, 5000, 100, 5000));
        ModuleRegistry.registerSetting("fast_teleport", SettingDefinition.text("area_one", "Warp Area 1", "dragon's nest"));
        ModuleRegistry.registerSetting("fast_teleport", SettingDefinition.text("cmd_one", "Warp Cmd 1", "warp top"));
        ModuleRegistry.registerSetting("fast_teleport", SettingDefinition.text("area_two", "Warp Area 2", "spider mound"));
        ModuleRegistry.registerSetting("fast_teleport", SettingDefinition.text("cmd_two", "Warp Cmd 2", "warp drag"));
    }

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
        if(CONFIG.getInt("ptc_protection_ms") == 0) return true;
        return now() >= canWarpTime;
    }

    private static void handleWarp() {
        if (ScoreboardAreaMatcher.isInArea(CONFIG.getString("area_one"))) {
            ChatUtils.sendCommand(CONFIG.getString("cmd_one"));
        } else if (ScoreboardAreaMatcher.isInArea(CONFIG.getString("area_two"))) {
            ChatUtils.sendCommand(CONFIG.getString("cmd_two"));
        }
        canWarpTime = now() + CONFIG.getInt("ptc_protection_ms");
    }

    private static long now(){
        return System.currentTimeMillis();
    }
}
