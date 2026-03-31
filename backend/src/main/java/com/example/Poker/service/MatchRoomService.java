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
        Poker pokerGame = new Poker(playerUsernames);
        pokerGame.print();
        activeGames.putIfAbsent(gameId,pokerGame);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, pokerGame.toDto());

        gameId++;

        for(String username : playerUsernames) {
            System.out.println("Seinding hand to player: " + username);
            messagingTemplate.convertAndSendToUser(username,"/queue/game", pokerGame.getPlayerHand(username));
        }
    }

    public PokerDTO processMove(Long gameId,String username, MoveDTO move) {
        Poker selectedGame = activeGames.get(gameId);
        if(selectedGame == null) return null;

        boolean playerIsPlaying = selectedGame.playerIsPlaying(username);

        if(!playerIsPlaying) return null;

        if(selectedGame.handleMessage(username,move) != Poker.PokerError.SUCCESS) return null; 
        return selectedGame.toDto();
    }
}
