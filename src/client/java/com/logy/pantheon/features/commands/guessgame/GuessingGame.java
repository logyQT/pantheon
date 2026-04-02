package com.logy.pantheon.features.commands.guessgame;

import java.util.Random;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.NumberUtils;
import com.logy.pantheon.utils.TimeUtils;

public class GuessingGame implements GameInstance {
    private boolean active = false;
    private int targetNumber = 0;
    private int attempts = 0;
    private static final Random random = new Random();
    private long lastActivity = 0;
    private long startTime = 0;
    private String starter = null;

    private static final int ENTRY_COST = 10;
    private static final int MIN_PAYOUT = 20;
    private static final long COOLDOWN_MS = 2 * 60 * 1000L;
    private static long lastUsed = 0;

    private static int calculatePayout(int attempts, long elapsedMs) {
        int payout = MIN_PAYOUT;

        if (attempts <= 3) payout = 100;
        else if (attempts <= 6) payout = 60;
        else if (attempts <= 10) payout = 40;

        long seconds = elapsedMs / 1000;
        if (seconds < 10) payout += 30;
        else if (seconds < 20) payout += 15;
        else if (seconds < 30) payout += 5;

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
        if (elapsed < COOLDOWN_MS) {
            ChatUtils.sendPartyMessage("Guessing game is on cooldown! Wait " + TimeUtils.formatDuration(COOLDOWN_MS-elapsed));
            return;
        }

        if (!Economy.takeMoney(sender, ENTRY_COST)) {
            ChatUtils.sendPartyMessage(
                    sender + " can't afford to start the game! (costs " + ENTRY_COST + " coins, balance: " + Economy.getCurrentBalance(sender) + ")"
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
                sender + " started a guessing game! (-" + ENTRY_COST + " coins) | Pick a number (1-100). Winner gets at least " + MIN_PAYOUT + " coins!"
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

        if (System.currentTimeMillis() - lastActivity > 10000) {
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