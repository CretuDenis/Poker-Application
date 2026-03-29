package com.example.Poker.controller;

import com.example.Poker.dto.GameDto.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import org.springframework.beans.factory.annotation.*;
import java.util.ArrayList;
import java.util.List;
import com.example.Poker.service.MatchRoomService;

@Controller
public class MatchRoomController {
    @Autowired private MatchRoomService matchRoomService;

    public MatchRoomController(MatchRoomService matchRoomService) {
        this.matchRoomService = matchRoomService; 
    }

    @MessageMapping("/game/{gameId}")
    public void requestMove(@DestinationVariable Long gameId,Principal principal,MoveDTO move) {
        if (principal == null) {
            System.out.println("ERROR: Principal is null. Is the user authenticated?");
            return null;
        }
        String username = principal.getName();

        matchRoomService.processMove(gameId,username,move);
    }
}
