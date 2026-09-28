package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickUpSpaceListResponseDto(
        List<ClickUpSpaceDto> spaces
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClickUpSpaceDto(
            String id,
            String name,
            String color,
            @JsonProperty("private") Boolean isPrivate,
            @JsonProperty("archived") Boolean isArchived
    ) {}
}