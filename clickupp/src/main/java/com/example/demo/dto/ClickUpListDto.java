package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;



public record ClickUpListDto(
        String id,
        String name,
        @JsonProperty("task_count") Integer taskCount,
        @JsonProperty("start_date") Long startDate,
        @JsonProperty("due_date") Long dueDate


) {

}
