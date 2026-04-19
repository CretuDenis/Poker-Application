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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

@Service
public class MatchRoomService {
    @Autowired private SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<Long,Poker> activeGames;
    private final ConcurrentHashMap<Long,Poker> prevGameState;
    private Long gameId; 

    public MatchRoomService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ConcurrentHashMap<>();
        this.prevGameState = new ConcurrentHashMap<>();
        this.gameId = 0L;
    }

    public HandDTO getPlayerHand(Long gameId, String username) {
        Poker game = activeGames.get(gameId);
        if(game == null) return null;
        return game.getPlayerHand(username);
    }

    public Poker getState(Long gameId) {
        return activeGames.get(gameId);
    }

    public Poker handleDisconnect(Long gameId, String username) {
        Poker game = activeGames.get(gameId);
        if(game == null) return null;
        Poker prev = new Poker(game);
        prevGameState.put(gameId, prev);
        game.removePlayer(username);
        return game;
    }
    
    public Long createGame(List<String> playerUsernames) {
        Poker pokerGame = new Poker(playerUsernames);
        activeGames.putIfAbsent(gameId,pokerGame);
        Long currGameId = gameId;
        gameId++;

        for(String username : playerUsernames) {
            messagingTemplate.convertAndSendToUser(username,"/queue/private", Map.of("game",currGameId));
        }
        return currGameId;
    }

    public Poker getPrevState(Long gameId) {
        return prevGameState.get(gameId);
    }

    public Poker processMove(Long gameId,String username, MoveDTO move) {
        Poker selectedGame = activeGames.get(gameId);
        if(selectedGame == null) return null;

        boolean playerIsPlaying = selectedGame.playerIsPlaying(username);
        Poker prevState = new Poker(selectedGame);
 
        if(!playerIsPlaying) return null;

        Poker.PokerError err = selectedGame.handleMessage(username,move); 
        if(err != Poker.PokerError.SUCCESS) return null; 
        prevGameState.put(gameId, prevState);
        return selectedGame;
    }
}
