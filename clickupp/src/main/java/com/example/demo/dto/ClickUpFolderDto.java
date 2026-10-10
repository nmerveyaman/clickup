package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClickUpFolderDto(
        String id,
        String name,
        @JsonProperty("hidden") Boolean hidden,
        List<ClickUpListDto> lists
)

{
}
