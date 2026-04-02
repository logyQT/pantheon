package com.logy.pantheon.features.commands.wordchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordChainLoader {
    private static final List<String> WORD_DATABASE = new ArrayList<>();
    private static final File FILE_PATH = new File("config/pantheon/skyblock_words.txt");
    private static final Random random = new Random();

    public static void loadWords() {
        WORD_DATABASE.clear();
        if (!FILE_PATH.exists()) {
            WORD_DATABASE.addAll(List.of("COMBAT", "ATTRIBUTE", "TERMINATOR", "HYPERION", "DRAGON", "ARMOR"));
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                WORD_DATABASE.add(line.trim().toUpperCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getRandomStartWord() {
        if (WORD_DATABASE.isEmpty()) loadWords();
        return WORD_DATABASE.get(random.nextInt(WORD_DATABASE.size()));
    }

    public static boolean isValidWord(String word) {
        return WORD_DATABASE.contains(word.toUpperCase());
    }
}