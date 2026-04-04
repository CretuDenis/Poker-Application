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
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MatchRoomController {
    @Autowired private MatchRoomService matchRoomService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    public MatchRoomController(MatchRoomService matchRoomService,SimpMessagingTemplate messagingTemplate) {
        this.matchRoomService = matchRoomService; 
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/game/{gameId}")
    @ResponseBody
    public PokerDTO getState(@PathVariable Long gameId,Principal principal) {
        PokerDTO dto = matchRoomService.getState(gameId); 
        if(dto == null) System.out.println("Poker was null");
        return dto;
    }

    @MessageMapping("/game/{gameId}/disconnect")
    @SendTo("/topic/game/{gameId}")
    public PokerDTO disconnect(@DestinationVariable Long gameId,Principal principal) {
        if(principal == null) {
            System.out.println("Cannot disconnect null user");
            return null;
        }
        String username = principal.getName();
        return matchRoomService.handleDisconnect(gameId,username);
    }

    @MessageMapping("/game/{gameId}/move")
    @SendTo("/topic/game/{gameId}")
    public PokerDTO requestMove(@DestinationVariable Long gameId,Principal principal,@Payload MoveDTO move) {
        if (principal == null) {
            System.out.println("ERROR: Principal is null. Is the user authenticated?");
            return null;
        }
        String username = principal.getName();
        System.out.println(username + " requested a move");
        return matchRoomService.processMove(gameId,username,move);
    }
}
