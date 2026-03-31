package com.example.Poker.dto;

import com.example.Poker.dto.PokerPlayerDTO;
import com.example.Poker.dto.CardDTO;

import java.util.List;

public record PokerDTO(List<PokerPlayerDTO> players,CardDTO[] communityCards,Integer buttonIndex) {}
