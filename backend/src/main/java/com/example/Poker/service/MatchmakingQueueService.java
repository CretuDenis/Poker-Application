package com.example.Poker.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import com.example.Poker.game.Poker;
import com.example.Poker.service.MatchRoomService;


@Service
public class MatchmakingQueueService {
    private final BlockingQueue<String> matchmakingQueue;
    private final MatchRoomService matchService;

    public MatchmakingQueueService(MatchRoomService matchService) {
        this.matchmakingQueue = new LinkedBlockingQueue<>();
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
