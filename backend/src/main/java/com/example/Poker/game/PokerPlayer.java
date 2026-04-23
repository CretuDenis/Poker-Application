package com.example.Poker.game;
import com.example.Poker.game.Card;
import com.example.Poker.dto.PokerPlayerDTO;
import com.example.Poker.dto.HandDTO;
import com.example.Poker.dto.CardDTO;

public class PokerPlayer {
    public Integer balance;
    public Integer bet;
    public String name;
    public boolean spoken;

    public Card first,second;

    public PokerPlayer(Integer balance, String name) {
        this.balance = balance;
        this.bet = 0;
        this.name = name;
        this.spoken = false;
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
        if (folded())
            return new PokerPlayerDTO(name,balance,bet,null); 

        CardDTO firstDto = observer.equals(this.name) ? first.toDto() : null;
        CardDTO secondDto = observer.equals(this.name) ? second.toDto() : null;
        HandDTO handDto = new HandDTO(firstDto,secondDto);

        return new PokerPlayerDTO(name,balance,bet,handDto);
    }

    public PokerPlayerDTO toDto() {
        if (folded())
            return new PokerPlayerDTO(name,balance,bet,null); 
        return new PokerPlayerDTO(name,balance,bet,new HandDTO(first.toDto(),second.toDto()));
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
