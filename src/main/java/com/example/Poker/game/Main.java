import com.example.Poker.game.CardDeck;
import com.example.Poker.game.Card;
import com.example.Poker.game.Poker;
import com.example.Poker.game.PokerScore;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Optional;

public class Main {
    public static void main(String[] args) { 
        List<String> players = List.of("mrRafala","ajkim","cok_pardon","skibidi");
        Poker poker = new Poker(players);
        List<Card> cards = List.of(
            new Card(Card.Symbol.JACK,Card.Suit.HEARTS),
            new Card(Card.Symbol.SEVEN,Card.Suit.DIAMONDS),
            new Card(Card.Symbol.FIVE,Card.Suit.SPADES),
            new Card(Card.Symbol.FOUR,Card.Suit.CLUBS),
            new Card(Card.Symbol.THREE,Card.Suit.HEARTS),
            new Card(Card.Symbol.TWO,Card.Suit.HEARTS),
            new Card(Card.Symbol.ACE,Card.Suit.HEARTS)
        );

        var suitMap     = poker.countSuits(cards);
        var symbolMap   = poker.countSymbols(cards);

        PokerScore tsScore1 = new PokerScore();
        PokerScore tsScore2 = new PokerScore();
        tsScore1.setNibble(0,12);
        tsScore2.setNibble(21,14);
        tsScore2.setRoyalFlush();

        tsScore1.print();
        tsScore2.print();

        System.out.println(tsScore1.lessThan(tsScore2));
        
        //System.out.printf("%016X%n", score);
    }
}
