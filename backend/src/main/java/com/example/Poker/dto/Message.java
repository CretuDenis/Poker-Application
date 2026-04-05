package com.example.Poker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;



public record Message<T>(T content) {
    @JsonProperty("type")
    public String type() {
        return content.getClass().getSimpleName();
    }
}
