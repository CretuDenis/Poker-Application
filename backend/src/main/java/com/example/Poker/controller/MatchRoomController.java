package com.example.Poker.controller;

import com.example.Poker.service.MatchRoomService;
import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.dto.HandDTO;
import com.example.Poker.dto.Message;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MatchRoomController {
    private final MatchRoomService matchRoomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public MatchRoomController(MatchRoomService matchRoomService,SimpMessagingTemplate messagingTemplate,ObjectMapper objectMapper) {
        this.matchRoomService = matchRoomService; 
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/game/{gameId}")
    public void handleMessage(@DestinationVariable Long gameId,Principal principal,@Payload String rawMessage) throws Exception {
        if(principal == null) {
            System.out.println("Game room cannot handle unauthentificated users");
            return;
        }
        String username = principal.getName();
        JsonNode node = objectMapper.readTree(rawMessage);
        String type = node.get("type").asText();
        switch(type) {
            case "DisconnectRequest" -> {
                Message<PokerDTO> msg = new Message<>(matchRoomService.handleDisconnect(gameId,username));
                messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
                break;
            }
            case "MoveDTO" -> {
                MoveDTO content = objectMapper.treeToValue(node.get("content"), MoveDTO.class);
                Message<PokerDTO> msg = new Message<>(matchRoomService.processMove(gameId,username,content));
                messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
                break;
            }
            case "HandQuery" -> {
                HandDTO hand = matchRoomService.getPlayerHand(gameId,username);
                Message<HandDTO> msg = new Message<>(hand);
                messagingTemplate.convertAndSendToUser(username,"/queue/private",msg);
                break;
            }
            case "StateQuery" -> {
                PokerDTO state = matchRoomService.getState(gameId);
                HandDTO hand = matchRoomService.getPlayerHand(gameId, username);
                if(state == null) {
                    System.out.println("STATE IS NULL WHY THE FUCK IS IT NULL");
                    return;
                }

                Message<PokerDTO> msg = new Message<>(state);
                messagingTemplate.convertAndSendToUser(username,"/queue/private",msg);
                break;
            }
            default -> {
                System.out.println("Unexpected message passed to the game room of type: " + type);
                break;
            }
        }
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
