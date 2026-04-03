package com.example.Poker.dto;

public class AuthDto {
    public record RegisterRequest(String username,String password) {}

    public record LoginRequest(String username,String password) {}

    public record RefreshRequest(String refresh) {}

    public record AuthResponse(String access,String refresh,String username) {}
}
