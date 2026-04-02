package com.logy.pantheon.features.commands.blackjack;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlackjackGame implements GameInstance {
    private GameState state = GameState.FINISHED;

    private final Deck deck = new Deck();
    private final List<HandSession> activeHands = new CopyOnWriteArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Map<String, Integer> insuranceBets = new HashMap<>();

    private boolean gameActive = false;
    private long stateStartTime = 0;
    private int currentHandIndex = 0;

    private static final int MAX_BET = 1000000;
    private static final int MIN_BET = 10;
    private static final long BETTING_TIME_MS = 11000;
    private static final long TURN_TIME_MS = 30000;
    private static final long INSURANCE_TIME_MS = 8000;

    public void start() {
        if (gameActive) return;
        gameActive = true;
        state = GameState.BETTING;
        stateStartTime = System.currentTimeMillis();

        activeHands.clear();
        dealerHand.clear();
        insuranceBets.clear();
        currentHandIndex = 0;

        ChatUtils.sendPartyMessage("BLACKJACK! Betting open (10s). Type .bet <amount>");
    }

    @Override
    public void handleChat(String sender, String content) {
        String msg = content.toLowerCase().trim();

        if (state == GameState.BETTING && msg.startsWith(".bet")) {
            handleBet(sender, msg);
            return;
        }

        if (state == GameState.INSURANCE && msg.equals(".ins")) {
            handleInsurance(sender);
            return;
        }

        if (state == GameState.PLAYER_TURNS) {
            if (currentHandIndex >= activeHands.size()) return;
            HandSession currentHand = activeHands.get(currentHandIndex);

            if (!sender.equalsIgnoreCase(currentHand.playerName)) return;
            if (currentHand.finished) return;

            processAction(currentHand, msg);
        }
    }

    private void handleBet(String sender, String msg) {
        if (activeHands.stream().anyMatch(h -> h.playerName.equalsIgnoreCase(sender))) {
            ChatUtils.sendPartyMessage(sender + ", you already placed a bet!");
            return;
        }

        try {
            String[] parts = msg.split(" ");
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;

            if (amount < MIN_BET || amount > MAX_BET) {
                ChatUtils.sendPartyMessage("Bet: " + MIN_BET + " - " + MAX_BET);
                return;
            }

            if (Economy.takeMoney(sender, amount)) {
                activeHands.add(new HandSession(sender, amount));
                ChatUtils.sendPartyMessage(sender + " joined with " + amount);
            } else {
                ChatUtils.sendPartyMessage(sender + ", you don't have enough money!");
            }
        } catch (NumberFormatException e) {
            ChatUtils.sendPartyMessage("Usage: .bet <amount>");
        }
    }

    private void beginMatch() {
        if (activeHands.isEmpty()) {
            stop();
            return;
        }

        deck.reset();
        for (HandSession hand : activeHands) {
            hand.cards.add(deck.draw());
            hand.cards.add(deck.draw());
            ChatUtils.sendPartyMessage(hand.playerName + " received: " + hand.cards + " (" + calculateScore(hand.cards) + ")");
        }

        dealerHand.add(deck.draw());
        dealerHand.add(deck.draw());

        ChatUtils.sendPartyMessage("Dealer's hand: " + dealerHand.getFirst() + " [?]");

        if (dealerHand.getFirst().getRank() == Rank.ACE) {
            state = GameState.INSURANCE;
            resetTimer();
            ChatUtils.sendPartyMessage("Dealer shows ACE. .ins to buy insurance (8s).");
        } else {
            state = GameState.PLAYER_TURNS;
            resetTimer();
            processTurns();
        }
    }

    private void processAction(HandSession hand, String msg) {
        switch (msg) {
            case ".hit" -> handleHit(hand);
            case ".stand" -> {
                hand.finished = true;
                processTurns();
            }
            case ".double" -> handleDouble(hand);
            case ".split" -> handleSplit(hand);
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
            ChatUtils.sendPartyMessage(playerName + " bought Insurance.");
        }
    }

    private void processTurns() {
        while (currentHandIndex < activeHands.size()) {
            HandSession h = activeHands.get(currentHandIndex);

            if (h.finished) {
                currentHandIndex++;
                resetTimer();
                continue;
            }

            if (calculateScore(h.cards) == 21) {
                ChatUtils.sendPartyMessage(h.playerName + " has 21!");
                h.finished = true;
                currentHandIndex++;
                continue;
            }

            ChatUtils.sendPartyMessage("Turn: " + h.playerName + " | Hand: " + h.cards + " (" + calculateScore(h.cards) + ")");
            return;
        }

        state = GameState.DEALER_TURN;
        handleDealerFinal();
    }

    private void handleHit(HandSession hand) {
        hand.cards.add(deck.draw());
        int score = calculateScore(hand.cards);
        ChatUtils.sendPartyMessage(hand.playerName + " drew " + hand.cards.getLast() + " (Total: " + score + ")");

        if (score >= 21) {
            hand.finished = true;
            processTurns();
        } else {
            resetTimer();
        }
    }

    private void handleDouble(HandSession hand) {
        if (hand.cards.size() != 2) return;
        if (!Economy.takeMoney(hand.playerName, hand.bet)) return;

        hand.bet *= 2;
        hand.cards.add(deck.draw());
        ChatUtils.sendPartyMessage(hand.playerName + " DOUBLED: " + calculateScore(hand.cards));
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

            ChatUtils.sendPartyMessage("Split successful!");
            processTurns();
        }
    }

    private void handleDealerFinal() {
        int dScore = calculateScore(dealerHand);

        ChatUtils.sendPartyMessage("Dealer reveals: " + dealerHand + " (" + dScore + ")");

        if (dealerHand.size() == 2 && dScore == 21) {
            ChatUtils.sendPartyMessage("Dealer has BLACKJACK!");
            finalizeResults();
            return;
        }

        while (calculateScore(dealerHand) < 17) {
            Card drawn = deck.draw();
            dealerHand.add(drawn);
            ChatUtils.sendPartyMessage("Dealer draws: " + drawn + " (" + calculateScore(dealerHand) + ")");
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
                ChatUtils.sendPartyMessage(h.playerName + ": BUSTED!");
            } else if (dBJ && !pBJ) {
                ChatUtils.sendPartyMessage(h.playerName + ": LOST (Dealer BJ)");
            } else if (pBJ && !dBJ) {
                Economy.addMoney(h.playerName, (int) (h.bet * 2.5));
                ChatUtils.sendPartyMessage(h.playerName + ": BLACKJACK!");
            } else if (dScore > 21 || pScore > dScore) {
                Economy.addMoney(h.playerName, h.bet * 2);
                ChatUtils.sendPartyMessage(h.playerName + ": WON!");
            } else if (pScore < dScore) {
                ChatUtils.sendPartyMessage(h.playerName + ": LOST!");
            } else {
                Economy.addMoney(h.playerName, h.bet);
                ChatUtils.sendPartyMessage(h.playerName + ": PUSH!");
            }
        }

        if (dBJ) {
            insuranceBets.forEach((name, amount) -> {
                int payout = amount * 3;
                Economy.addMoney(name, payout);
                ChatUtils.sendPartyMessage(name + ": Insurance payout +" + payout);
            });
        }
        stop();
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
    public void update() {
        if (!gameActive) return;
        long elapsed = System.currentTimeMillis() - stateStartTime;

        switch (state) {
            case BETTING -> {
                if (elapsed > BETTING_TIME_MS) beginMatch();
            }
            case INSURANCE -> {
                if (elapsed > INSURANCE_TIME_MS) {
                    state = GameState.PLAYER_TURNS;
                    processTurns();
                }
            }
            case PLAYER_TURNS -> {
                if (elapsed > TURN_TIME_MS && currentHandIndex < activeHands.size()) {
                    HandSession h = activeHands.get(currentHandIndex);
                    ChatUtils.sendPartyMessage(h.playerName + " timed out! Standing.");
                    h.finished = true;
                    processTurns();
                }
            }
        }
    }

    @Override public boolean isActive() { return gameActive; }
    @Override public void stop() { gameActive = false; state = GameState.FINISHED; }
    private void resetTimer() { stateStartTime = System.currentTimeMillis(); }

    private static class HandSession {
        final String playerName;
        int bet;
        final List<Card> cards = new ArrayList<>();
        boolean finished = false;
        boolean isSplitResult = false;
        HandSession(String n, int b) { this.playerName = n; this.bet = b; }
    }

    private enum GameState { BETTING, INSURANCE, PLAYER_TURNS, DEALER_TURN, FINISHED }
}