package com.logy.pantheon.features.commands.hangman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class WordLoader {
    private static List<HangmanWord> WORD_DATABASE = new ArrayList<>();
    private static final File WORDS_FILE = new File("config/pantheon/hangman_words.txt");
    private static final Random random = new Random();

    public static void loadWords() {
        CompletableFuture.runAsync(() -> {
            List<HangmanWord> tempDatabase = new ArrayList<>();
            if (!WORDS_FILE.exists()) {
                tempDatabase.add(new HangmanWord("HYPERION", "Skyblock", "Weapons", "The boom boom sword."));
            } else {
                try (BufferedReader reader = new BufferedReader(new FileReader(WORDS_FILE))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(";", 4);
                        if (parts.length < 4) continue;
                        tempDatabase.add(new HangmanWord(parts[0], parts[1], parts[2], parts[3]));
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
            WORD_DATABASE = tempDatabase;
        });
    }
    public static HangmanWord getRandomWord(){
        if(WORD_DATABASE.isEmpty()) loadWords();
        return WORD_DATABASE.remove(random.nextInt(WORD_DATABASE.size()));
    }

}
