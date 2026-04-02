package com.logy.pantheon.features.commands.hangman;

public class HangmanWord {
    public String word, category, subcategory, definition;

    public HangmanWord(String word, String category, String subcategory, String definition) {
        this.word = word.toUpperCase().trim();
        this.category = category.trim();
        this.subcategory = subcategory.trim();
        this.definition = definition.trim();
    }
}
