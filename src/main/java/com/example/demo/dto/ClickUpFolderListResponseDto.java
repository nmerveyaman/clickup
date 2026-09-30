package com.example.demo.dto;
import java.util.List;

public record ClickUpFolderListResponseDto (
    List<ClickUpFolderDto> folders
) {
}
