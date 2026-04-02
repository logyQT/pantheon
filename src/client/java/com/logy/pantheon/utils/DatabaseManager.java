package com.logy.pantheon.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class DatabaseManager {
    private static Connection connection;
    private static final String DB_PATH = "config/pantheon/economy.db";

    public static void init() {
        try {
            File dbFile = new File(DB_PATH);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            String url = "jdbc:sqlite:" + DB_PATH;
            connection = DriverManager.getConnection(url);

            createTables();
            LOGGER.info("Database initialized successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS economy (" +
                "player_name TEXT PRIMARY KEY, " +
                "balance INTEGER DEFAULT 0" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
