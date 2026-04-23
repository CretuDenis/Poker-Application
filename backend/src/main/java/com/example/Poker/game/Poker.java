package com.example.Poker.game;

import com.example.Poker.game.PokerPlayer;
import com.example.Poker.game.CardDeck;
import com.example.Poker.game.PokerScore;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.dto.HandDTO;
import com.example.Poker.dto.PokerPlayerDTO;
import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.CardDTO;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.lang.Math;
import java.util.concurrent.CopyOnWriteArrayList;

public class Poker {
    public static final Integer numPlayers = 3;
    
    public enum PokerError {
        SUCCESS,
        MESSAGE_REQUESTED_BY_NON_SPEAKING,
        MESSAGE_REQUESTED_BY_FOLDED,
        PLAYER_SHOULD_CHECK,
        INSUFICIENT_BALANCE,
        PLAYER_CANNOT_CHECK,
        PLAYER_CANNOT_FOLD,
        YOU_ARE_NOT_THE_REMAINING_PLAYER,
        RAISE_AMOUNT_IS_NULL,
    }

    private enum CommunityCards {
        FLOP,
        TURN,
        RIVER
    }
    private enum BettingRound {
        PRE_FLOP,
        POST_FLOP,
        POST_TURN,
        POST_RIVER
    }

    private final Integer baseBalance = 1000;
    private final Integer startSmallBlind = 15;
    private Integer roundCount;
    private Integer smallBlind;
    private Integer buttonIndex;
    private Integer speakingIndex;
    private Integer raiseIndex;
    private Integer pot;
    private BettingRound bettingRound;
    private CardDeck cards;
    private List<PokerPlayer> players;
    private Card[] communityCards;

    public Poker(List<String> playerNames) {
        communityCards  = new Card[5];
        smallBlind      = startSmallBlind;
        bettingRound    = BettingRound.PRE_FLOP;
        roundCount      = 0;
        buttonIndex     = 0;
        raiseIndex      = null;
        cards           = new CardDeck();
        players         = new CopyOnWriteArrayList<>();

        for(String name : playerNames) {
            players.add(new PokerPlayer(baseBalance,name));
        }

        roundSetup();

    }

    public Poker(Poker other) {
        if (other == null) {
            throw new IllegalArgumentException("Source game cannot be null");
        }

        this.roundCount = other.roundCount;
        this.pot = other.pot;
        this.smallBlind = other.smallBlind;
        this.buttonIndex = other.buttonIndex;
        this.speakingIndex = other.speakingIndex;
        this.raiseIndex = other.raiseIndex;
        this.bettingRound = other.bettingRound;

        this.cards = new CardDeck(other.cards);

        this.players = new CopyOnWriteArrayList<>();
        for (PokerPlayer p : other.players) {
            this.players.add(new PokerPlayer(p));
        }

        this.communityCards = new Card[5];
        System.arraycopy(other.communityCards, 0, this.communityCards, 0, 5);
    }

    

    private int getMaxBet() {
        int maxBet = 0;
        for(PokerPlayer player : players) {
            if(player.bet > maxBet)
                maxBet = player.bet;
        }
        return maxBet;
    }

    private int getPot() {
        return this.pot;
    }

    private int nextPlaying(int current) {
        int next = (current + 1) % players.size();

        // def a player can disconnect in the middle of this maybe I'll have a 
        while (players.get(next).folded() || players.get(next).balance == 0) 
            next = (next + 1) % players.size();
        return next;
    }

    private int previousPlaying(int current) {
        int prev = (((current - 1) % players.size()) + players.size()) % players.size();

        // def a player can disconnect in the middle of this maybe I'll have a 
        while (players.get(prev).folded()) 
            prev = (((prev - 1) % players.size()) + players.size()) % players.size();
        return prev;
    }

    private void roundSetup() {
        cards = new CardDeck();
        pot = 0;
        raiseIndex = null;
        clearCommunityCards();
        ++roundCount;
        bettingRound = BettingRound.PRE_FLOP;
        players.removeIf(p -> p.balance <= 0);

        for(PokerPlayer player : players) {
            player.bet = 0;
            player.first = new Card(Card.Symbol.ACE,Card.Suit.CLUBS);
            player.second = new Card(Card.Symbol.ACE,Card.Suit.CLUBS);
            player.spoken = false;
        } 

        int numPlayers = players.size();
        cards.shuffle();

        if(roundCount > 1) {
            buttonIndex = nextPlaying(buttonIndex);
        }

        int currentDealt = nextPlaying(buttonIndex);

        int dealtCards = 0;
        while(dealtCards != numPlayers) {
            Card dealtCard = cards.dealCard();
            PokerPlayer player = players.get(currentDealt);
            player.first = dealtCard;
            
            currentDealt = nextPlaying(currentDealt);
            dealtCards++;
        }

        dealtCards = 0;

        while(dealtCards != numPlayers) {
            Card dealtCard = cards.dealCard();
            PokerPlayer player = players.get(currentDealt);
            player.second = dealtCard;
            
            currentDealt = nextPlaying(currentDealt);
            dealtCards++;
        }

        speakingIndex = firstToSpeakIndex();
        int smallBlindIndex = smallBlindIndex();
        int bigBlindIndex   = bigBlindIndex();
        
        assert smallBlindIndex != bigBlindIndex : "Cannot be small and big blind at the same time";

        PokerPlayer smallBlindPlayer = players.get(smallBlindIndex);
        PokerPlayer bigBlindPlayer = players.get(bigBlindIndex);

        if(!smallBlindPlayer.tryBet(smallBlind)) {
            smallBlindPlayer.allIn();
        }
        
        if(!bigBlindPlayer.tryBet(smallBlind * 2)) {
            bigBlindPlayer.allIn();
        }

        pot += smallBlindPlayer.bet;
        pot += bigBlindPlayer.bet;
    } 

    private void dealCommunityCard() {
        switch(bettingRound) {
            case PRE_FLOP:
                for(int i = 0; i < 3; i++) {
                    communityCards[i] = cards.dealCard();
                }
                break;
            case POST_FLOP:
                communityCards[3] = cards.dealCard();
                break;
            case POST_TURN:
                communityCards[4] = cards.dealCard();
                break;
            default:
                break;
        }
    }

    private void clearCommunityCards() {
        for(int i = 0; i < 5; i++) {
            communityCards[i] = null;
        }
    }

    private boolean allActiveBetsAreEqual() {
        int maxBet = getMaxBet();

        for(PokerPlayer player : players) {
            if(player.bet < maxBet && !player.folded())
                return false;
        }

        return true;
    }

    public int getActivePlayers() {
        int count = 0;
        for(PokerPlayer player : players) {
            if(!player.folded())
                count++;
        }
        return count;
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
    
    private Optional<PokerScore> evaluateRoyaleFlush(List<Card> cards, HashMap<Card.Suit,Integer> suitMap) {
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

    private Optional<PokerScore> evaluateStraightFlush(List<Card> cards,HashMap<Card.Suit,Integer> suitMap) {
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

    private Optional<PokerScore> evaluateFourOfAKind(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluateFullHouse(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluateFlush(List<Card> cards,HashMap<Card.Suit,Integer> suitMap) {
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

    private Optional<PokerScore> evaluateStraight(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluateThreeOfAKind(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluateTwoPair(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluatePair(List<Card> cards,HashMap<Card.Symbol,Integer> symbolMap) {
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

    private Optional<PokerScore> evaluateHighCard(List<Card> cards) {
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
    

    private HashMap<Card.Suit,Integer> countSuits(List<Card> cards) {
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

    private HashMap<Card.Symbol,Integer> countSymbols(List<Card> cards) {
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

    private PokerScore evaluateHand(PokerPlayer player) {
        if (communityCards[0] == null || communityCards[3] == null || communityCards[4] == null) return new PokerScore();

        if(player.folded()) return new PokerScore();

        List<Card> hand = new ArrayList<Card>();

        hand.add(player.first);
        hand.add(player.second);
        hand.add(communityCards[0]);
        hand.add(communityCards[1]);
        hand.add(communityCards[2]);
        hand.add(communityCards[3]);
        hand.add(communityCards[4]);

        HashMap<Card.Symbol,Integer> symbolMap = countSymbols(hand);
        HashMap<Card.Suit,Integer> suitMap = countSuits(hand);
        
        Optional<PokerScore> handScore = evaluateRoyaleFlush(hand,suitMap)
            .or(() -> evaluateStraightFlush(hand,suitMap))
            .or(() -> evaluateFourOfAKind(hand,symbolMap))
            .or(() -> evaluateFullHouse(hand,symbolMap))
            .or(() -> evaluateFlush(hand,suitMap))
            .or(() -> evaluateStraight(hand,symbolMap))
            .or(() -> evaluateThreeOfAKind(hand,symbolMap))
            .or(() -> evaluateTwoPair(hand,symbolMap))
            .or(() -> evaluatePair(hand,symbolMap))
            .or(() -> evaluateHighCard(hand));

        return handScore.get();
    }

    private List<PokerPlayer> determineWinners(List<PokerScore> scores) {
        PokerScore max = new PokerScore();
        List<PokerPlayer> winners = new ArrayList<>();

        for(int i = 0; i < players.size(); i++) {
            PokerScore score = scores.get(i);
            
            if(score.greaterThan(max)) {
                PokerPlayer winner = players.get(i);
                max = score;
                winners.clear();
                winners.add(winner);
            } else if (score.equalTo(max)) {
                PokerPlayer anotherWinner = players.get(i);
                winners.add(anotherWinner);
            }
        }

        return winners;
    }

    private int smallBlindIndex()   { return (buttonIndex + 1) % players.size(); }
    private int bigBlindIndex()     { return (buttonIndex + 2) % players.size(); }
    private int firstToSpeakIndex() { return bettingRound == BettingRound.PRE_FLOP ? nextPlaying(bigBlindIndex()) : nextPlaying(buttonIndex); }
    private int lastToSpeakIndex()  { 
        if (raiseIndex != null) return previousPlaying(raiseIndex);
        return bettingRound == BettingRound.PRE_FLOP ? bigBlindIndex() : buttonIndex; 
    }

    private boolean flopOnTable() {
        return communityCards[0] != null && communityCards[1] != null && communityCards[2] != null;
    }

    private boolean turnOnTable() {
        return communityCards[3] != null;
    }

    private boolean riverOnTable() {
        return communityCards[4] != null;
    }

    private void updateBettingRound() {
        switch(bettingRound) {
            case PRE_FLOP:
                bettingRound = BettingRound.POST_FLOP;
                break;
            case POST_FLOP:
                bettingRound = BettingRound.POST_TURN;
                break;
            case POST_TURN:
                bettingRound = BettingRound.POST_RIVER;
                break;
            case POST_RIVER:
                bettingRound = BettingRound.PRE_FLOP;
                break;
            default:
                break;
        }
    }

    private void dealFlop() {
        communityCards[0] = cards.dealCard();
        communityCards[1] = cards.dealCard();
        communityCards[2] = cards.dealCard();
    }

    private void dealTurn() {
        communityCards[3] = cards.dealCard();
    }

    private void dealRiver() {
        communityCards[4] = cards.dealCard();
    }

    public HandDTO getPlayerHand(String username) {
        for (PokerPlayer player : players) {
            if (player.name.equals(username) && !player.folded()) {
                return new HandDTO(player.first.toDto(),player.second.toDto());
            }
        }
        return new HandDTO(null,null);
    }
    
    public boolean allActivePlayersSpoken() {
        for(PokerPlayer player : players) {
            if (player.folded()) continue;
            if (!player.spoken) return false;
        }

        return true;
    }

    private boolean shouldFinishBettingRound() {
        boolean existsActiveSeparatePot = players.stream()
            .filter(p -> !p.folded() && p.balance == 0 && p.bet < getMaxBet())
            .count() > 0;

        boolean uncalledPlayersAreAllIn = true;

        for(PokerPlayer player : players) {
            if (player.folded()) continue;
            if (player.bet < getMaxBet() && player.balance != 0) {
                uncalledPlayersAreAllIn = false;
                break;
            } 
        }

        return allActivePlayersSpoken() && (allActiveBetsAreEqual() || uncalledPlayersAreAllIn);
    }

    public boolean shouldFinishHand() {
        long activePlayers = players.stream()
            .filter(p -> !p.folded())
            .count();

        long allInPlayers = players.stream()
            .filter(p -> !p.folded() && p.balance == 0)
            .count();

        boolean shouldFinishBetting = shouldFinishBettingRound();
        boolean finishHand = activePlayers == 1 || 
            (shouldFinishBetting && 
            (activePlayers - allInPlayers <= 1 || bettingRound == BettingRound.POST_RIVER)) ;
        System.out.println("should finish hand " + finishHand);
        
        return finishHand;
    }

    public PokerError handleMessage(String playerName,MoveDTO message) {
        PokerPlayer speakingPlayer = players.get(speakingIndex);

        if (!playerName.equals(speakingPlayer.name)) return PokerError.MESSAGE_REQUESTED_BY_NON_SPEAKING;
        if (speakingPlayer.folded()) return PokerError.MESSAGE_REQUESTED_BY_FOLDED;

        if (message.action().equals("CALL")) {
            int maxBet = getMaxBet();
            int toBet = maxBet - speakingPlayer.bet; 
            if(toBet == 0) return PokerError.PLAYER_SHOULD_CHECK;
            if(!speakingPlayer.tryBet(toBet)) return PokerError.INSUFICIENT_BALANCE;
            pot += toBet;
        } else if (message.action().equals("RAISE")) {
            if (message.amount() == null) return PokerError.RAISE_AMOUNT_IS_NULL;

            Integer amount = message.amount();
            int maxBet = getMaxBet();
            int toBet = maxBet - speakingPlayer.bet + amount;

            if(!speakingPlayer.tryBet(toBet)) return PokerError.INSUFICIENT_BALANCE;
            for (PokerPlayer player : players)
                player.spoken = false;

            //raiseIndex = speakingIndex;
            pot += toBet;
        } else if (message.action().equals("CHECK")) {
            Integer maxBet = getMaxBet();        
            if(speakingPlayer.bet < maxBet) return PokerError.PLAYER_CANNOT_CHECK;
        } else if (message.action().equals("FOLD")) {
            if(speakingPlayer.folded() || getActivePlayers() == 1) return PokerError.PLAYER_CANNOT_FOLD;
            speakingPlayer.fold(); 
        } else if (message.action().equals("ALLIN")) {
            int maxBet = getMaxBet();
            pot += speakingPlayer.balance;
            speakingPlayer.allIn();
            if (speakingPlayer.bet > maxBet) {
                raiseIndex = speakingIndex;
            }
        }

        speakingPlayer.spoken = true;

        if (!shouldFinishBettingRound()) {
            speakingIndex = nextPlaying(speakingIndex);
            return PokerError.SUCCESS;
        } 

        if (bettingRound != BettingRound.POST_RIVER && !shouldFinishHand()) {
            dealCommunityCard();
            updateBettingRound();
            speakingIndex = firstToSpeakIndex();

            for(PokerPlayer player : players) {
                if (player.folded()) continue;
                player.spoken = false;
            }

            raiseIndex = null;
            return PokerError.SUCCESS;
        }

        return PokerError.SUCCESS; 
    }

    private void distributePot() {
        List<PokerScore> scores = new ArrayList<>();

        for(PokerPlayer player : players) {
            scores.add(evaluateHand(player));
        }

        List<PokerPlayer> winners = determineWinners(scores);
        assert winners.size() != 0;
        System.out.println(winners);

        int maxBet = getMaxBet();

        if(winners.size() == 1) {
            PokerPlayer winningPlayer = winners.get(0);

            if (winningPlayer.bet < maxBet) {
                pot -= winningPlayer.bet;
                winningPlayer.claim(2 * winningPlayer.bet);

                int redistributedPot = pot / (getActivePlayers() - 1);
                for (PokerPlayer player : players) {
                    if (!player.folded() && player != winningPlayer) 
                        player.claim(redistributedPot);
                }
            } else {
                winningPlayer.claim(pot);
            }
        } else {
            for(PokerPlayer winner : winners) {
                if (winner.balance == 0 && winner.bet < maxBet) {
                    pot -= winner.bet;
                    winner.claim(winner.bet * 2);
                }
            }
            
            winners.removeIf(p -> p.bet < maxBet);
            
            int redistributedPot = pot / winners.size();

            for(PokerPlayer remainingWinner : winners) {
                remainingWinner.claim(redistributedPot);
            }
        }
    }

    public void removePlayer(String username) {      
        PokerPlayer speakingPlayer = players.get(speakingIndex);
        PokerPlayer buttonPlayer = players.get(buttonIndex);
        if (username.equals(speakingPlayer.name)) {
            speakingIndex = nextPlaying(speakingIndex);
        }
        
        if (username.equals(buttonPlayer.name)) {
            buttonIndex = nextPlaying(buttonIndex);
        }
        players.removeIf(p -> p.name.equals(username));
    }
    
    public PokerDTO toDto(String observer) {
        List<PokerPlayerDTO> playersDto = new ArrayList<>();

        for(PokerPlayer player : players) {
            playersDto.add(player.toDto(observer));
        }

        CardDTO[] communityCardsDtos = new CardDTO[5];
        for(int i = 0; i < 5; i++) {
            if (communityCards[i] != null) {
                communityCardsDtos[i] = communityCards[i].toDto();
            }
        }
        
        return new PokerDTO(playersDto,communityCardsDtos,players.get(buttonIndex).name,players.get(speakingIndex).name,roundCount,pot);
    }

    public PokerDTO toDto() {
        List<PokerPlayerDTO> playersDto = new ArrayList<>();

        for(PokerPlayer player : players) {
            playersDto.add(player.toDto());
        }

        CardDTO[] communityCardsDtos = new CardDTO[5];
        for(int i = 0; i < 5; i++) {
            if (communityCards[i] != null) {
                communityCardsDtos[i] = communityCards[i].toDto();
            }
        }
        
        return new PokerDTO(playersDto,communityCardsDtos,players.get(buttonIndex).name,players.get(speakingIndex).name,roundCount,pot);
    }

    public List<String> getPlayerNames() {
        return players.stream()
          .map(player -> player.name)
          .toList();
    }

    public boolean playerIsPlaying(String username) {
        for(PokerPlayer player : players) {
            if(username.equals(player.name)) return true;
        }
        return false;
    }

    public List<List<Poker>> getStatesToFinishHand() {
        List<Poker> prevStates = new ArrayList<>();
        List<Poker> nextStates = new ArrayList<>();

        Poker current = this;

        long activePlayers = players.stream()
            .filter(p -> !p.folded())
            .count();
        
        if (activePlayers == 1) {
            Poker hiddenCardState = new Poker(current);
            Poker shownCardState = new Poker(current);

            prevStates.add(hiddenCardState);
            nextStates.add(shownCardState);

            Poker newRoundState = new Poker(shownCardState);
            newRoundState.distributePot();
            newRoundState.roundSetup();

            prevStates.add(shownCardState);
            nextStates.add(newRoundState);

            return List.of(prevStates,nextStates);
        }

        if (!flopOnTable()) {
            Poker flopState = new Poker(current);
            flopState.dealFlop();
            prevStates.add(new Poker(current));
            nextStates.add(flopState);
            current = new Poker(flopState);
        }
        if (!turnOnTable()) {
            Poker turnState = new Poker(current);
            turnState.dealTurn();
            prevStates.add(new Poker(current));
            nextStates.add(turnState);
            current = new Poker(turnState);
        }
        if (!riverOnTable()) {
            Poker riverState = new Poker(current);
            riverState.dealRiver();
            prevStates.add(new Poker(current));
            nextStates.add(riverState);
            current = new Poker(riverState);
        }

        if (nextStates.size() == 0) {
            Poker hiddenCardState = new Poker(current);
            Poker shownCardState = new Poker(current);

            prevStates.add(hiddenCardState);
            nextStates.add(shownCardState);

            Poker newRoundState = new Poker(shownCardState);
            newRoundState.distributePot();
            newRoundState.roundSetup();

            prevStates.add(shownCardState);
            nextStates.add(newRoundState);
        } else {
            Poker finalState = new Poker(current);
            finalState.distributePot();
            finalState.roundSetup();
            prevStates.add(new Poker(current));
            nextStates.add(finalState);
        }
    

        assert prevStates.size() == nextStates.size();

        return List.of(prevStates, nextStates);
    }
}
