package com.logy.pantheon.features.commands.blackjack;

public class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public int getValue() { return rank.value; }
    public Rank getRank() { return rank; }

    @Override
    public String toString() {
        return "[" + rank.name() + " " + suit.getSymbol() + "]";
    }
}

