package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClickUpOAuthTokenResponseDto(
        @JsonProperty("access_token")
        String accessToken
) {}