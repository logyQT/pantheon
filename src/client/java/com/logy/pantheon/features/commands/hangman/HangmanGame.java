package com.logy.pantheon.features.commands.hangman;

import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HangmanGame implements GameInstance {
    private boolean active = false;
    private HangmanWord currentWord;
    private List<Character> guessedLetters = new ArrayList<>();
    private int lives = 6;
    private int wrongGuesses = 0;
    private final Random random = new Random();
    private long lastActivityTime = 0;

    public void start() {
        resetTimer();
        currentWord = WordLoader.getRandomWord();
        guessedLetters.clear();
        lives = 6;
        wrongGuesses = 0;
        active = true;

        ChatUtils.sendPartyMessage("Hangman started! Topic: " + currentWord.category);
        ChatUtils.sendPartyMessage("Word: " + getHiddenWordDisplay());
    }

    private void handleWrongGuess(String input) {
        lives--;
        wrongGuesses++;

        if (lives <= 0) {
            ChatUtils.sendPartyMessage("GAME OVER! The word was: " + currentWord.word);
            stop();
            return;
        }

        if (wrongGuesses == 2) {
            ChatUtils.sendPartyMessage("HINT (Subcategory): " + currentWord.subcategory);
        }
        if (lives == 1) {
            ChatUtils.sendPartyMessage("FINAL HINT (Definition): " + currentWord.definition);
        }

        ChatUtils.sendPartyMessage("Wrong! " + lives + " lives left. Word: " + getHiddenWordDisplay());
    }

    private void handleCorrectLetter(String sender) {
        String hidden = getHiddenWordDisplay();
        if (!hidden.contains("_")) {
            handleWin(sender);
        } else {
            ChatUtils.sendPartyMessage("Correct! Word: " + hidden);
        }
    }

    private void handleWin(String winner) {
        ChatUtils.sendPartyMessage("GG! " + winner + " guessed the word: " + currentWord.word);
        stop();
    }

    private String getHiddenWordDisplay() {
        StringBuilder builder = new StringBuilder();
        for (char c : currentWord.word.toCharArray()) {
            if (c == ' ') builder.append("  ");
            else builder.append(guessedLetters.contains(c) ? c + " " : "_ ");
        }
        return builder.toString().trim();
    }

    private void resetTimer() {
        lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void stop() { active = false; }

    @Override
    public void handleChat(String sender, String message) {
        if(!active || !message.startsWith(".")) return;
        var guess = message.substring(1).trim().toUpperCase();
        if(guess.isEmpty()) return;

        resetTimer();

        boolean isFullWord = guess.length() > 1;

        if (isFullWord && guess.equals(currentWord.word)) {
            handleWin(sender);
            return;
        }

        char letter = guess.charAt(0);
        if (guessedLetters.contains(letter)) return;

        guessedLetters.add(letter);

        if (currentWord.word.indexOf(letter) >= 0) {
            handleCorrectLetter(sender);
            return;
        }

        handleWrongGuess(guess);
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void update() {
        if(!active) return;

        if(System.currentTimeMillis() - lastActivityTime > 60000){
            ChatUtils.sendPartyMessage("Hangman game was canceled due to inactivity.");
            stop();
        }
    }
}