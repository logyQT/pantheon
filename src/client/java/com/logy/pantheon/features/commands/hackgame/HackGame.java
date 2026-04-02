package com.logy.pantheon.features.commands.hackgame;

import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HackGame implements GameInstance {
    private boolean active = false;
    private String password = "";
    private String controlSum = "";
    private long startTime = 0;
    private static long globalCooldown = 0;

    private final Map<String, Integer> playerAttempts = new HashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final int REWARD = 100;
    private static final long GAME_TIMEOUT_MS = 18000;
    private static final long COOLDOWN_MS = 600000;

    public void start() {
        if (active) return;

        long now = System.currentTimeMillis();
        if (now < globalCooldown) {
            String remaining = getRemaining(now);
            ChatUtils.sendPartyMessage("Hack game is on cooldown! Wait " + remaining);
            return;
        }

        int n1 = NumberUtils.getRandomNumber(1, 9);
        int n2 = NumberUtils.getRandomNumber(1, 9);

        this.password = n1 + " " + n2;

        int sum = n1 + n2;
        int product = n1 * n2;
        this.controlSum = String.valueOf(sum) + product;

        this.active = true;
        this.startTime = now;
        this.playerAttempts.clear();

        ChatUtils.sendPartyMessage("HACK INITIALIZED! Decrypt the password.");
        ChatUtils.sendPartyMessage("Password: _ _ | Control sum: " + controlSum);
        ChatUtils.sendPartyMessage("Time: 15s | Attempts: 3 | Format: X Y");
    }

    private static @NotNull String getRemaining(long now) {
        long totalSeconds = (globalCooldown - now) / 1000;

        if (totalSeconds <= 0) return "0 seconds";

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        }

        if (seconds > 0 || minutes == 0) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString().trim();
    }

    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;

        String guess = message.trim();

        if (!guess.matches("\\d\\s\\d"))  return;

        int attempts = playerAttempts.getOrDefault(sender, 0) + 1;
        if (attempts > MAX_ATTEMPTS) return;
        playerAttempts.put(sender, attempts);

        if (guess.equals(password)) {
            ChatUtils.sendPartyMessage("✔ ACCESS GRANTED! " + sender + " decrypted " + password + " | Reward: " + REWARD + " coins");
            Economy.addMoney(sender, REWARD);
            setGlobalCooldown();
            stop();
        } else {
            if (attempts == MAX_ATTEMPTS) {
                ChatUtils.sendPartyMessage(sender + " failed 3 attempts!");
            }
        }
    }

    @Override
    public void update() {
        if (active && System.currentTimeMillis() - startTime > GAME_TIMEOUT_MS) {
            ChatUtils.sendPartyMessage("HACK FAILED! Connection timed out. The password was: " + password);
            setGlobalCooldown();
            stop();
        }
    }

    private void setGlobalCooldown() {
        globalCooldown = System.currentTimeMillis() + COOLDOWN_MS;
    }

    @Override public boolean isActive() { return active; }
    @Override public void stop() { active = false; }
}