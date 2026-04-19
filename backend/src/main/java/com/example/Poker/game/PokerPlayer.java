package com.example.Poker.game;
import com.example.Poker.game.Card;
import com.example.Poker.dto.PokerPlayerDTO;
import com.example.Poker.dto.HandDTO;

public class PokerPlayer {
    public Integer balance;
    public Integer bet;
    public String name;

    public Card first,second;

    public PokerPlayer(Integer balance, String name) {
        this.balance = balance;
        this.bet = 0;
        this.name = name;
    }

    public PokerPlayer(PokerPlayer other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot copy a null player");
        }
        this.name = other.name;
        this.balance = other.balance;
        this.bet = other.bet;

        this.first = other.first;
        this.second = other.second;
    }

    public boolean tryBet(Integer amount) {
        if(balance - amount < 0) return false;
        bet += amount;
        balance -= amount; 
        return true;
    }

    public void allIn() {
        bet += balance;
        balance = 0;
    }

    public void claim(Integer amount) { balance += amount; }
    public boolean brokeAss() { return balance == 0; }
    public boolean folded() { return (first == null || second == null) && !(balance == 0 && bet == 0); }
    public void fold() {
        first = null;
        second = null;
    }

    public PokerPlayerDTO toDto(String observer) {
        return new PokerPlayerDTO(name,balance,bet, folded() ? null : new HandDTO(observer == this.name ? first.toDto() : null,observer == this.name ? second.toDto() : null));
    }

    public PokerPlayerDTO toDto() {
        return new PokerPlayerDTO(name,balance,bet, folded() ? null : new HandDTO(first.toDto(),second.toDto()));
    }

    public void print() {
        System.out.println("Name: " + name);
        System.out.println("Balance: " + balance);
        System.out.println("Current bet: " + bet);
        if(first != null && second != null) {
            System.out.println("Hand: " + first.string() + " " + second.string());
        } else {
            System.out.println("Folded");
        }
    }
}
