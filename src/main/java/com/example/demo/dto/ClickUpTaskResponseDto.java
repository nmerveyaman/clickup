package com.example.demo.dto;
import java.util.List;

public record ClickUpTaskResponseDto(
        List<ClickUpTaskDto> tasks
) {
}
