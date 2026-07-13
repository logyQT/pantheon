package com.logy.pantheon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PantheonConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pantheon.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean AUTO_EXPERIMENTS = false;
    public boolean AUTO_EXPERIMENTS_AUTO_CLOSE = false;
    public boolean AUTO_EXPERIMENTS_GET_MAX_XP = false;
    public Integer AUTO_EXPERIMENTS_CLICK_DELAY = 200;
    public Integer AUTO_EXPERIMENTS_SERUM_COUNT = 0;

    public Integer MESSAGE_QUE_COOLDOWN_MS = 1000;
    public Integer MESSAGE_QUE_MAX_RETRIES = 3;

    public Integer BLACKJACK_MIN_BET = 10;
    public Integer BLACKJACK_MAX_BET = 1000000;
    public Integer BLACKJACK_BETTING_TIME_MS = 11000;
    public Integer BLACKJACK_TURN_TIME_MS = 30000;
    public Integer BLACKJACK_INSURANCE_TIME_MS = 10000;

    public String FASTWARP_AREA_ONE = "dragon's nest";
    public String FASTWARP_AREA_TWO = "spider mound";
    public String FASTWARP_CMD_ONE = "warp top";
    public String FASTWARP_CMD_TWO = "warp drag";
    public Integer FASTWARP_PTC_PROTECTION_MS = 5000; // PTC - Player transfer cooldown

    public Boolean AUTO_PEARLS = true;

    // === Party Games Toggle States ===
    public boolean BLACKJACK_ENABLED = true;
    public boolean HANGMAN_ENABLED = true;
    public boolean HACK_ENABLED = true;
    public boolean WORDCHAIN_ENABLED = true;
    public boolean ROULETTE_ENABLED = true;
    public boolean WHOAMI_ENABLED = true;
    public boolean MATH_ENABLED = true;
    public boolean SPEEDTYPE_ENABLED = true;
    public boolean GUESS_ENABLED = true;
    public boolean RGUESS_ENABLED = true;
    public boolean WHEEL_ENABLED = true;

    // === Blackjack ===
    public double BLACKJACK_BJ_PAYOUT = 2.5;
    public double BLACKJACK_INSURANCE_PAYOUT = 3.0;
    public Integer BLACKJACK_TIMEOUT_MS = 60000;

    // === Hangman ===
    public Integer HANGMAN_LIVES = 6;
    public Integer HANGMAN_TIMEOUT_MS = 60000;

    // === Hack Game ===
    public Integer HACK_MAX_ATTEMPTS = 3;
    public Integer HACK_REWARD = 100;
    public Integer HACK_TIMEOUT_MS = 18000;
    public Integer HACK_COOLDOWN_MS = 600000;

    // === Word Chain ===
    public Integer WORDCHAIN_REGISTRATION_FEE = 100;
    public Integer WORDCHAIN_INITIAL_TIMEOUT_MS = 20000;
    public Integer WORDCHAIN_TIMEOUT_DECAY_PCT = 95;

    // === Roulette ===
    public Integer ROULETTE_MAX_BETS_PER_PLAYER = 5;
    public Integer ROULETTE_BETTING_TIME_MS = 30000;

    // === WhoAmI ===
    public Integer WHOAMI_ENTRY_COST = 10;
    public Integer WHOAMI_PAYOUT_FAST = 60;
    public Integer WHOAMI_PAYOUT_MID = 40;
    public Integer WHOAMI_PAYOUT_SLOW = 20;
    public Integer WHOAMI_COOLDOWN_MS = 120000;
    public Integer WHOAMI_HINT_TIMEOUT_MS = 15000;

    // === Math Game ===
    public Integer MATH_REWARD = 10;
    public Integer MATH_COOLDOWN_MS = 60000;
    public Integer MATH_TIMEOUT_MS = 10000;

    // === Speed Typing ===
    public Integer SPEEDTYPE_REWARD = 10;
    public Integer SPEEDTYPE_COOLDOWN_MS = 60000;
    public Integer SPEEDTYPE_TIMEOUT_MS = 10000;

    // === Guessing Game ===
    public Integer GUESS_ENTRY_COST = 10;
    public Integer GUESS_PAYOUT_3 = 100;
    public Integer GUESS_PAYOUT_6 = 60;
    public Integer GUESS_PAYOUT_10 = 40;
    public Integer GUESS_SPEED_BONUS_10S = 30;
    public Integer GUESS_SPEED_BONUS_20S = 15;
    public Integer GUESS_COOLDOWN_MS = 120000;
    public Integer GUESS_TIMEOUT_MS = 10000;

    // === Reverse Guess ===
    public Integer RGUESS_MAX_ATTEMPTS = 8;
    public Integer RGUESS_TIMEOUT_MS = 60000;

    // === Wheel Game ===
    public Integer WHEEL_REGISTRATION_FEE = 250;
    public Integer WHEEL_VOWEL_COST = 500;
    public Integer WHEEL_PHRASE_BONUS = 5000;
    public Integer WHEEL_TURN_TIMEOUT_MS = 30000;

    public String prefix = "!";

    private static PantheonConfig INSTANCE = new PantheonConfig();

    public static PantheonConfig get() {
        return INSTANCE;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, PantheonConfig.class);
            } catch (IOException e) {
                INSTANCE = new PantheonConfig();
            }
        }
    }
}
