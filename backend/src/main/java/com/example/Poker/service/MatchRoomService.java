package com.example.Poker.service;

import com.example.Poker.dto.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import com.example.Poker.game.Poker;
import java.util.List;

@Service
public class MatchRoomService {
    @Autowired private SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<Long,Poker> activeGames;
    private Long gameId; 

    public MatchRoomService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ConcurrentHashMap<>();
        this.gameId = 0L;
    }
    
    public void createGame(List<String> playerUsernames) {
        for(String s : playerUsernames) {
            System.out.println(s);
        }
        Poker pokerGame = new Poker(playerUsernames);
        pokerGame.print();
        activeGames.putIfAbsent(gameId,pokerGame);
        gameId++;
    }

    ("/topic/game/{gameId}/updates")
    public void processMove(Long gameId,String username,MoveDTO move) {
        Poker selectedGame = activeGames.get(gameId);
        if(selectedGame == null) return;

        boolean playerIsPlaying = selectedPlayer.playerIsPlaying(username);

        if(!playerIsPlaying) return;

        
            
    }
}
