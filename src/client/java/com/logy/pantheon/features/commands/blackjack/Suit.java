package com.logy.pantheon.features.commands.blackjack;

public enum Suit {
    KIER("♥"), CARO("♦"), TREFL("♣"), PIK("♠");
    private final String symbol;
    Suit(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
}
