package com.example.Poker.controller;

import com.example.Poker.service.MatchRoomService;
import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MatchRoomController {
    @Autowired private MatchRoomService matchRoomService;

    public MatchRoomController(MatchRoomService matchRoomService) {
        this.matchRoomService = matchRoomService; 
    }

    @MessageMapping("/game/{gameId}/move")
    @SendTo("/topic/game/{gameId}")
    public PokerDTO requestMove(@DestinationVariable Long gameId,Principal principal,@Payload MoveDTO move) {
        if (principal == null) {
            System.out.println("ERROR: Principal is null. Is the user authenticated?");
            return null;
        }
        String username = principal.getName();

        return matchRoomService.processMove(gameId,username,move);
    }
}
