package com.example.Poker.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.example.Poker.game.Poker;
import com.example.Poker.service.MatchRoomService;


@Service
public class MatchmakingQueueService {
    private final BlockingQueue<String> matchmakingQueue;
    private final ConcurrentHashMap<String,List<String>> privateLobbies;
    private final MatchRoomService matchService;

    public MatchmakingQueueService(MatchRoomService matchService) {
        this.matchmakingQueue = new LinkedBlockingQueue<>();
        this.privateLobbies = new ConcurrentHashMap<>();
        this.matchService = matchService;
    }
    
    public synchronized void processJoinRequest(String username) {
        this.enqueue(username);

        if(this.size() >= Poker.numPlayers) {
            List<String> players = new ArrayList<>();
            for (int i = 0; i < Poker.numPlayers; i++) {
                try {
                    players.add(this.dequeue());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            matchService.createGame(players);
        }
    }

    public synchronized String processCreatePrivateLobby(String username) {
        for (Map.Entry<String,List<String>> entry : privateLobbies.entrySet()) {
            for(String playerName : entry.getValue()) {
                if (playerName.equals(username)) return null;
            }
        }

        String lobbyId = matchService.generateRandomString(20);
        List<String> players = privateLobbies.get(lobbyId);

        while (players != null) {
            lobbyId = matchService.generateRandomString(20);
            players = privateLobbies.get(lobbyId);
        }

        List<String> list = new ArrayList<>();
        list.add(username);
        privateLobbies.put(lobbyId,list);
        System.out.println("Created private lobby " + lobbyId);
        return lobbyId;
    }

    public synchronized void processJoinPrivateLobby(String lobbyId, String username) {
        List<String> players = privateLobbies.get(lobbyId);
        if (players == null) return;
        
        players.add(username);

        if (players.size() != 3) return;

        matchService.createGame(players);  
        privateLobbies.remove(lobbyId); 
    }

    public synchronized void processLeavePrivateLobby(String lobbyId, String username) {
        List<String> players = privateLobbies.get(lobbyId);
        if (players == null) return;
        
        players.removeIf(p -> p.equals(username));
        
        if (players.size() > 0) return;

        privateLobbies.remove(lobbyId); 
        System.out.println("Deleted private lobby " + lobbyId);
    }
    

    public synchronized void processLeaveRequest() {
        try {
            dequeue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void enqueue(String username)  {
        matchmakingQueue.offer(username);
    }

    public String dequeue() throws InterruptedException {
        return matchmakingQueue.take();
    }

    public int size() {
        return matchmakingQueue.size();
    }

    public void printSize() {
        System.out.println("Queue size: " + matchmakingQueue.size());
    }
}
