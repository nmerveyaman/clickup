package com.example.demo.dto;

import java.util.List;

public record ClickUpListResponseDto(
    List<ClickUpListDto> lists
) {
}
