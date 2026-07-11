package com.logy.pantheon.features.commands.guessgame;

import com.logy.pantheon.features.commands.main.BaseGame;
import java.util.Random;

public class ReverseGuessingGame extends BaseGame {

    private enum GamePhase {
        WAITING_FOR_PLAYER,
        GUESSING
    }

    private GamePhase phase = GamePhase.WAITING_FOR_PLAYER;
    private int low = 1;
    private int high = 100;
    private int currentGuess;
    private int attemptCount = 0;
    private final Random random = new Random();

    private static final String[] GUESS_TEMPLATES = {
            "I'm thinking it's %d!",
            "Is your number %d?",
            "My bet is on %d.",
            "Logic dictates your number is %d.",
            "Maybe %d?",
            "Let's try %d."
    };

    private static final String[] SNARKY_REPLIES = {
            "That's not 'start'. Reading comprehension is hard, isn't it?",
            "The instruction was simple: type 'start'. Try again...",
            "I'm waiting for 'start' and I get this? My CPU is crying.",
            "Invalid access code! Type 'start' before I overheat."
    };

    @Override
    protected String getName() { return "Reverse Guess"; }

    @Override
    protected void onStart() {
        phase = GamePhase.WAITING_FOR_PLAYER;
        send(starterName + ", think of a number between 1 and 100. Type 'start' when you're ready!");
    }

    @Override
    protected void onChat(String sender, String message) {
        // Block: Respond only to the player who started the game
        if (!sender.equalsIgnoreCase(starterName)) return;

        String msg = message.toLowerCase().trim();

        if (phase == GamePhase.WAITING_FOR_PLAYER) {
            if (!msg.equals("start")) return;
            //send(SNARKY_REPLIES[random.nextInt(SNARKY_REPLIES.length)]);

            phase = GamePhase.GUESSING;
            send("Let's go! Reply with 'h' (higher), 'l' (lower), or 'win'.");
            makeGuess();
            return;
        }

        if (phase == GamePhase.GUESSING) {
            switch (msg) {
                case "h" -> {
                    low = currentGuess + 1;
                    makeGuess();
                }
                case "l" -> {
                    high = currentGuess - 1;
                    makeGuess();
                }
                case "win", "correct" -> {
                    send("GG! I knew it was " + currentGuess + "! Range: " + low + "-" + high);
                    stop(StopReason.WIN);
                }
            }
        }
    }

    private void makeGuess() {
        attemptCount++;

        if (low > 100) {
            send("Seriously? Higher than 100? We agreed on 1-100. I'm out!");
            stop(StopReason.ERROR);
            return;
        }
        if (high < 1) {
            send("Less than 1? Very funny. Go prank someone else!");
            stop(StopReason.ERROR);
            return;
        }

        if (low > high || attemptCount > 8) {
            send("Attempt #" + attemptCount + "... Wait a second! Math says you're lying.");
            send("It shouldn't take this long for a range of 100. Ending the game, you cheater!");
            stop(StopReason.ERROR);
            return;
        }

        currentGuess = calculateSmartGuess();
        String template = GUESS_TEMPLATES[random.nextInt(GUESS_TEMPLATES.length)];
        send(String.format(template, currentGuess));
    }

    private int calculateSmartGuess() {
        if (low <= 69 && high >= 69 && random.nextInt(100) < 45) return 69;
        if (low <= 21 && high >= 21 && (high - low) < 15) return 21;
        if (low <= 42 && high >= 42 && (high - low) < 20) return 42;
        if (low <= 7 && high >= 7 && (high - low) < 10) return 7;

        return (low + high) / 2;
    }

    @Override
    protected void onCleanup(StopReason reason) {
        attemptCount = 0;
        currentGuess = 0;
        high = 100;
        low = 1;
    }

    @Override
    protected long getTimeoutMs() {
        return 60000L; // 60 seconds for player response
    }
}