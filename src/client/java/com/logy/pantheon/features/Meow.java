package com.logy.pantheon.features;

import com.logy.pantheon.utils.ChatUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class Meow {

    public static void handleChat(String rawText) {
        String cleanText = ChatUtils.stripFormatting(rawText).toLowerCase();

        if (cleanText.contains("meow")) {
            playMeow();
        }
    }

    private static void playMeow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.CAT_AMBIENT, 1.0F, 1.0F);
        }
    }
}
