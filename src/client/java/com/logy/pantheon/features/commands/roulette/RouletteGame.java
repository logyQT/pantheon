package com.logy.pantheon.features.commands.roulette;

import com.logy.pantheon.features.commands.main.GameInstance;
import com.logy.pantheon.features.commands.economy.Economy;
import com.logy.pantheon.utils.ChatUtils;
import java.util.*;

public class RouletteGame implements GameInstance {
    private boolean active = false;
    private final List<Bet> currentBets = new ArrayList<>();
    private long startTime = 0;
    private static final int MAX_BETS_PER_PLAYER = 5;

    private static final Set<Integer> RED_NUMBERS = new HashSet<>(Arrays.asList(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36));

    private record Bet(String player, int amount, RouletteBetType type, Set<String> chosenValues) {}

    public void start() {
        if (active) return;
        active = true;
        currentBets.clear();
        startTime = System.currentTimeMillis();
        ChatUtils.sendPartyMessage("✔ ROULETTE STARTED! Place bets (30s) using .bet <amount> <value>");
        ChatUtils.sendPartyMessage("Values: 0-36, 00, RED, BLACK, EVEN, ODD, 1st, C1, or numbers like 1,2,3");
    }

    public void spin() {
        Random rand = new Random();
        int result = rand.nextInt(38); // 0-36 + 37 as "00"
        String resultStr = (result == 37) ? "00" : String.valueOf(result);
        String color = (result == 0 || result == 37) ? "GREEN" : (RED_NUMBERS.contains(result) ? "RED" : "BLACK");

        ChatUtils.sendPartyMessage("The wheel spins... The ball landed on: " + color + " " + resultStr + "!");

        Map<String, Integer> winners = new HashMap<>();

        for (Bet bet : currentBets) {
            if (isWinner(result, resultStr, bet)) {
                int payout = bet.type.calculatePayout(bet.amount);
                winners.put(bet.player, winners.getOrDefault(bet.player, 0) + payout);
            }
        }

        if (winners.isEmpty()) {
            ChatUtils.sendPartyMessage("House wins! No winners this round.");
        } else {
            winners.forEach((player, total) -> {
                Economy.addMoney(player, total);
                ChatUtils.sendPartyMessage("✔ " + player + " won a total of " + total + " coins!");
            });
        }
        stop();
    }

    private boolean isWinner(int roll, String rollStr, Bet bet) {
        // Zero / Double Zero safety
        if (rollStr.equals("0") || rollStr.equals("00")) {
            return bet.chosenValues.contains(rollStr);
        }

        return switch (bet.type) {
            case STRAIGHT_UP, SPLIT, STREET, CORNER, FIVE_NUMBER, LINE -> bet.chosenValues.contains(rollStr);
            case EVEN_MONEY -> checkEvenMoney(roll, bet.chosenValues.iterator().next());
            case DOZEN -> checkDozen(roll, bet.chosenValues.iterator().next());
            case COLUMN -> checkColumn(roll, bet.chosenValues.iterator().next());
        };
    }

    private boolean checkColumn(int roll, String choice) {
        int col = Integer.parseInt(choice);
        if (col == 3) return roll % 3 == 0;
        return roll % 3 == col;
    }

    private boolean checkEvenMoney(int roll, String choice) {
        return switch (choice.toUpperCase()) {
            case "RED" -> RED_NUMBERS.contains(roll);
            case "BLACK" -> !RED_NUMBERS.contains(roll);
            case "EVEN" -> roll % 2 == 0;
            case "ODD" -> roll % 2 != 0;
            case "LOW" -> roll >= 1 && roll <= 18;
            case "HIGH" -> roll >= 19 && roll <= 36;
            default -> false;
        };
    }

    private boolean checkDozen(int roll, String choice) {
        return switch (choice) {
            case "1" -> roll >= 1 && roll <= 12;
            case "2" -> roll >= 13 && roll <= 24;
            case "3" -> roll >= 25 && roll <= 36;
            default -> false;
        };
    }

    @Override
    public void handleChat(String sender, String msg) {
        if (!active || !msg.toLowerCase().startsWith(".bet")) return;

        String[] args = msg.split("\\s+");
        if (args.length < 3) return;

        // Limit zakładów na gracza
        long playerBetCount = currentBets.stream().filter(b -> b.player.equalsIgnoreCase(sender)).count();
        if (playerBetCount >= MAX_BETS_PER_PLAYER) {
            ChatUtils.sendPartyMessage(sender + ", max " + MAX_BETS_PER_PLAYER + " bets allowed!");
            return;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) return;

            String betValue = args[2].toUpperCase();

            if (!Economy.hasEnough(sender, amount)) {
                ChatUtils.sendPartyMessage(sender + ", insufficient balance!");
                return;
            }

            Bet bet = parseBet(sender, amount, betValue);
            if (bet == null) {
                ChatUtils.sendPartyMessage(sender + ", invalid bet type/value!");
                return;
            }

            if (Economy.takeMoney(sender, amount)) {
                currentBets.add(bet);
                ChatUtils.sendPartyMessage("✔ " + sender + " placed " + amount + " on " + betValue);
            }

        } catch (NumberFormatException ignored) {}
    }

    private Bet parseBet(String player, int amount, String val) {
        // Multi-number parser (Split, Street, Corner, Line)
        if (val.contains(",")) {
            Set<String> nums = new HashSet<>(Arrays.asList(val.split(",")));
            // Validate all are valid numbers
            if (!nums.stream().allMatch(n -> n.matches("^(00|[0-9]|[12][0-9]|3[0-6])$"))) return null;

            return switch (nums.size()) {
                case 2 -> new Bet(player, amount, RouletteBetType.SPLIT, nums);
                case 3 -> new Bet(player, amount, RouletteBetType.STREET, nums);
                case 4 -> new Bet(player, amount, RouletteBetType.CORNER, nums);
                case 5 -> nums.containsAll(Arrays.asList("0","00","1","2","3")) ? new Bet(player, amount, RouletteBetType.FIVE_NUMBER, nums) : null;
                case 6 -> new Bet(player, amount, RouletteBetType.LINE, nums);
                default -> null;
            };
        }

        // STRAIGHT UP
        if (val.matches("^(00|[0-9]|[12][0-9]|3[0-6])$")) {
            return new Bet(player, amount, RouletteBetType.STRAIGHT_UP, Collections.singleton(val));
        }

        // EVEN MONEY
        if (Arrays.asList("RED", "BLACK", "EVEN", "ODD", "LOW", "HIGH").contains(val)) {
            return new Bet(player, amount, RouletteBetType.EVEN_MONEY, Collections.singleton(val));
        }

        // DOZENS
        if (val.equals("1ST") || val.equals("1")) return new Bet(player, amount, RouletteBetType.DOZEN, Collections.singleton("1"));
        if (val.equals("2ND") || val.equals("2")) return new Bet(player, amount, RouletteBetType.DOZEN, Collections.singleton("2"));
        if (val.equals("3RD") || val.equals("3")) return new Bet(player, amount, RouletteBetType.DOZEN, Collections.singleton("3"));

        // COLUMN
        if (val.startsWith("C") && val.length() == 2) {
            return new Bet(player, amount, RouletteBetType.COLUMN, Collections.singleton(val.substring(1)));
        }

        return null;
    }

    @Override public void update() {
        if (active && System.currentTimeMillis() - startTime > 30000) spin();
    }
    @Override public void stop() { active = false; }
    @Override public boolean isActive() { return active; }
}