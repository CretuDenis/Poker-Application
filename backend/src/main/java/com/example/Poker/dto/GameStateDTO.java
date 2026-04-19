package com.example.Poker.dto;

import com.example.Poker.dto.PokerDTO;

import java.util.List;

public record GameStateDTO(PokerDTO previous, PokerDTO current) {}
