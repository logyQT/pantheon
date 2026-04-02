package com.logy.pantheon.features.commands.wheelgame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.logy.pantheon.PantheonMod.LOGGER;

public class WheelWordLoader {
    private static final List<WheelPhrase> PHRASE_DATABASE = new ArrayList<>();
    private static final Random random = new Random();
    private static final File FILE_PATH = new File("config/pantheon/wheel_phrases.txt");

    public static void loadPhrases() {
        PHRASE_DATABASE.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    PHRASE_DATABASE.add(new WheelPhrase(
                            parts[0].trim().toUpperCase(), // HASŁO
                            parts[1].trim(),              // KATEGORIA
                            parts[2].trim()               // PODPOWIEDŹ/DEFINICJA
                    ));
                }
            }

            java.util.Collections.shuffle(PHRASE_DATABASE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static WheelPhrase getRandomPhrase() {
        if (PHRASE_DATABASE.isEmpty()) loadPhrases();

        int index = random.nextInt(PHRASE_DATABASE.size());

        var debug_phrase = PHRASE_DATABASE.get(index);

        LOGGER.info(debug_phrase.phrase);
        LOGGER.info(debug_phrase.hint);

        return PHRASE_DATABASE.remove(index);
    }

    public record WheelPhrase(String phrase, String category, String hint) {
    }
}
