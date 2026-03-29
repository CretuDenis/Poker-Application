package com.example.Poker.dto;

public class AuthDto {
    public record RegisterRequest(String username,String password) {}

    public record LoginRequest(String username,String password) {}

    public record AuthResponse(String token,String username) {}
}
