package com.example.Poker.game;

import com.example.Poker.game.PokerPlayer;
import com.example.Poker.game.CardDeck;
import com.example.Poker.game.PokerScore;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.lang.Math;

public class Poker {
    public enum BettingStage {
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER
    }

    private final Integer baseBalance = 1000;
    private final Integer startSmallBlind = 15;

    private Integer roundCount;
    private Integer smallBlind;
    private Integer buttonIndex;
    private Integer speakingIndex;
    private Integer minimumRaise;

    private BettingStage bettingStage;
    
    private CardDeck cards;

    private List<Card> foldedCards;
    private List<PokerPlayer> players;

    private Card[]  flopCards;
    private Card    turnCard;
    private Card    riverCard;

    public Poker(List<String> playerNames) {
        bettingStage    = BettingStage.PRE_FLOP;
        flopCards       = null;
        turnCard        = null;
        riverCard       = null;

        smallBlind      = startSmallBlind;

        roundCount      = 0;
        buttonIndex     = 0;
        minimumRaise    = smallBlind * 2;
        // + 1 small +2 big blind
        speakingIndex   = (buttonIndex + 3) % playerNames.size();

        cards           = new CardDeck();
        foldedCards     = new ArrayList<Card>();
            players         = new ArrayList<PokerPlayer>();

        for(String name : playerNames) {
            players.add(new PokerPlayer(baseBalance,name));
        }
    }

    public Integer getMaxBet() {
        Integer maxBet = 0;
        for(PokerPlayer player : players) {
            if(player.bet > maxBet)
                maxBet = player.bet;
        }
        return maxBet;
    }

    public Integer getPot() {
        Integer pot = 0;
        for(PokerPlayer player : players) {
            pot += player.bet;
        }
        return pot;
    }
    
    public Integer getSmallBlind()  { return smallBlind; }
    public Integer getBigBlind()    { return 2 * smallBlind; }

    public void doubleBlind() { smallBlind *= 2; }

    public void roundSetup() {
        bettingStage = BettingStage.PRE_FLOP;
        List<PokerPlayer> remainingPlayers = new ArrayList<PokerPlayer>();
        for(PokerPlayer player : players) {
            if(player.balance != 0)
                remainingPlayers.add(player);
        }

        players = remainingPlayers;

        int numPlayers = players.size();

        cards.shuffle();
        
        if(roundCount != 0) {
            buttonIndex = (buttonIndex + 1) % numPlayers;
        }

        int currentDealt = (buttonIndex + 1) % numPlayers;

        int dealtCards = 0;
        while(dealtCards != numPlayers) {
            Card dealtCard = cards.dealCard();
            PokerPlayer player = players.get(currentDealt);
            player.first = dealtCard;
            
            currentDealt = (currentDealt + 1) % numPlayers;
            dealtCards++;
        }

        dealtCards = 0;

        while(dealtCards != numPlayers) {
            Card dealtCard = cards.dealCard();
            PokerPlayer player = players.get(currentDealt);
            player.second = dealtCard;
            
            currentDealt = (currentDealt + 1) % numPlayers;
            dealtCards++;
        }

        int smallBlindIndex = (buttonIndex + 1) % numPlayers;
        int bigBlindIndex   = (buttonIndex + 2) % numPlayers;
        
        if(smallBlindIndex == bigBlindIndex) {
            System.out.println("Big blind is equal to small blind!");
        }

        PokerPlayer smallBlindPlayer = players.get(smallBlindIndex);
        PokerPlayer bigBlindPlayer = players.get(bigBlindIndex);

        if(!smallBlindPlayer.tryBet(smallBlind)) {
            smallBlindPlayer.allIn();
        }
        
        if(!bigBlindPlayer.tryBet(smallBlind * 2)) {
            bigBlindPlayer.allIn();
        }
    } 

    public void flop() {
        flopCards = new Card[3]; 
        for(int i = 0; i < 3; i++) {
            flopCards[i] = cards.dealCard();
        }
    }

    public void turn() {
        turnCard = cards.dealCard();
    }

    public void river() {
        riverCard = cards.dealCard();
    }

    public void clearFlop() {
        flopCards = null;
    }

    public void clearTurn() {
        flopCards = null;
        turnCard = null;
    }

    public void clearRiver() {
        flopCards = null;
        turnCard = null;
        riverCard = null;
    }

    public void handleFold() {
        PokerPlayer speakingPlayer = players.get(speakingIndex);
        foldedCards.add(speakingPlayer.first);                 
        foldedCards.add(speakingPlayer.second);                 

        speakingPlayer.fold();

        speakingIndex = (speakingIndex + 1) % players.size();
    }

    public boolean handleCall() {
        PokerPlayer speakingPlayer = players.get(speakingIndex);
        Integer maxBet = getMaxBet();        

        Integer toBet = maxBet - speakingPlayer.bet; 
        if(!speakingPlayer.tryBet(toBet)) return false;
        
        speakingIndex = (speakingIndex + 1) % players.size();

        return true;
    }

    public String getSpeakingPlayer() {
        return players.get(speakingIndex).name;
    }

    public boolean handleCheck() {
        PokerPlayer speakingPlayer = players.get(speakingIndex);
        Integer maxBet = getMaxBet();        

        if(speakingPlayer.bet < maxBet) return false;
        
        speakingIndex = (speakingIndex + 1) % players.size();
        return true;
    }
    
    public boolean handleRaise(Integer amount) {
        if(amount < minimumRaise) return false;
        minimumRaise = amount;
        PokerPlayer speakingPlayer = players.get(speakingIndex);

        if(!speakingPlayer.tryBet(amount)) return false;

        speakingIndex = (speakingIndex + 1) % players.size();
        return true;
    }

    private boolean allActiveBetsAreEqual() {
        int maxBet = getMaxBet();

        for(PokerPlayer player : players) {
            if(player.bet < maxBet && player.first != null && player.second != null)
                return false;
        }

        return true;
    }

    private Integer getActivePlayers() {
        Integer count = 0;
        for(PokerPlayer player : players) {
            if(player.first != null && player.second != null)
                count++;
        }
        return count;
    }

    public void bet() {
        Scanner scanner = new Scanner(System.in);
        int numPlayers = players.size();
        boolean allSpoke = false;
        boolean bigRaised = false;
        Integer bigBlindIndex = (buttonIndex + 2) % numPlayers;

        while((!allActiveBetsAreEqual() || (speakingIndex == bigBlindIndex && !bigRaised) || !allSpoke) && getActivePlayers() > 1) {
            if(speakingIndex == bigBlindIndex) allSpoke = true;

            print();
            System.out.println(" "); 
            
            System.out.println(getSpeakingPlayer() + " is speaking now"); 
            System.out.println("Command: "); 

            String command = scanner.nextLine();

            if(command.equals("FOLD")) {
                handleFold();
            } else if(command.equals("RAISE")) {
                if(speakingIndex == bigBlindIndex) bigRaised = true;
                Integer amount = Integer.parseInt(scanner.nextLine());
                handleRaise(amount);
            } else if(command.equals("CALL")) {
                handleCall();
            } else if(command.equals("CHECK")) {
                handleCheck();
            } else {
                System.out.println("Invalid command"); 
            }
        } 
        speakingIndex = (bigBlindIndex + 1) % numPlayers;
    }


// all bytes will be set to ff when the score is for a royale flush
// straight flush = encode the largest element 5 4 3 2 1 we will store 5 in the most significant nibble  1 nibble  index    : 25
// four of a kind = store the symbol of the 4 of a kind in the next nibble and the remaining card        2 nibble  index    : 24-23
// full hosue     = store the symbol of the biggest 3 of a kind and the symbol of the pair               2 nibble  index    : 22-21
// flush          = need to store all 5 symbols of the flush                                             5 nibbles indices  : 20-19-18-17-16
// straight       = biggest of the straight                                                              1 nibble  index    : 15
// three of kind  = biggest three of a kind symbol,and the next 2 cards                                  3 nibble  index    : 14-13-12
// two pair       =                                                                                      3 nibbles indices  : 11-10-9
// one pair       = 1 nibble for the pair 3 for the other 3 cards                                        4 nibbles indices  : 8-7-6-5
// high card      = store all 5 card values                                                              5 nibbles indices  : 4-3-2-1-0
    
    public Optional<PokerScore> evaluateRoyaleFlush(List<Card> cards, HashMap<Card.Suit,Integer> suitMap) {
        Card.Suit flushSuit = null;
        for(Map.Entry<Card.Suit,Integer> entry : suitMap.entrySet()) {
            Integer count = entry.getValue();
            if(count >= 5) {
                flushSuit = entry.getKey();
                break;
            }
        } 

        if(flushSuit == null) return Optional.empty();

        List<Card> royaleCards = new ArrayList<Card>();
        for(Card card : cards) {
            Card.Symbol symbol = card.symbol();
            if((symbol == Card.Symbol.ACE || 
                        symbol == Card.Symbol.KING || 
                        symbol == Card.Symbol.QUEEN || 
                        symbol == Card.Symbol.JACK || 
                        symbol == Card.Symbol.TEN) && card.suit() == flushSuit)
                royaleCards.add(card);
        }
        if(royaleCards.size() != 5) return Optional.empty();
        PokerScore score = new PokerScore();
        score.setRoyalFlush();

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateStraightFlush(List<Card> cards,HashMap<Card.Suit,Integer> suitMap) {
        Card.Suit flushSuit = null; 
        for(Map.Entry<Card.Suit,Integer> entry : suitMap.entrySet()) {
            Integer count = entry.getValue();
            if(count >= 5) {
                flushSuit = entry.getKey();
                break;
            }
        }

        if(flushSuit == null) return Optional.empty();

        List<Card> flushCards = new ArrayList<>();
        for(Card card : cards) {
            if(card.suit() == flushSuit)
                flushCards.add(card);
        }

        flushCards.sort((a,b) -> b.symbol().getValue() - a.symbol().getValue());

        PokerScore score = new PokerScore();
        int begin = 0;
        int end   = 5;

        while(end <= flushCards.size()) {
            boolean consecutives = true;
            for(int i = begin; i < end - 1; i++) {
                Integer diff = flushCards.get(i).symbol().getValue() - flushCards.get(i + 1).symbol().getValue(); 
                if(diff != 1) {
                    consecutives = false;
                    break; 
                }
            }

            if(consecutives) {
                int biggestSymbolValue = flushCards.get(begin).symbol().getValue();
                score.setNibble(25,biggestSymbolValue);
                return Optional.of(score);
            }

            begin++;
            end++;
        }
        return Optional.empty();
    }

    public Optional<PokerScore> evaluateFourOfAKind(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        Card.Symbol fourKindSymbol = null;
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            if(count == 4) {
                fourKindSymbol = entry.getKey();
                break;
            }
        } 

        if(fourKindSymbol == null) return Optional.empty();

        int symbolValue = fourKindSymbol == Card.Symbol.ACE ? 14 : fourKindSymbol.getValue();
        PokerScore score = new PokerScore();
        score.setNibble(24,symbolValue);

        int max = 0;
        for(Card card : cards) {
            int currValue = card.symbol().getValue();
            if(card.symbol() != fourKindSymbol && card.symbol() == Card.Symbol.ACE) {
                max = 14;
                break;
            }

            if(card.symbol() != fourKindSymbol && currValue > max) {
                max = currValue;
            }
        } 
        score.setNibble(23,max);

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateFullHouse(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        Card.Symbol maxThreeKindSymbol = null;
        int threeKindValue = 0;

        Card.Symbol maxPairSymbol = null;
        int pairValue = 0;

        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol symbol = entry.getKey(); 

            if(count == 3 && symbol == Card.Symbol.ACE) {
                maxThreeKindSymbol = symbol;
                threeKindValue = 14;
            } else if (count == 3 && symbol.getValue() > threeKindValue) {
                maxThreeKindSymbol = symbol;
                threeKindValue = symbol.getValue();
            } else if(count == 2 && symbol == Card.Symbol.ACE) {
                maxPairSymbol = symbol;
                pairValue = 14;
            } else if (count == 2 && symbol.getValue() > pairValue) {
                maxPairSymbol = symbol;
                pairValue = symbol.getValue();
            }
        } 

        if(maxThreeKindSymbol == null || maxPairSymbol == null) return Optional.empty();
        PokerScore score = new PokerScore();
        score.setNibble(22,threeKindValue);
        score.setNibble(21,pairValue);

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateFlush(List<Card> cards,HashMap<Card.Suit,Integer> suitMap) {
        Card.Suit flushSuit = null; 
        for(Map.Entry<Card.Suit,Integer> entry : suitMap.entrySet()) {
            Integer count = entry.getValue();
            if(count >= 5) {
                flushSuit = entry.getKey();
                break;
            }
        }

        if(flushSuit == null) return Optional.empty();

        List<Card> flushCards = new ArrayList<>();
        for(Card card : cards) {
            if(card.suit() == flushSuit)
                flushCards.add(card);
        }

        flushCards.sort((a,b) -> b.symbol().getValue() - a.symbol().getValue());
        PokerScore score = new PokerScore();
        for(int i = 0; i < 5; i++) {
            Card.Symbol symbol = flushCards.get(i).symbol();
            
            if(symbol == Card.Symbol.ACE) {
                score.setNibble(20 - i, 14);
            } else {
                score.setNibble(20 - i, symbol.getValue());
            }
        } 
        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateStraight(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        Integer aces = symbolMap.get(Card.Symbol.ACE);
        Integer kings = symbolMap.get(Card.Symbol.KING);
        Integer queens = symbolMap.get(Card.Symbol.QUEEN);
        Integer jacks = symbolMap.get(Card.Symbol.JACK);
        Integer tens = symbolMap.get(Card.Symbol.TEN);

        PokerScore score = new PokerScore();

        if(aces != null && kings != null && queens != null && jacks != null && tens != null) {
            score.setNibble(15,14);
            return Optional.of(score);
        }

        List<Card> cpy = new ArrayList<>(cards);
        cpy.sort((a,b) -> b.symbol().getValue() - a.symbol().getValue());
        
        int begin = 0;
        int end   = 5;

        while(end <= cpy.size()) {
            boolean consecutives = true;
            for(int i = begin; i < end - 1; i++) {
                Integer diff = cpy.get(i).symbol().getValue() - cpy.get(i + 1).symbol().getValue(); 

                if(diff != 1) {
                    consecutives = false;
                    break; 
                }
            }

            if(consecutives) {
                int biggestValue = cpy.get(begin).symbol().getValue();
                score.setNibble(15,biggestValue);
                return Optional.of(score);
            }

            begin++;
            end++;
        }
        return Optional.empty();
    }

    public Optional<PokerScore> evaluateThreeOfAKind(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        Card.Symbol threeKindSymbol = null;
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            if(count == 3) {
                threeKindSymbol = entry.getKey();
                break;
            }
        } 

        if(threeKindSymbol == null) return Optional.empty();

        int symbolValue = threeKindSymbol == Card.Symbol.ACE ? 14 : threeKindSymbol.getValue();
        PokerScore score = new PokerScore();
        score.setNibble(14,symbolValue);

        int max = 0;
        Card.Symbol maxSymbol = null;
        for(Card card : cards) {
            Card.Symbol symbol = card.symbol();
            if(symbol != threeKindSymbol && symbol == Card.Symbol.ACE) {
                max = 14;
                maxSymbol = symbol;
                break;
            } else if(symbol != threeKindSymbol && max < symbol.getValue()) {
                max = symbol.getValue();
                maxSymbol = symbol;
            }
        }
        
        score.setNibble(13,max);

        int secondMax = 0;
        for(Card card : cards) {
            Card.Symbol symbol = card.symbol();
            if(symbol != threeKindSymbol && symbol != maxSymbol && symbol == Card.Symbol.ACE) {
                secondMax = 14;
                break;
            } else if(symbol != threeKindSymbol && symbol != maxSymbol && secondMax < symbol.getValue() && symbol.getValue() < max) {
                secondMax = symbol.getValue();
            }
        }

        score.setNibble(12,secondMax);

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateTwoPair(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        int pairCount = 0;

        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            if(count == 2) {
                pairCount++;
            }
        } 
        
        if(pairCount < 2) return Optional.empty();
        
        int maxValue = 0;
        Card.Symbol topPairSymbol = null;

        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();

            if(count == 2 && currentSymbol == Card.Symbol.ACE) {
                maxValue = 14;
                topPairSymbol = Card.Symbol.ACE;
                break;
            } else if(count == 2 && currentSymbol.getValue() > maxValue) {
                maxValue = currentSymbol.getValue();
                topPairSymbol = currentSymbol;
            }
        } 
         
        int secondMax = 0;
        Card.Symbol secondPairSymbol = null;
        
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();
            int currentValue = currentSymbol.getValue();  
            if(count == 2 && currentValue > secondMax && currentValue < maxValue) {
                secondMax = currentValue;
                secondPairSymbol = currentSymbol;
            }
        } 

        int thirdMax = 0;
        Card.Symbol thirdSymbol = null;
        
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();
            int currentValue = currentSymbol.getValue();  

            if(count == 1 && currentValue > thirdMax && currentValue < secondMax) {
                thirdMax = currentValue;
                thirdSymbol = currentSymbol;
            }
        } 

        PokerScore score = new PokerScore();
        
        score.setNibble(11,maxValue);
        score.setNibble(10,secondMax);
        score.setNibble(9,thirdMax);

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluatePair(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
        int pairCount = 0;

        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            if(count == 2) {
                pairCount++;
            }
        } 
        
        if(pairCount < 1) return Optional.empty();
        
        int maxValue = 0;
        Card.Symbol topPairSymbol = null;

        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();

            if(count == 2 && currentSymbol == Card.Symbol.ACE) {
                maxValue = 14;
                topPairSymbol = Card.Symbol.ACE;
                break;
            } else if(count == 2 && currentSymbol.getValue() > maxValue) {
                maxValue = currentSymbol.getValue();
                topPairSymbol = currentSymbol;
            }
        } 
         
        int secondMax = 0;
        Card.Symbol secondPairSymbol = null;
        
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();
            int currentValue = currentSymbol.getValue();  
            if(count == 1 && currentValue > secondMax && currentValue < maxValue) {
                secondMax = currentValue;
                secondPairSymbol = currentSymbol;
            }
        } 

        int thirdMax = 0;
        Card.Symbol thirdSymbol = null;
        
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();
            int currentValue = currentSymbol.getValue();  

            if(count == 1 && currentValue > thirdMax && currentValue < secondMax) {
                thirdMax = currentValue;
                thirdSymbol = currentSymbol;
            }
        } 
        
        int fourthMax = 0;
        Card.Symbol fourthSymbol = null;
        
        for(Map.Entry<Card.Symbol,Integer> entry : symbolMap.entrySet()) {
            Integer count = entry.getValue();
            Card.Symbol currentSymbol = entry.getKey();
            int currentValue = currentSymbol.getValue();  

            if(count == 1 && currentValue > fourthMax && currentValue < thirdMax) {
                fourthMax = currentValue;
                fourthSymbol = currentSymbol;
            }
        } 

        PokerScore score = new PokerScore();

        score.setNibble(8,maxValue);
        score.setNibble(7,secondMax);
        score.setNibble(6,thirdMax);
        score.setNibble(5,fourthMax);

        return Optional.of(score);
    }

    public Optional<PokerScore> evaluateHighCard(List<Card> cards) {
        boolean aceFound = false;
        for(Card card : cards) {
            if(card.symbol() == Card.Symbol.ACE) {
                aceFound = true;
                break;
            }
        }

        List<Card> cpy = new ArrayList<>(cards);
        cpy.sort((a,b) -> b.symbol().getValue() - a.symbol().getValue());

        PokerScore score = new PokerScore();

        if(aceFound)
            score.setNibble(4,14);

        int n = aceFound ? 4 : 5;
        for(int i = 0; i < n; i++) {
            int value = cpy.get(i).symbol().getValue();
            score.setNibble(n - i - 1,value);
        }
        return Optional.of(score);
    }
    

    public HashMap<Card.Suit,Integer> countSuits(List<Card> cards) {
        HashMap<Card.Suit,Integer> suitMap = new HashMap<>();
        
        for(Card card : cards) {
            Card.Suit suit = card.suit();
            Integer currentSuit = suitMap.get(suit);

            if(currentSuit == null) {
                suitMap.put(suit, 1);
            } else {
                suitMap.put(suit, currentSuit + 1);
            }
        } 

        return suitMap; 
    }

    public HashMap<Card.Symbol,Integer> countSymbols(List<Card> cards) {
        HashMap<Card.Symbol,Integer> symbolMap = new HashMap<>();
        
        for(Card card : cards) {
            Card.Symbol symbol = card.symbol();
            Integer currentSymbol = symbolMap.get(symbol);

            if(currentSymbol == null) {
                symbolMap.put(symbol, 1);
            } else {
                symbolMap.put(symbol, currentSymbol + 1);
            }
        } 

        return symbolMap; 
    }

    public Integer evaluateHand(PokerPlayer player) {
        if(player.first == null || player.second == null) return 0;

        assert flopCards != null : "Evaluate cannot be called when the flop is null";
        assert turnCard != null : "Evaluate cannot be called when the turn is null";
        assert riverCard != null : "Evaluate cannot be called when the river is null";

        List<Card> hand = new ArrayList<Card>();

        hand.add(player.first);
        hand.add(player.second);
        hand.add(flopCards[0]);
        hand.add(flopCards[1]);
        hand.add(flopCards[2]);
        hand.add(turnCard);
        hand.add(riverCard);

        hand.sort((a,b) -> b.symbol().getValue() - a.symbol().getValue());

        HashMap<Card.Symbol,Integer> symbolMap = countSymbols(hand);
        HashMap<Card.Suit,Integer> suitMap = countSuits(hand);
        
        Optional<PokerScore> handScore = evaluateRoyaleFlush(hand,suitMap);
        if(handScore.isEmpty() ) {
            System.out.println("No royale flush for u lol!!!");
        }

        return 0;
    }

    private void finishRound() {
        roundCount++;
    }

    public void play() {
        while(players.size() > 1) {
            roundSetup();
            bet();
            //bettingStage = BettingStage.FLOP;
            flop();
            bet();
            //bettingStage = BettingStage.TURN;
            turn();
            bet();
            //bettingStage = BettingStage.RIVER;
            river();
            bet();
            for(PokerPlayer player : players) {
                evaluateHand(player);
            }
            finishRound();
        }
    }

    public void print() {
        System.out.println("Round count: " + roundCount);
        System.out.println("Small blind: " + smallBlind);
        System.out.println("Button index: " + buttonIndex);
        System.out.println("Speaking index: " + speakingIndex);
        System.out.println("Minimum raise: " + minimumRaise);
        
       // System.out.println(" ");
       // System.out.println("Card deck " + "(" + cards.size() + "):");
       // cards.print();

        System.out.println(" ");
        System.out.println("Folded cards:");
        for(Card card : foldedCards) {
            card.print();
        }

        System.out.println(" ");
        for(PokerPlayer player : players) {
            player.print();
            System.out.println(" ");
        }

        System.out.println(" ");
        System.out.println("Flop:");
        if(flopCards != null) {
            for(Card card : flopCards) {
                card.print();
            }
        } else {
            System.out.println("null");
        }
        System.out.println("Turn:");
        if(turnCard != null) {
            turnCard.print();
        } else {
            System.out.println("null");
        }
        System.out.println("River:");
        if(riverCard != null) {
            riverCard.print();
        } else {
            System.out.println("null");
        }
    }
}
