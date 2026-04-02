package com.logy.pantheon.features.commands.economy;

import com.logy.pantheon.utils.DatabaseManager;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static com.logy.pantheon.PantheonMod.LOGGER;

public class Economy {
    private static final ConcurrentHashMap<String, AtomicInteger> balances = new ConcurrentHashMap<>();

    public static void init() {
        String sql = "SELECT player_name, balance FROM economy";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            balances.clear();
            while (rs.next()) {
                String name = rs.getString("player_name").toLowerCase();
                int balance = rs.getInt("balance");
                balances.put(name, new AtomicInteger(balance));
            }
            LOGGER.info("[Pantheon] Loaded " + balances.size() + " players into economy cache.");
        } catch (SQLException e) {
            LOGGER.error("[Pantheon] Error loading economy data", e);
        }
    }

    public static boolean hasEnough(String playerName, int amount) {
        AtomicInteger balance = balances.get(playerName.toLowerCase());
        return balance != null && balance.get() >= amount;
    }

    public static boolean takeMoney(String playerName, int amount) {
        if (amount < 0) return false;
        String name = playerName.toLowerCase();
        AtomicInteger balance = balances.computeIfAbsent(name, k -> new AtomicInteger(0));

        while (true) {
            int current = balance.get();
            if (current < amount) return false;
            int next = current - amount;
            if (balance.compareAndSet(current, next)) {
                saveToDatabase(name, next);
                return true;
            }
        }
    }

    public static void addMoney(String playerName, int amount) {
        if (amount <= 0) return;
        String name = playerName.toLowerCase();
        int next = balances.computeIfAbsent(name, k -> new AtomicInteger(0)).addAndGet(amount);
        saveToDatabase(name, next);
    }

    private static void saveToDatabase(String playerName, int amount) {
        String sql = "INSERT INTO economy(player_name, balance) VALUES(?, ?) " +
                "ON CONFLICT(player_name) DO UPDATE SET balance = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, amount);
            pstmt.setInt(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("[Pantheon] Failed to save balance for " + playerName, e);
        }
    }

    public static boolean playerExists(String playerName) {
        return balances.containsKey(playerName.toLowerCase());
    }

    public static AtomicInteger getCurrentBalance(String playerName){
        return balances.getOrDefault(playerName.toLowerCase(), new AtomicInteger(0));
    }
}