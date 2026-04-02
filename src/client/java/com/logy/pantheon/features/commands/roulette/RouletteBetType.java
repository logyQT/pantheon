package com.logy.pantheon.features.commands.roulette;

public enum RouletteBetType {
    STRAIGHT_UP(35),   // Pojedynczy numer
    SPLIT(17),         // 2 numery
    STREET(11),        // 3 numery
    CORNER(8),         // 4 numery
    FIVE_NUMBER(6),    // 0, 00, 1, 2, 3
    LINE(5),           // 6 numerów
    DOZEN(2),          // 12 numerów (tuziny)
    COLUMN(2),         // 12 numerów (kolumny)
    EVEN_MONEY(1);     // 18 numerów (Red/Black, Odd/Even, 1-18, 19-36)

    private final int multiplier;

    RouletteBetType(int multiplier) {
        this.multiplier = multiplier;
    }

    public int calculatePayout(int betAmount) {
        return betAmount + (betAmount * multiplier);
    }
}
