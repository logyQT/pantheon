package com.logy.pantheon.features.commands.mathgame;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import java.util.Random;

public class MathGame implements GameInstance {
    private boolean active = false;
    private int result = 0;
    private long startTime = 0;
    private final Random random = new Random();

    private static long globalCooldown = 0;
    private static final long COOLDOWN_MS = 60000;
    private static final int REWARD = 10;

    public void start() {
        if (active) return;

        long now = System.currentTimeMillis();
        if (now < globalCooldown) {
            long remaining = (globalCooldown - now) / 1000;
            ChatUtils.sendPartyMessage("MathGame is on cooldown! Wait " + remaining + "s.");
            return;
        }

        int patternType = random.nextInt(5);
        String equation = "";

        int a = random.nextInt(12) + 2;
        int b = random.nextInt(12) + 2;
        int c = random.nextInt(10) + 2;

        switch (patternType) {
            case 0 -> {
                equation = "(" + a + " + " + b + ") x " + c;
                result = (a + b) * c;
            }
            case 1 -> {
                while ((a * b) <= c) { a++; b++; }
                equation = a + " x " + b + " - " + c;
                result = (a * b) - c;
            }
            case 2 -> {
                equation = a + " + " + b + " + " + c;
                result = a + b + c;
            }
            case 3 -> {
                if (b > a) { int temp = a; a = b; b = temp; }
                if (a == b) a++;
                equation = "(" + a + " - " + b + ") + " + c;
                result = (a - b) + c;
            }
            case 4 -> {
                int s1 = random.nextInt(5) + 2;
                int s2 = random.nextInt(5) + 2;
                int s3 = random.nextInt(5) + 2;
                equation = s1 + " x " + s2 + " x " + s3;
                result = s1 * s2 * s3;
            }
        }

        active = true;
        resetTimer();
        ChatUtils.sendPartyMessage("MATH! Solve: " + equation + " = ?");
    }

    private void resetTimer(){
        startTime = System.currentTimeMillis();
    }

    @Override
    public void handleChat(String sender, String input) {
        if (!active) return;

        String cleanInput = input.trim();
        if (cleanInput.isEmpty() || !cleanInput.matches("-?\\d+")) return;

        try {
            int guess = Integer.parseInt(cleanInput);
            if (guess != result) return;

            long timeTakenMs = System.currentTimeMillis() - startTime;
            double seconds = timeTakenMs / 1000.0;

            ChatUtils.sendPartyMessage("GG! Rewarded " + sender + " " + REWARD + " coins for solving it in " + String.format("%.2fs", seconds) + "! Answer: " + result);

            Economy.addMoney(sender, REWARD);

            setGlobalCooldown();
            stop();
        } catch (NumberFormatException ignored) {}
    }

    private void setGlobalCooldown() {
        globalCooldown = System.currentTimeMillis() + COOLDOWN_MS;
    }

    @Override public void stop() { active = false; }
    @Override public boolean isActive() { return active; }

    @Override public void update() {
        if(!active) return;

        if(System.currentTimeMillis() - startTime > 10000){
            ChatUtils.sendPartyMessage("Nobody guessed! The solution was: " + result);
            setGlobalCooldown(); // CD po timeout też się przyda
            stop();
        }
    }
}