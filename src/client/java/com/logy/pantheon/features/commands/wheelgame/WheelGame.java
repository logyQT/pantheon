package com.logy.pantheon.features.commands.wheelgame;

import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.utils.ChatUtils;
import java.util.*;

public class WheelGame implements GameInstance {
    private boolean active = false;
    private boolean registrationOpen = false;

    private final List<String> players = new ArrayList<>();
    private final Map<String, Integer> scores = new HashMap<>();
    private int currentPlayerIndex = 0;

    private WheelWordLoader.WheelPhrase currentPhrase;
    private final Set<Character> revealedLetters = new HashSet<>();
    private long stateStartTime = 0;
    private long activityTimer = 0;
    private int currentSpinValue = 0;

    private static final Random random = new Random();
    private static final String VOWELS = "AĄEĘIOÓUY";
    private static final int[] WHEEL_VALUES = {100, 200, 250, 300, 400, 500, 600, 700, 800, 900, 1000, 2500};
    private static final int REGISTRATION_FEE = 250;

    public void start() {
        if (active) return;
        active = true;
        registrationOpen = true;
        players.clear();
        scores.clear();
        revealedLetters.clear();
        stateStartTime = System.currentTimeMillis();
        activityTimer = System.currentTimeMillis();

        ChatUtils.sendPartyMessage("KOŁO FORTUNY! Wpisowe: " + REGISTRATION_FEE + " coins. Zapisy (10s): wpisz .reg");
    }

    private void handleRegistration(String sender) {
        if (players.contains(sender)) return;

        if (!Economy.hasEnough(sender, REGISTRATION_FEE)) {
            ChatUtils.sendPartyMessage(sender + ", nie masz wystarczająco pieniędzy! Potrzebujesz " + REGISTRATION_FEE + " coins.");
            return;
        }

        if (Economy.takeMoney(sender, REGISTRATION_FEE)) {
            players.add(sender);
            scores.put(sender, 0);
            ChatUtils.sendPartyMessage("✔ " + sender + " dołączył/a do gry! Pobrano opłatę.");
        }
    }

    public void beginMatch() {
        currentPhrase = WheelWordLoader.getRandomPhrase();
        currentPlayerIndex = 0;
        stateStartTime = System.currentTimeMillis();

        Collections.shuffle(players);

        ChatUtils.sendPartyMessage("Kategoria: " + currentPhrase.category());
        ChatUtils.sendPartyMessage("Hasło: " + getHiddenPhrase());
        ChatUtils.sendPartyMessage("Format: .L (litera) lub .HASLO");

        spinWheel();
    }

    private void handleLetterGuess(String sender, char letter) {
        String turnOwner = players.get(currentPlayerIndex);
        if (!sender.equalsIgnoreCase(turnOwner)) return;

        if (revealedLetters.contains(letter)) {
            ChatUtils.sendPartyMessage("Ta litera została już odkryta!");
            return;
        }

        if (VOWELS.indexOf(letter) != -1) {
            int currentPoints = scores.get(sender);
            if (currentPoints >= 500) {
                scores.put(sender, currentPoints - 500);
                ChatUtils.sendPartyMessage("Kupiono samogłoskę (-500 pkt).");
            } else {
                ChatUtils.sendPartyMessage("Za mało punktów na samogłoskę! (Min. 500 pkt)");
                return;
            }
        }

        int count = 0;
        String phraseText = currentPhrase.phrase().toUpperCase();
        for (char c : phraseText.toCharArray()) if (c == letter) count++;

        if (count > 0) {
            revealedLetters.add(letter);
            int gain = currentSpinValue * count;
            scores.put(sender, scores.get(sender) + gain);

            ChatUtils.sendPartyMessage("Trafione! " + count + "x " + letter + " (+" + gain + " pkt)");

            if (isAllRevealed()) {
                finishGame(sender, " odkrył/a wszystkie litery!");
            } else {
                ChatUtils.sendPartyMessage(getHiddenPhrase());
                spinWheel();
            }
        } else {
            ChatUtils.sendPartyMessage("Brak litery " + letter + ". Strata tury.");
            shiftTurn();
        }
    }

    private void handleFullGuess(String sender, String guess) {
        if (!sender.equalsIgnoreCase(players.get(currentPlayerIndex))) return;

        if (guess.equalsIgnoreCase(currentPhrase.phrase())) {
            int bonus = 5000;
            scores.put(sender, scores.get(sender) + bonus);
            finishGame(sender, " odgadł/a HASŁO!");
        } else {
            ChatUtils.sendPartyMessage("Błędne hasło! Strata tury.");
            shiftTurn();
        }
    }

    private void spinWheel() {
        stateStartTime = System.currentTimeMillis();
        String turnOwner = players.get(currentPlayerIndex);

        int eventRoll = random.nextInt(100);

        if (eventRoll < 3) {
            scores.put(turnOwner, 0);
            ChatUtils.sendPartyMessage("☠ BANKRUT! " + turnOwner + " traci wszystkie punkty!");
            shiftTurn();
            return;
        } else if (eventRoll < 8) {
            ChatUtils.sendPartyMessage("⚓ STOP! " + turnOwner + " traci kolejkę.");
            shiftTurn();
            return;
        }

        currentSpinValue = WHEEL_VALUES[random.nextInt(WHEEL_VALUES.length)];
        ChatUtils.sendPartyMessage("Odpowiada: " + turnOwner + " | Stawka: " + currentSpinValue);
    }

    private void shiftTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        ChatUtils.sendPartyMessage(getHiddenPhrase());
        spinWheel();
    }

    private void finalizeEconomy() {
        ChatUtils.sendPartyMessage("PODSUMOWANIE WYPŁAT");

        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>(scores.entrySet());
        leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : leaderboard) {
            String pName = entry.getKey();
            int finalPoints = entry.getValue();
            int moneyReward = finalPoints / 10;

            if (moneyReward > 0) {
                Economy.addMoney(pName, moneyReward);
                ChatUtils.sendPartyMessage(pName + ": " + finalPoints + " pkt -> +" + moneyReward + " coins");
            } else {
                ChatUtils.sendPartyMessage(pName + ": " + finalPoints + " pkt -> Brak nagrody pieniężnej.");
            }
        }
    }

    private void finishGame(String winner, String action) {
        ChatUtils.sendPartyMessage("★ KONIEC GRY! " + winner + action);
        ChatUtils.sendPartyMessage("Hasło: " + currentPhrase.phrase().toUpperCase());
        finalizeEconomy();
        stop();
    }

    private String getHiddenPhrase() {
        StringBuilder sb = new StringBuilder("Hasło: ");
        for (char c : currentPhrase.phrase().toUpperCase().toCharArray()) {
            if (c == ' ') sb.append("  ");
            else if (revealedLetters.contains(c)) sb.append(c).append(" ");
            else sb.append("_ ");
        }
        return sb.toString();
    }

    private boolean isAllRevealed() {
        for (char c : currentPhrase.phrase().toUpperCase().toCharArray()) {
            if (c != ' ' && !revealedLetters.contains(c)) return false;
        }
        return true;
    }

    @Override
    public void update() {
        if (!active) return;
        long elapsed = System.currentTimeMillis() - stateStartTime;

        if (registrationOpen && elapsed > 11000) {
            registrationOpen = false;
            if (players.size() < 2) {
                ChatUtils.sendPartyMessage("Zbyt mało graczy. Gra anulowana, wpisowe nie podlega zwrotowi.");
                stop();
            } else {
                beginMatch();
            }
            return;
        }

        if (!registrationOpen && elapsed > 30000) {
            ChatUtils.sendPartyMessage(players.get(currentPlayerIndex) + " nie rusza się! Następny gracz.");
            shiftTurn();
        }

        if (System.currentTimeMillis() - activityTimer > 120000) {
            ChatUtils.sendPartyMessage("Gra zamknięta z powodu bezczynności.");
            stop();
        }
    }

    @Override
    public void handleChat(String sender, String content) {
        if (!content.startsWith(".")) return;
        String input = content.substring(1).trim().toUpperCase();

        if (registrationOpen && input.equals("REG")) {
            handleRegistration(sender);
            return;
        }

        if (!active || registrationOpen || !players.contains(sender)) return;

        activityTimer = System.currentTimeMillis();

        if (input.length() > 1) {
            handleFullGuess(sender, input);
        } else if (input.length() == 1) {
            handleLetterGuess(sender, input.charAt(0));
        }
    }

    @Override public void stop() { active = false; registrationOpen = false; }
    @Override public boolean isActive() { return active; }
}