package com.logy.pantheon.features.commands.speedtyping;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.hangman.WordLoader;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;

public class SpeedTypingGame implements GameInstance {
    private boolean active = false;
    private String targetWord = "";
    private long startTime = 0;

    private static long globalCooldown = 0;
    private static final long COOLDOWN_MS = 60000; // 1 minuta
    private static final int REWARD = 10;

    public void start() {
        long now = System.currentTimeMillis();
        if (now < globalCooldown) {
            long remaining = (globalCooldown - now) / 1000;
            ChatUtils.sendPartyMessage("SpeedTyping is on cooldown! Wait " + remaining + "s.");
            return;
        }

        var wordObj = WordLoader.getRandomWord();
        if (wordObj == null) return;

        targetWord = wordObj.word.toLowerCase();
        active = true;
        startTime = now;

        ChatUtils.sendPartyMessage("QUICK TYPING! First to type: " + targetWord);
    }

    @Override
    public void update() {
        if (!active) return;

        if (System.currentTimeMillis() - startTime > 10000) {
            ChatUtils.sendPartyMessage("Typing game timed out! Nobody was fast enough.");
            setGlobalCooldown();
            stop();
        }
    }

    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;

        if (!message.trim().equalsIgnoreCase(targetWord)) return;

        long timeTakenMs = System.currentTimeMillis() - startTime;
        double seconds = timeTakenMs / 1000.0;

        ChatUtils.sendPartyMessage("GG! Rewarded " + sender + " " + REWARD + " coins for typing it in " + String.format("%.2fs", seconds) + "!");

        Economy.addMoney(sender, REWARD);

        setGlobalCooldown();
        stop();
    }

    private void setGlobalCooldown() {
        globalCooldown = System.currentTimeMillis() + COOLDOWN_MS;
    }

    @Override
    public void stop() {
        active = false;
        targetWord = "";
    }

    @Override
    public boolean isActive() {
        return active;
    }
}