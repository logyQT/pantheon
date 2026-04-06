package com.logy.pantheon.features.commands.blackjack;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.BaseGame;
import com.logy.pantheon.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlackjackGame extends BaseGame {

    private enum BlackjackPhase {
        BETTING, INSURANCE, PLAYER_TURNS, DEALER_TURN
    }

    private BlackjackPhase phase = BlackjackPhase.BETTING;
    private long phaseStartTime = 0;
    private final Deck deck = new Deck();
    private final List<HandSession> activeHands = new CopyOnWriteArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Map<String, Integer> insuranceBets = new HashMap<>();
    private int currentHandIndex = 0;

    private static final int MAX_BET = 1000000;
    private static final int MIN_BET = 10;

    private static final long BETTING_TIME_MS = 11000;
    private static final long TURN_TIME_MS = 30000;
    private static final long INSURANCE_TIME_MS = 8000;

    @Override
    protected String getName() {
        return "Blackjack";
    }

    @Override
    protected void onStart() {
        phase = BlackjackPhase.BETTING;
        resetPhaseTimer();
        activeHands.clear();
        dealerHand.clear();
        insuranceBets.clear();
        currentHandIndex = 0;

        send("Betting open (10s). Type .bet <amount>");
    }

    private void resetPhaseTimer() {
        this.phaseStartTime = now();
    }

    @Override
    protected void onChat(String sender, String message) {
        String msg = message.toLowerCase().trim();

        if (phase == BlackjackPhase.BETTING && msg.startsWith(".bet")) {
            handleBet(sender, msg);
        } else if (phase == BlackjackPhase.INSURANCE && msg.equals(".ins")) {
            handleInsurance(sender);
        } else if (phase == BlackjackPhase.PLAYER_TURNS) {
            handlePlayerAction(sender, msg);
        }
    }

    private void handleBet(String sender, String msg) {
        if (activeHands.stream().anyMatch(h -> h.playerName.equalsIgnoreCase(sender))) {
            send(sender + ", you already placed a bet!");
            return;
        }

        try {
            String[] parts = msg.split(" ");
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;

            if (amount < MIN_BET || amount > MAX_BET) {
                send("Bet limit: " + MIN_BET + " - " + MAX_BET);
                return;
            }

            if (Economy.takeMoney(sender, amount)) {
                activeHands.add(new HandSession(sender, amount));
                send(sender + " joined with " + amount);
            } else {
                send(sender + ", insufficient funds!");
            }
        } catch (NumberFormatException e) {
            send("Usage: .bet <amount>");
        }
    }

    private void handleInsurance(String playerName) {
        Optional<HandSession> playerHand = activeHands.stream()
                .filter(h -> h.playerName.equalsIgnoreCase(playerName))
                .findFirst();

        if (playerHand.isEmpty() || insuranceBets.containsKey(playerName)) return;

        int insAmount = playerHand.get().bet / 2;
        if (Economy.takeMoney(playerName, insAmount)) {
            insuranceBets.put(playerName, insAmount);
            send(playerName + " bought Insurance.");
        }
    }

    private void handlePlayerAction(String sender, String msg) {
        if (currentHandIndex >= activeHands.size()) return;
        HandSession currentHand = activeHands.get(currentHandIndex);

        if (!sender.equalsIgnoreCase(currentHand.playerName) || currentHand.finished) return;

        switch (msg) {
            case ".hit" -> handleHit(currentHand);
            case ".stand" -> {
                currentHand.finished = true;
                processTurns();
            }
            case ".double" -> handleDouble(currentHand);
            case ".split" -> handleSplit(currentHand);
        }
    }

    private void beginMatch() {
        if (activeHands.isEmpty()) {
            stop(StopReason.ERROR);
            send("No players joined. Game cancelled.");
            return;
        }

        deck.reset();
        for (HandSession hand : activeHands) {
            hand.cards.add(deck.draw());
            hand.cards.add(deck.draw());
            send(hand.playerName + " received: " + hand.cards + " (" + calculateScore(hand.cards) + ")");
        }

        dealerHand.add(deck.draw());
        dealerHand.add(deck.draw());

        send("Dealer's hand: " + dealerHand.get(0) + " [?]");

        if (dealerHand.get(0).getRank() == Rank.ACE) {
            phase = BlackjackPhase.INSURANCE;
            resetPhaseTimer();
            send("Dealer shows ACE. .ins to buy insurance (8s).");
        } else {
            startPlayerTurns();
        }
    }

    private void startPlayerTurns() {
        phase = BlackjackPhase.PLAYER_TURNS;
        resetPhaseTimer();
        processTurns();
    }

    private void processTurns() {
        while (currentHandIndex < activeHands.size()) {
            HandSession h = activeHands.get(currentHandIndex);

            if (h.finished) {
                currentHandIndex++;
                resetPhaseTimer();
                continue;
            }

            if (calculateScore(h.cards) == 21) {
                send(h.playerName + " has 21!");
                h.finished = true;
                currentHandIndex++;
                continue;
            }

            send("Turn: " + h.playerName + " | Hand: " + h.cards + " (" + calculateScore(h.cards) + ")");
            return;
        }

        phase = BlackjackPhase.DEALER_TURN;
        handleDealerFinal();
    }

    private void handleHit(HandSession hand) {
        hand.cards.add(deck.draw());
        int score = calculateScore(hand.cards);
        send(hand.playerName + " drew " + hand.cards.get(hand.cards.size() - 1) + " (Total: " + score + ")");

        if (score >= 21) {
            hand.finished = true;
            processTurns();
        } else {
            resetPhaseTimer();
        }
    }

    private void handleDouble(HandSession hand) {
        if (hand.cards.size() != 2) return;
        if (!Economy.takeMoney(hand.playerName, hand.bet)) return;

        hand.bet *= 2;
        hand.cards.add(deck.draw());
        send(hand.playerName + " DOUBLED: " + calculateScore(hand.cards));
        hand.finished = true;
        processTurns();
    }

    private void handleSplit(HandSession hand) {
        if (hand.isSplitResult || hand.cards.size() != 2) return;
        if (hand.cards.get(0).getValue() != hand.cards.get(1).getValue()) return;

        if (Economy.takeMoney(hand.playerName, hand.bet)) {
            HandSession splitHand = new HandSession(hand.playerName, hand.bet);
            splitHand.isSplitResult = true;
            hand.isSplitResult = true;

            splitHand.cards.add(hand.cards.remove(1));
            hand.cards.add(deck.draw());
            splitHand.cards.add(deck.draw());

            activeHands.add(currentHandIndex + 1, splitHand);
            send("Split successful!");
            processTurns();
        }
    }

    private void handleDealerFinal() {
        int dScore = calculateScore(dealerHand);
        send("Dealer reveals: " + dealerHand + " (" + dScore + ")");

        if (dealerHand.size() == 2 && dScore == 21) {
            send("Dealer has BLACKJACK!");
        } else {
            while (calculateScore(dealerHand) < 17) {
                Card drawn = deck.draw();
                dealerHand.add(drawn);
                send("Dealer draws: " + drawn + " (" + calculateScore(dealerHand) + ")");
            }
        }
        finalizeResults();
    }

    private void finalizeResults() {
        int dScore = calculateScore(dealerHand);
        boolean dBJ = dealerHand.size() == 2 && dScore == 21;

        for (HandSession h : activeHands) {
            int pScore = calculateScore(h.cards);
            boolean pBJ = h.cards.size() == 2 && pScore == 21 && !h.isSplitResult;

            if (pScore > 21) {
                send(h.playerName + ": BUSTED!");
            } else if (dBJ && !pBJ) {
                send(h.playerName + ": LOST (Dealer BJ)");
            } else if (pBJ && !dBJ) {
                Economy.addMoney(h.playerName, (int) (h.bet * 2.5));
                send(h.playerName + ": BLACKJACK!");
            } else if (dScore > 21 || pScore > dScore) {
                Economy.addMoney(h.playerName, h.bet * 2);
                send(h.playerName + ": WON!");
            } else if (pScore < dScore) {
                send(h.playerName + ": LOST!");
            } else {
                Economy.addMoney(h.playerName, h.bet);
                send(h.playerName + ": PUSH!");
            }
        }

        if (dBJ) {
            insuranceBets.forEach((name, amount) -> {
                int payout = amount * 3;
                Economy.addMoney(name, payout);
                send(name + ": Insurance payout +" + payout);
            });
        }
        stop(StopReason.WIN);
    }

    private int calculateScore(List<Card> hand) {
        int s = 0, a = 0;
        for (Card c : hand) {
            s += c.getValue();
            if (c.getRank() == Rank.ACE) a++;
        }
        while (s > 21 && a > 0) { s -= 10; a--; }
        return s;
    }

    @Override
    protected void onUpdate(long deltaMs) {
        long elapsedSinceState = now() - phaseStartTime;

        switch (phase) {
            case BETTING -> {
                if (elapsedSinceState > BETTING_TIME_MS) beginMatch();
            }
            case INSURANCE -> {
                if (elapsedSinceState > INSURANCE_TIME_MS) startPlayerTurns();
            }
            case PLAYER_TURNS -> {
                if (elapsedSinceState > TURN_TIME_MS) {
                    HandSession h = activeHands.get(currentHandIndex);
                    send(h.playerName + " timed out! Standing.");
                    h.finished = true;
                    processTurns();
                }
            }
        }
    }

    @Override
    protected void onCleanup(StopReason reason) {
        // Logika czyszczenia, jeśli potrzebna
    }

    @Override
    protected void onTimeout() {
        send("Blackjack game timed out due to inactivity.");
    }

    @Override
    protected long getTimeoutMs() {
        return 60000L; // Dajemy graczom minutę na całą grę bez ruchu
    }

    private static class HandSession {
        final String playerName;
        int bet;
        final List<Card> cards = new ArrayList<>();
        boolean finished = false;
        boolean isSplitResult = false;
        HandSession(String n, int b) { this.playerName = n; this.bet = b; }
    }
}