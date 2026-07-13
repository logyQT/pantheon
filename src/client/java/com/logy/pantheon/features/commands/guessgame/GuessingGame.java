package com.logy.pantheon.features.commands.guessgame;

import java.util.Random;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.NumberUtils;
import com.logy.pantheon.utils.TimeUtils;

public class GuessingGame implements GameInstance {
    private static final PantheonConfig CONFIG = PantheonConfig.get();
    private boolean active = false;
    private int targetNumber = 0;
    private int attempts = 0;
    private static final Random random = new Random();
    private long lastActivity = 0;
    private long startTime = 0;
    private String starter = null;

    private static long lastUsed = 0;

    private int calculatePayout(int attempts, long elapsedMs) {
        int payout = CONFIG.GUESS_PAYOUT_10;

        if (attempts <= 3) payout = CONFIG.GUESS_PAYOUT_3;
        else if (attempts <= 6) payout = CONFIG.GUESS_PAYOUT_6;

        long seconds = elapsedMs / 1000;
        if (seconds < 10) payout += CONFIG.GUESS_SPEED_BONUS_10S;
        else if (seconds < 20) payout += CONFIG.GUESS_SPEED_BONUS_20S;

        return payout;
    }

    private static final String[] HIGHER_MESSAGES = {
            "%d is a good guess, but you gotta aim higher!",
            "Nope, %d is too low. Try a bigger number.",
            "Go up! %d isn't enough.",
            "Think bigger than %d!"
    };

    private static final String[] LOWER_MESSAGES = {
            "%d? Too much! Aim lower.",
            "You went overboard with %d. Try something smaller.",
            "Lower! %d is too high.",
            "Dial it back, %d is way above the target."
    };

    public void start(String sender) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed;
        if (elapsed < CONFIG.GUESS_COOLDOWN_MS) {
            ChatUtils.sendPartyMessage("Guessing game is on cooldown! Wait " + TimeUtils.formatDuration(CONFIG.GUESS_COOLDOWN_MS - elapsed));
            return;
        }

        if (!Economy.takeMoney(sender, CONFIG.GUESS_ENTRY_COST)) {
            ChatUtils.sendPartyMessage(
                    sender + " can't afford to start the game! (costs " + CONFIG.GUESS_ENTRY_COST + " coins, balance: " + Economy.getCurrentBalance(sender) + ")"
            );
            return;
        }

        targetNumber = NumberUtils.getRandomNumber(1, 100);
        attempts = 0;
        active = true;
        startTime = System.currentTimeMillis();
        lastActivity = startTime;
        starter = sender;

        lastUsed = now;
        ChatUtils.sendPartyMessage(
                sender + " started a guessing game! (-" + CONFIG.GUESS_ENTRY_COST + " coins) | Pick a number (1-100). Winner gets at least " + CONFIG.GUESS_PAYOUT_10 + " coins!"
        );
    }

    @Override public void stop() {
        active = false;
        starter = null;
    }

    @Override public boolean isActive() {
        return active;
    }

    @Override public void update() {
        if (!active) return;

        if (System.currentTimeMillis() - lastActivity > CONFIG.GUESS_TIMEOUT_MS) {
            ChatUtils.sendPartyMessage("Guessing game was canceled due to inactivity!");
            stop();
        }
    }

    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;

        try {
            int guess = Integer.parseInt(message.trim());
            attempts++;
            lastActivity = System.currentTimeMillis();

            if (guess < targetNumber) {
                String template = HIGHER_MESSAGES[random.nextInt(HIGHER_MESSAGES.length)];
                ChatUtils.sendPartyMessage(String.format(template, guess));
            } else if (guess > targetNumber) {
                String template = LOWER_MESSAGES[random.nextInt(LOWER_MESSAGES.length)];
                ChatUtils.sendPartyMessage(String.format(template, guess));
            } else {
                long elapsed = System.currentTimeMillis() - startTime;
                int payout = calculatePayout(attempts, elapsed);

                Economy.addMoney(sender, payout);
                ChatUtils.sendPartyMessage(
                        "GG! " + sender + " found " + targetNumber + " in " + attempts +
                                " tries (" + (elapsed / 1000) + "s)! Won " + payout + " coins! | Balance: " + Economy.getCurrentBalance(sender)
                );
                stop();
            }
        } catch (NumberFormatException ignored) {}
    }
}