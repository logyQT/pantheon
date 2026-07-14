package com.logy.pantheon.utils;


import com.logy.pantheon.config.ModuleConfig;
import com.logy.pantheon.config.ModuleRegistry;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.features.commands.main.CommandManager;
import com.logy.pantheon.features.commands.main.FunCommands;
import com.logy.pantheon.features.commands.scripting.ModuleLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class ChatUtils {

    private static final Minecraft client = Minecraft.getInstance();

    private record QueueEntry(String message, boolean confirmable) {}
    private static final Queue<QueueEntry> queue = new LinkedList<>();
    private static long lastSentTime = 0;
    private static QueueEntry pendingEntry = null;
    private static long pendingTimestamp = 0;
    private static int pendingRetries = 0;

    private static String username = null;
    private static Consumer<String> chatCallback;

    public static void setChatCallback(Consumer<String> callback) {
        chatCallback = callback;
    }

    public static String getUsername() {
        if (username == null) {
            username = Minecraft.getInstance().getUser().getName();
        }
        return username;
    }

    public static void updateSentTime() {
        lastSentTime = System.currentTimeMillis();
    }

    private static int getMaxRetries() {
        return ModuleConfig.get("general").getInt("message_que_max_retries");
    }

    public static void queMessage(String message) {
        if(message.length()>=200) {
            LOGGER.info("Message too long " + message);
            return;
        }
        queue.add(new QueueEntry(message, true));
        LOGGER.debug("[Queue] +1 confirmable (size={}): {}", queue.size(), message);
    }

    public static void queCommand(String command) {
        if(command.length()>=200) {
            LOGGER.info("Command too long " + command);
            return;
        }
        queue.add(new QueueEntry(command, false));
        LOGGER.debug("[Queue] +1 fire-and-forget (size={}): {}", queue.size(), command);
    }

    public static void tickQueue() {
        long now = System.currentTimeMillis();

        if (pendingEntry != null) {
            if (pendingEntry.confirmable() && now - pendingTimestamp > 3000) {
                pendingRetries++;
                if (pendingRetries > getMaxRetries()) {
                    LOGGER.warn("[Queue] Dropping after {} retries: {}", getMaxRetries(), pendingEntry.message());
                    queue.poll();
                    pendingEntry = null;
                    pendingRetries = 0;
                    return;
                }
                LOGGER.debug("[Queue] Retry {}/{}: {}", pendingRetries, getMaxRetries(), pendingEntry.message());
                executeCommand(pendingEntry.message());
                pendingTimestamp = now;
            } else if (pendingEntry.confirmable()) {
                LOGGER.trace("[Queue] Waiting for confirmation ({}ms remain)", 3000 - (now - pendingTimestamp));
            }
            return;
        }

        if (queue.isEmpty()) return;
        if (now - lastSentTime <= getMessageQueCooldownMs()) return;

        QueueEntry entry = queue.peek();
        if (entry == null) return;

        if (!entry.confirmable()) {
            queue.poll();
            LOGGER.debug("[Queue] Executing fire-and-forget: {}", entry.message());
            executeCommand(entry.message());
            lastSentTime = now;
            return;
        }

        pendingEntry = entry;
        LOGGER.debug("[Queue] Sending confirmable (will wait for echo): {}", entry.message());
        executeCommand(entry.message());
        pendingTimestamp = now;
        lastSentTime = now;
    }

    private static void confirmMessage(String chatMessage) {
        if (pendingEntry == null || !pendingEntry.confirmable()) return;

        String text = pendingEntry.message();
        if (text.startsWith("pc ")) text = text.substring(3);
        else if (text.startsWith("gc ")) text = text.substring(3);
        else if (text.startsWith("ac ")) text = text.substring(3);

        text = text.trim();

        if (chatMessage.contains(text)) {
            LOGGER.debug("[Queue] Confirmed match \"{}\" -> {}", text, chatMessage);
            queue.poll();
            pendingEntry = null;
            pendingRetries = 0;
        } else {
            LOGGER.trace("[Queue] No match for \"{}\" in \"{}\"", text, chatMessage);
        }
    }

    public static int getMessageQueCooldownMs(){
        return ModuleConfig.get("general").getInt("message_que_cooldown_ms");
    }

    private static void executeCommand(String msg) {
        if (client.player == null) {
            LOGGER.warn("[Queue] Cannot send (player null): {}", msg);
            return;
        }
        LOGGER.debug("[Queue] -> sendCommand: {}", msg);
        client.player.connection.sendCommand(msg);
    }

    public static void sendCommand(String cmd) {queCommand(cmd);}

    public static void sendFeedback(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§6[Pantheon] §f" + message));
        }
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

    public static void register() {
        ModuleRegistry.registerModule("general", "General", "Main", "java", false);
        ModuleRegistry.registerSetting("general", SettingDefinition.text("prefix", "Command Prefix", "!"));
        ModuleRegistry.registerSetting("general", SettingDefinition.slider("message_que_cooldown_ms", "Message Queue Cooldown", 100, 1000, 10, 1000));
        ModuleRegistry.registerSetting("general", SettingDefinition.slider("message_que_max_retries", "Max Retries", 0, 10, 1, 3));
    }

    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatUtils.tickQueue();
            if(CommandManager.isGameRunning()) CommandManager.update();
            ModuleLoader.getInstance().tickAll();
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
        if (chatCallback != null) chatCallback.accept(message);
        confirmMessage(message);
        if (stripFormatting(message).contains(getUsername())) {
            updateSentTime();
        }
        FunCommands.processMessage(message);
    }

}

