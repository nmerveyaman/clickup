package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClickUpStatusDto(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("orderindex") Integer orderIndex,
        @JsonProperty("color") String color,
        @JsonProperty("type") String type
) {}