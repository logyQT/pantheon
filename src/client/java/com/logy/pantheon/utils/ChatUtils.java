package com.logy.pantheon.utils;


import com.logy.pantheon.features.Meow;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.FunCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ChatUtils {

    private static final Minecraft client = Minecraft.getInstance();

    private static final Queue<String> messageQueue = new LinkedList<>();
    private static long lastSentTime = 0;

    public static void queMessage(String message) {
        if(message.length()>=200) {
            LOGGER.info("Message too long" + message);
            return;
        }
        messageQueue.add(message);
    }

    public static void tickQueue() {
        long now = System.currentTimeMillis();

        if (messageQueue.isEmpty()) return;
        if (now - lastSentTime <= 750) return;

        String msg = messageQueue.poll();
        sendCommand(msg);
        lastSentTime = now;
    }

    public static void updateSentTime(){
        lastSentTime = System.currentTimeMillis();
    }

    private static void sendCommand(String msg) {
        if (client.player == null) return;
        client.player.connection.sendCommand(msg);
    }

    public static void sendPartyMessage(String message){
        queMessage("pc " + message);
    }
    public static void sendGuildMessage(String message){
        queMessage("gc " + message);
    }
    public static void sendAllMessage(String message){
        queMessage("ac " + message);
    }

    public static @NotNull String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)§[0-9A-FK-ORX]", "");
    }

    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatUtils.tickQueue();
            if(CommandManager.isGameRunning()) CommandManager.update();
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            messageHandler(message.getString());
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if(overlay) return;
            messageHandler(message.getString());
        });
    }

    private static void messageHandler(String message){
        FunCommands.processMessage(message);
        Meow.handleChat(message);
    }

}

