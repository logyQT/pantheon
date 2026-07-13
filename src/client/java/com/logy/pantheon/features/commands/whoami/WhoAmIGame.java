package com.logy.pantheon.features.commands.whoami;

import com.logy.pantheon.config.PantheonConfig;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import com.logy.pantheon.utils.TimeUtils;

import java.util.List;
import java.util.Random;

public class WhoAmIGame implements GameInstance {

    private static final PantheonConfig CONFIG = PantheonConfig.get();

    // ── Cooldown ─────────────────────────────────────────────
    private static long lastUsed = 0;

    // ── State ────────────────────────────────────────────────
    private boolean active = false;
    private WhoAmIEntry current = null;
    private int stage = 0; // 0=hint1, 1=hint1+letter, 2=hint2, 3=definition
    private long stageStart = 0;

    private static final Random random = new Random();

    // ── GameInstance ─────────────────────────────────────────
    @Override public boolean isActive() { return active; }

    @Override
    public void stop() {
        active  = false;
        current = null;
        stage   = 0;
    }

    @Override
    public void update() {
        if (!active) return;

        if (System.currentTimeMillis() - stageStart >= CONFIG.WHOAMI_HINT_TIMEOUT_MS) {
            advanceStage();
        }
    }

    // ── Start ────────────────────────────────────────────────
    public boolean start(String sender) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastUsed;

        if (elapsed < CONFIG.WHOAMI_COOLDOWN_MS) {
            ChatUtils.sendPartyMessage("WhoAmI is on cooldown! (" + TimeUtils.formatDuration(CONFIG.WHOAMI_COOLDOWN_MS - elapsed) + " remaining)");
            return false;
        }

        List<WhoAmIEntry> entries = WhoAmILoader.getEntries();
        if (entries.isEmpty()) {
            ChatUtils.sendPartyMessage("Error: No WhoAmI entries loaded.");
            return false;
        }

        if (!Economy.takeMoney(sender, CONFIG.WHOAMI_ENTRY_COST)) {
            ChatUtils.sendPartyMessage(
                    sender + " can't afford to start! (costs " + CONFIG.WHOAMI_ENTRY_COST +
                            " coins, balance: " + Economy.getCurrentBalance(sender) + ")"
            );
            return false;
        }

        current    = entries.get(random.nextInt(entries.size()));
        stage      = 0;
        active     = true;
        stageStart = now;
        lastUsed   = now;

        ChatUtils.sendPartyMessage("WhoAmI? started! (-" + CONFIG.WHOAMI_ENTRY_COST + " coins)");
        printStage();
        return true;
    }

    // ── Chat handler ─────────────────────────────────────────
    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;

        if (message.trim().equalsIgnoreCase(current.name())) {
            int payout = payoutForStage();
            Economy.addMoney(sender, payout);
            ChatUtils.sendPartyMessage(
                    "✔ " + sender + " guessed it! The answer was \"" + current.name() + "\"" +
                            " | +" + payout + " coins | Balance: " + Economy.getCurrentBalance(sender)
            );
            stop();
        } else {
        }
    }

    // ── Internal helpers ─────────────────────────────────────
    private void advanceStage() {
        stage++;
        stageStart = System.currentTimeMillis();

        if (stage > 3) {
            ChatUtils.sendPartyMessage("Nobody guessed it! The answer was: \"" + current.name() + "\"");
            stop();
            return;
        }
        printStage();
    }

    private void printStage() {
        String name = current.name();
        switch (stage) {
            case 0 -> ChatUtils.sendPartyMessage("Hint: " + current.hintOne());
            case 1 -> ChatUtils.sendPartyMessage(
                    "First letter: " + name.charAt(0) + " | Letters: " + name.length() +
                            " | Pattern: " + letterPattern(name)
            );
            case 2 -> ChatUtils.sendPartyMessage("Hint 2: " + current.hintTwo());
            case 3 -> ChatUtils.sendPartyMessage("Definition: " + current.definition());
        }
    }

    /** e.g. "Ender Pearl" → "E____ P____" */
    private static String letterPattern(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == ' ') sb.append(' ');
            else if (sb.isEmpty() || name.charAt(0) == c && sb.length() == 0) sb.append(c);
            else sb.append('_');
        }
        return sb.toString();
    }

    private int payoutForStage() {
        return switch (stage) {
            case 0, 1 -> CONFIG.WHOAMI_PAYOUT_FAST;
            case 2    -> CONFIG.WHOAMI_PAYOUT_MID;
            default   -> CONFIG.WHOAMI_PAYOUT_SLOW;
        };
    }
}