import com.example.Poker.game.CardDeck;
import com.example.Poker.game.Card;
import com.example.Poker.game.Poker;
import com.example.Poker.game.PokerScore;
import com.example.Poker.game.PokerMessage;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Optional;

public class Main {
    public static void main(String[] args) { 
        List<String> players = List.of("mrRafala","ajkim","cok_pardon","skibidi");
        Poker poker = new Poker(players);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            poker.print();
            System.out.print("Enter command (name ACTION [amount]): ");
            String line = scanner.nextLine();
            String[] parts = line.split(" ");

            String name = parts[0];
            String action = parts[1];
            Optional<Integer> amount = parts.length > 2 ? Optional.of(Integer.parseInt(parts[2])) : Optional.empty();

            PokerMessage message = new PokerMessage(name, action, amount);
            Poker.PokerError err = poker.handleMessage(message);
            if (err != Poker.PokerError.SUCCESS)
                System.out.println("Error: " + err.name());
            System.out.println("");
        }    
    }
}
