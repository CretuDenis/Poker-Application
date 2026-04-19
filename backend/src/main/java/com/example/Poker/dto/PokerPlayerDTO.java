package com.example.Poker.dto;

import com.example.Poker.dto.HandDTO;

public record PokerPlayerDTO(String name,Integer balance, Integer bet,HandDTO hand) {}
