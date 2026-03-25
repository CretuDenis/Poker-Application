package com.example.Poker.service;

import com.example.Poker.dto.GameDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;

    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void startGame(Long gameId) {
        GameDto.GameState state = new GameDto.GameState(
            gameId, "john", "PREFLOP", 0, "GAME_STARTED"
        );

        messagingTemplate.convertAndSend("/topic/game/" + gameId, state);
    }
}
