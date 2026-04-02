package com.logy.pantheon.features.commands.wordchain;

import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.utils.ChatUtils;
import java.util.*;

public class WordChainGame implements GameInstance {
    private boolean active = false;
    private boolean registrationOpen = false;
    private String lastWord = "";
    private String requiredPrefix = "";
    private int chainCount = 0;
    private long lastActivityTime = 0;

    private long currentTimeoutLimit = 20000;
    private static final double TIMEOUT_REDUCTION_FACTOR = 0.95;
    private static final int REGISTRATION_FEE = 100;

    private final Set<String> usedWords = new HashSet<>();
    private final List<String> players = new ArrayList<>();
    private final Map<String, Integer> wordsContributed = new HashMap<>();
    private int currentPlayerIndex = 0;

    public void start() {
        if (active) return;

        active = true;
        registrationOpen = true;
        chainCount = 0;
        currentTimeoutLimit = 20000;
        usedWords.clear();
        players.clear();
        wordsContributed.clear();

        ChatUtils.sendPartyMessage("WORD CHAIN! Registration open (10s). Type .reg to join (Cost: " + REGISTRATION_FEE + ")");
        resetTimer();
    }

    private void beginGame() {
        resetTimer();
        registrationOpen = false;
        if (players.size() < 2) {
            ChatUtils.sendPartyMessage("Not enough players to start. Game cancelled.");
            stop();
            return;
        }

        Collections.shuffle(players);

        String startWord = WordChainLoader.getRandomStartWord();
        usedWords.add(startWord.toUpperCase());
        updateRequirement(startWord);

        currentPlayerIndex = 0;
        ChatUtils.sendPartyMessage("Game starts! Order: " + String.join(" -> ", players));
        ChatUtils.sendPartyMessage("Starting word: " + startWord);
        announceTurn();
    }

    @Override
    public void handleChat(String sender, String message) {
        if (!active) return;
        String content = message.trim().toUpperCase();

        if (registrationOpen && content.equals(".REG")) {
            handleRegistration(sender);
            return;
        }

        if (players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return;
        }

        String activePlayer = players.get(currentPlayerIndex);
        if (!sender.equalsIgnoreCase(activePlayer)) return;

        if (!content.startsWith(requiredPrefix)) return;

        if (!WordChainLoader.isValidWord(content)) return;

        if (usedWords.contains(content)) {
            ChatUtils.sendPartyMessage("Word '" + content + "' was already used!");
            return;
        }

        processValidGuess(sender, content);
    }

    private void handleRegistration(String sender) {
        if (players.contains(sender)) return;

        if (!Economy.hasEnough(sender, REGISTRATION_FEE)) {
            ChatUtils.sendPartyMessage(sender + ", you need " + REGISTRATION_FEE + " coins to join!");
            return;
        }

        if (Economy.takeMoney(sender, REGISTRATION_FEE)) {
            players.add(sender);
            wordsContributed.put(sender, 0);
            ChatUtils.sendPartyMessage(sender + " joined the chain! (Paid " + REGISTRATION_FEE + ")");
        } else {
            ChatUtils.sendPartyMessage(sender + ", transaction failed! Check your balance.");
        }
    }

    private void processValidGuess(String sender, String word) {
        usedWords.add(word);
        chainCount++;
        wordsContributed.put(sender, wordsContributed.get(sender) + 1);

        updateRequirement(word);

        currentTimeoutLimit = (long) (currentTimeoutLimit * TIMEOUT_REDUCTION_FACTOR);
        if (currentTimeoutLimit < 3000) currentTimeoutLimit = 3000;

        ChatUtils.sendPartyMessage("Next: '" + requiredPrefix + "' | Time: " + (currentTimeoutLimit / 1000.0) + "s");

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        announceTurn();
        resetTimer();
    }

    private void announceTurn() {
        ChatUtils.sendPartyMessage("It's " + players.get(currentPlayerIndex) + "'s turn!");
    }

    @Override
    public void update() {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - lastActivityTime;

        if (registrationOpen) {
            if (elapsed > 11000) beginGame();
            return;
        }

        if (elapsed > currentTimeoutLimit) {
            // 1. Identify loser before removing
            String loser = players.get(currentPlayerIndex);
            ChatUtils.sendPartyMessage(loser + " failed to answer in time and is ELIMINATED!");

            // 2. Remove the player
            players.remove(currentPlayerIndex);

            // 3. Check for game end IMMEDIATELY
            if (players.size() <= 1) {
                finishGame();
                return; // CRITICAL: Stop execution here so we don't announce a turn
            }

            if (currentPlayerIndex >= players.size()) {
                currentPlayerIndex = 0;
            }

            // 5. Reset for the next person
            String newStart = WordChainLoader.getRandomStartWord();
            updateRequirement(newStart);
            ChatUtils.sendPartyMessage("New starting word: " + newStart + " (Next: " + requiredPrefix + ")");

            announceTurn();
            resetTimer();
        }
    }

    private void finishGame() {
        String winner = players.isEmpty() ? "No one" : players.get(0);
        ChatUtils.sendPartyMessage("GAME OVER! Last man standing: " + winner);
        ChatUtils.sendPartyMessage("Total Chain: " + chainCount);

        wordsContributed.forEach((name, count) -> {
            if (count > 0) {
                int reward = count * (50 + (chainCount * 2));

                if (name.equalsIgnoreCase(winner)) {
                    reward *= 2;
                }

                Economy.addMoney(name, reward);
                ChatUtils.sendPartyMessage(name + " earned " + reward + " coins.");
            }
        });

        stop();
    }

    private void updateRequirement(String word) {
        if (word == null || word.isEmpty()) {
            this.requiredPrefix = "A";
            this.lastWord = "ERR";
            return;
        }
        this.requiredPrefix = word.substring(word.length() - 1).toUpperCase();
        this.lastWord = word;
    }

    @Override public boolean isActive() { return active; }
    @Override public void stop() { active = false; registrationOpen = false; }
    private void resetTimer() { lastActivityTime = System.currentTimeMillis(); }
}