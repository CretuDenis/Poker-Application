package com.example.Poker.game;

import java.util.Optional;

public record PokerMessage(String playerName, String action, Optional<Integer> amount) {}

