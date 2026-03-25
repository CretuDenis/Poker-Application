package com.example.Poker.dto;

public class GameDto {
    public record GameAction(String type,int amount) {}
    public record GameState(Long gameId,String currentPlayer,String phase,Integer pot,String lastAction) {}
    public record MatchmakingJoinRequest(Long userID) {}
}
