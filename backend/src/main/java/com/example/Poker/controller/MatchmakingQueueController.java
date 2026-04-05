package com.example.Poker.controller;

import com.example.Poker.service.MatchRoomService;
import com.example.Poker.service.MatchmakingQueueService;
import com.example.Poker.dto.Message;
import com.example.Poker.dto.QueueMessage;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @MessageMapping("/queue")
    public void processQueueMessage(Principal principal,Message<QueueMessage> message) {
        if (principal == null) {
            System.out.println("Unauthentificated users cannot interact with the matchmaking queue");
            return;
        }

        String username = principal.getName();
        if (message.content().info().equals("join")) {
            queueService.processJoinRequest(username);
            messagingTemplate.convertAndSendToUser(username, "/queue/private",new Message<QueueMessage>(new QueueMessage("joined")));
        } else if (message.content().info().equals("leave")) {
            queueService.processLeaveRequest();
            messagingTemplate.convertAndSendToUser(username, "/queue/private",new Message<QueueMessage>(new QueueMessage("left")));
        } else {
            System.out.println("Invalid message recieved in the matchmaking queue");
        }
    }
}
