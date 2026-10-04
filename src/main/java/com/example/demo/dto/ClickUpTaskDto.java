package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ClickUpTaskDto(
        String id,
        String name,


        @JsonProperty("date_created") String dateCreated,
        @JsonProperty("date_updated") String dateUpdated,
        @JsonProperty("date_closed") String dateClosed,
        @JsonProperty("date_done") String dateDone,

        StatusDto status,
        CreatorDto creator,
        ListObjDto list,
        List<AssigneeDto> assignees,
        PriorityDto priority,
        List<DependencyDto> dependencies

) {
    public record StatusDto(String status,
                            String type,
                            @JsonProperty("orderindex") Integer orderIndex) {}
    public record CreatorDto(String id) {}
    public record ListObjDto(String id) {}
    public record AssigneeDto(Long id) {}
    public record PriorityDto(String priority) {}
    public record DependencyDto(
            @JsonProperty("task_id") String taskId,
            @JsonProperty("depends_on") String dependsOn
    ) {}
}
