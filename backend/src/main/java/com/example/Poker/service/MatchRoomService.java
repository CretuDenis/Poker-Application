package com.example.Poker.service;

import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.dto.HandDTO;
import com.example.Poker.game.Poker;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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

    public PokerDTO getState(Long gameId) {
        Poker game = activeGames.get(gameId);
        if(game != null) return game.toDto();
        return null;
    }

    public PokerDTO handleDisconnect(Long gameId, String username) {
        Poker game = activeGames.get(gameId);
        if(game == null) return null;
        game.removePlayer(username);
        return game.toDto();
    }
    
    public Long createGame(List<String> playerUsernames) {
        Poker pokerGame = new Poker(playerUsernames);
        activeGames.putIfAbsent(gameId,pokerGame);
        Long currGameId = gameId;
        gameId++;
        pokerGame.print();

        for(String username : playerUsernames) {
            messagingTemplate.convertAndSendToUser(username,"/queue/private", Map.of("game",currGameId));
        }
        System.out.println("Created game " + currGameId);

        return currGameId;
    }

    public PokerDTO processMove(Long gameId,String username, MoveDTO move) {
        Poker selectedGame = activeGames.get(gameId);
        if(selectedGame == null) return null;

        boolean playerIsPlaying = selectedGame.playerIsPlaying(username);

        if(!playerIsPlaying) return null;

        Poker.PokerError err = selectedGame.handleMessage(username,move); 
        System.out.println(err.name());
        if(err != Poker.PokerError.SUCCESS) return null; 
        return selectedGame.toDto();
    }
}
