package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ClickUpSpaceListResponseDto(
        List<SpaceDto> spaces
) {
    public record SpaceDto(
            String id,
            String name,

            @JsonProperty("private") // Java'da rezerve kelime olduğu için özel olarak belirtiyoruz
            Boolean privateSpace,

            Boolean multipleAssignees, // multiple_assignees -> multipleAssignees (Otomatik eşleşir)
            Boolean archived           // archived -> archived
    ) {}
}