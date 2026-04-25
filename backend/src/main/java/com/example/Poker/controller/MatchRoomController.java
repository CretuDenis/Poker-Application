package com.example.Poker.controller;

import com.example.Poker.service.MatchRoomService;
import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.game.Poker;
import com.example.Poker.dto.GameStateDTO;
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
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;

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
    public void handleMessage(@DestinationVariable String gameId,Principal principal,@Payload String rawMessage) throws Exception {
        if(principal == null) {
            System.out.println("Game room cannot handle unauthentificated users");
            return;
        }
        String username = principal.getName();
        JsonNode node = objectMapper.readTree(rawMessage);
        String type = node.get("type").asText();
        switch(type) {
            case "DisconnectRequest" -> {
                Poker currState = matchRoomService.handleDisconnect(gameId,username);

                if (currState == null) return;
                Poker prevState = matchRoomService.getPrevState(gameId);
                List<String> playerNames = currState.getPlayerNames();

                for(String name : playerNames) {
                    Message<GameStateDTO> msg = new Message<>(
                            new GameStateDTO(
                                prevState == null ? null : prevState.toDto(name),
                                currState.toDto(name)));
                    messagingTemplate.convertAndSendToUser(name,"/queue/private",msg);
                }
                break;
            }
            case "MoveDTO" -> {
                MoveDTO move = objectMapper.treeToValue(node.get("content"), MoveDTO.class);
                Poker currState = matchRoomService.processMove(gameId,username,move);
                if (currState == null) return;
                
                if (currState.shouldFinishHand()) {
                    System.out.println("Cutscene will be played");
                    matchRoomService.finishHand(gameId, currState); 
                    return;
                }

                Poker prevState = matchRoomService.getPrevState(gameId);
                List<String> playerNames = currState.getPlayerNames();

                for(String name : playerNames) {
                    Message<GameStateDTO> msg = new Message<>(
                            new GameStateDTO(
                                prevState == null ? null : prevState.toDto(name),
                                currState.toDto(name)));
                    messagingTemplate.convertAndSendToUser(name,"/queue/private",msg);
                }
                break;
            }
            case "StateQuery" -> {
                Poker curr = matchRoomService.getState(gameId);
                Poker prev = matchRoomService.getPrevState(gameId);
                assert curr != null;

                PokerDTO prevDto = prev == null ? null : prev.toDto(username);
                PokerDTO currDto = curr.toDto(username);

                Message<GameStateDTO> msg = new Message<>(new GameStateDTO(prevDto,currDto));
                messagingTemplate.convertAndSendToUser(username,"/queue/private",msg);
                break;
            }
            default -> {
                System.out.println("Unexpected message passed to the game room of type: " + type);
                break;
            }
        }
    }
}
