package com.example.demo.dto;

public record ClickUpAuthorizedUserResponseDto(
        ClickUpTeamResponseDto.ClickUpUserDto user
) {}