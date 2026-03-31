package com.example.Poker.controller;

import com.example.Poker.service.MatchRoomService;
import com.example.Poker.service.MatchmakingQueueService;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;


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

    @MessageMapping("/queue/join")
    public void joinQueue(Principal principal) {
        if (principal == null) {
            System.out.println("ERROR: Principal is null. Is the user authenticated?");
            return;
        }

        String username = principal.getName();
        queueService.processJoinRequest(username);
        messagingTemplate.convertAndSendToUser(username, "/queue/reply", "JOINED");
    }
}
