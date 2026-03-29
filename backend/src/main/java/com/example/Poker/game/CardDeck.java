package com.example.Poker.game;
import java.util.Stack;
import com.example.Poker.game.Card;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CardDeck {
    private Random random;

    private List<Card> deck;
    private final Integer numSwaps = 100;

    public CardDeck() {
        this.random = new Random();
        deck = new ArrayList<Card>();
        for(Card.Symbol symbol : Card.Symbol.values()) {
            for(Card.Suit suit : Card.Suit.values()) {
                deck.add(new Card(symbol,suit));
            }
        }
    }

    public Integer size() {
        return deck.size();
    }

    public void shuffle() {
        for(int i = 0; i < numSwaps; i++) {
            int first = random.nextInt(deck.size());
            int second = random.nextInt(deck.size());
            while(first == second) {
                second = random.nextInt(deck.size());
            }

            Collections.swap(deck,first,second);
        }
    }

    public Card dealCard() {
        return deck.remove(deck.size() - 1);
    }
    
    public void print() {
        for(Card card : deck) {
            card.print();
        }
    }
}
