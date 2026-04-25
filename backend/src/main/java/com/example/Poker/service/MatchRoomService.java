package com.example.Poker.service;

import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.dto.GameStateDTO;
import com.example.Poker.dto.HandDTO;
import com.example.Poker.game.Poker;
import com.example.Poker.dto.Message;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

@Service
public class MatchRoomService {
    @Autowired private SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String,Poker> activeGames;
    private final ConcurrentHashMap<String,Poker> prevGameState;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public MatchRoomService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ConcurrentHashMap<>();
        this.prevGameState = new ConcurrentHashMap<>();
    }

    public HandDTO getPlayerHand(String gameId, String username) {
        Poker game = activeGames.get(gameId);
        if(game == null) return null;
        return game.getPlayerHand(username);
    }

    public Poker getState(String gameId) {
        return activeGames.get(gameId);
    }

    public Poker handleDisconnect(String gameId, String username) {
        Poker game = activeGames.get(gameId);
        if(game == null) return null;
        Poker prev = new Poker(game);
        prevGameState.put(gameId, prev);
        game.removePlayer(username);
        return game;
    }
    
    public String generateRandomString(int length) {
        byte[] randomBytes = new byte[(length * 3) / 4];
        secureRandom.nextBytes(randomBytes);
        
        String result = base64Encoder.encodeToString(randomBytes);
        return result.substring(0, length);
    }

    public String createGame(List<String> playerUsernames) {
        Poker pokerGame = new Poker(playerUsernames);
        String gameId = generateRandomString(64);
        Poker game = activeGames.get(gameId);
        while(game != null) {
            gameId = generateRandomString(64);
            game = activeGames.get(gameId);
        }
        activeGames.put(gameId,pokerGame);

        for(String username : playerUsernames) {
            messagingTemplate.convertAndSendToUser(username,"/queue/private", Map.of("game",gameId));
        }

        return gameId;
    }

    public void setPrevState(String gameId, Poker state) {
        prevGameState.put(gameId,state);
    }

    public void setCurrState(String gameId, Poker state) {
        activeGames.put(gameId,state);
    }

    public Poker getPrevState(String gameId) {
        return prevGameState.get(gameId);
    }

    @Async
    public CompletableFuture<Void> finishHand(String gameId, Poker currState) {
        List<String> playerNames = currState.getPlayerNames();
        List<List<Poker>> states = currState.getStatesToFinishHand();
        
        assert states.get(0).size() == states.get(1).size();
        int numMessages = states.get(0).size();

        if (currState.getActivePlayers() == 1) {
            Poker prev = states.get(0).get(0);
            Poker curr = states.get(1).get(0);

            setPrevState(gameId,prev);
            setCurrState(gameId,curr);

            for (String playerName : playerNames) {
                GameStateDTO newGameState = newGameState = new GameStateDTO(prev.toDto(playerName), curr.toDto());
                Message<GameStateDTO> msg = new Message<>(newGameState);
                messagingTemplate.convertAndSendToUser(playerName, "/queue/private", msg);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CompletableFuture.failedFuture(e);
            }

            prev = states.get(0).get(1);
            curr = states.get(1).get(1);

            setPrevState(gameId,prev);
            setCurrState(gameId,curr);

            for (String playerName : playerNames) {
                GameStateDTO newGameState = newGameState = new GameStateDTO(prev.toDto(),curr.toDto(playerName));
                Message<GameStateDTO> msg = new Message<>(newGameState);
                messagingTemplate.convertAndSendToUser(playerName, "/queue/private", msg);
            }

            return CompletableFuture.completedFuture(null);
        }

        for (int i = 0; i < numMessages; i++) {
           Poker prev = states.get(0).get(i);
           Poker curr = states.get(1).get(i);

           setPrevState(gameId,prev);
           setCurrState(gameId,curr);

            for (String playerName : playerNames) {
                GameStateDTO newGameState = newGameState = new GameStateDTO(prev.toDto(),i != numMessages-1 ? curr.toDto() : curr.toDto(playerName));
                Message<GameStateDTO> msg = new Message<>(newGameState);
                messagingTemplate.convertAndSendToUser(playerName, "/queue/private", msg);
            }

            try {
                if (i == 0 || i == numMessages-2) {
                    Thread.sleep(4000);
                } else {
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CompletableFuture.failedFuture(e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public Poker processMove(String gameId,String username, MoveDTO move) {
        Poker selectedGame = activeGames.get(gameId);
        assert selectedGame != null : "How can the game state be null?";
        if (selectedGame.shouldFinishHand()) {
            System.out.println(username + " tried to make a move while the cutscene is playing");
            return null;
        } 

        boolean playerIsPlaying = selectedGame.playerIsPlaying(username);
        if(!playerIsPlaying) return null;

        Poker prevState = new Poker(selectedGame);
        Poker.PokerError err = selectedGame.handleMessage(username,move); 
        System.out.println(err.name());
        if(err != Poker.PokerError.SUCCESS) return null; 
        prevGameState.put(gameId, prevState);
        return selectedGame;
    }
}
