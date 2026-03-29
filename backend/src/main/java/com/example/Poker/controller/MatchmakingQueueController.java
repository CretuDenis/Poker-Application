package com.example.Poker.controller;

import com.example.Poker.dto.GameDto.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.example.Poker.service.MatchmakingQueueService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.*;
import java.util.ArrayList;
import java.util.List;
import com.example.Poker.service.MatchRoomService;

@Controller
public class MatchmakingQueueController {
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private MatchmakingQueueService queueService;
    @Autowired private MatchRoomService matchRoomService;

    public MatchmakingQueueController(SimpMessagingTemplate messagingTemplate,MatchmakingQueueService queueService,MatchRoomService matchRoomService) {
        this.messagingTemplate = messagingTemplate;
        this.queueService = queueService;
        this.matchRoomService = matchRoomService; 
    }

    @MessageMapping("/matchmaking/join")
    public void joinQueue(Principal principal) {
        if (principal == null) {
            System.out.println("ERROR: Principal is null. Is the user authenticated?");
            return;
        }

        String username = principal.getName();
        queueService.processJoinRequest(username);
        //messagingTemplate.convertAndSendToUser(username, "/queue/reply", "You are now in the queue!");
    }
}
