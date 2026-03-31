package com.example.Poker.game;

import com.example.Poker.dto.CardDTO;

public record Card(Symbol symbol,Suit suit) {
    public enum Suit {
        HEARTS("Red"),
        DIAMONDS("Red"),
        CLUBS("Black"),
        SPADES("Black");

        private final String color;

        Suit(String color) {
            this.color = color;
        }
        public String getColor()  { return color; }
    }

    public enum Symbol {
        ACE(1, "A"),
        TWO(2, "2"),
        THREE(3, "3"),
        FOUR(4, "4"),
        FIVE(5, "5"),
        SIX(6, "6"),
        SEVEN(7, "7"),
        EIGHT(8, "8"),
        NINE(9, "9"),
        TEN(10, "10"),
        JACK(11, "J"),
        QUEEN(12, "Q"),
        KING(13, "K");

        private final int value;
        private final String display;

        Symbol(int value, String display) {
            this.value = value;
            this.display = display;
        }

        public int getValue()      { return value; }
        public String getDisplay() { return display; }
    }

    public CardDTO toDto() {
        return new CardDTO(symbol.name(),suit.name());
    }

    public void print() {
        System.out.println(symbol.getDisplay() + " " + suit.name());
    }

    public String string() {
        return symbol.getDisplay() + " " + suit.name();
    }
}
