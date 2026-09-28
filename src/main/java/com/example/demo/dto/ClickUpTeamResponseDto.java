package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClickUpTeamResponseDto(
        List<TeamDto> teams //not necesssary
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TeamDto(
            String id,
            String name
    ) {}
}