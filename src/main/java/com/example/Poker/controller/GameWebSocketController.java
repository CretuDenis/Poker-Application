package com.example.Poker.controller;

import com.example.Poker.dto.GameDto.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/{gameId}/action")
    @SendTo("/topic/game/{gameId}")
    public GameState handleAction(@DestinationVariable Long gameId,GameAction action) {
        return new GameState(gameId, "John","BRUH",123123,"SOME_OTHER");
    }
}
