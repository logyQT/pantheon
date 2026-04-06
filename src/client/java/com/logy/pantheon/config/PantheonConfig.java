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

    public Integer BLACKJACK_MIN_BET = 10;
    public Integer BLACKJACK_MAX_BET = 1000000;
    public Integer BLACKJACK_BETTING_TIME_S = 11000;
    public Integer BLACKJACK_TURN_TIME_S = 30000;
    public Integer BLACKJACK_INSURANCE_TIME_S = 10000;

    public String FASTWARP_AREA_ONE = "dragon's nest";
    public String FASTWARP_AREA_TWO = "spider mound";
    public String FASTWARP_CMD_ONE = "warp top";
    public String FASTWARP_CMD_TWO = "warp drag";
    public Integer FASTWARP_PTC_PROTECTION_MS = 5000; // PTC - Player transfer cooldown


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
