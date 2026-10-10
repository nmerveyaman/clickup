package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.entity.TaskEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapperImpl();

    @Test
    void toEntity_mapsAllNestedFields() {
        var dto = new ClickUpTaskDto(
                "task1", "My Task", "1.5",
                "1000", "2000", null, null,
                new ClickUpTaskDto.StatusDto("in progress", "open"),
                new ClickUpTaskDto.CreatorDto("creator1"),
                new ClickUpTaskDto.ListObjDto("list1"),
                List.of(new ClickUpTaskDto.AssigneeDto(10L), new ClickUpTaskDto.AssigneeDto(20L))
        );

        TaskEntity entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isEqualTo("task1");
        assertThat(entity.getName()).isEqualTo("My Task");
        assertThat(entity.getOrderIndex()).isEqualTo("1.5");
        assertThat(entity.getDateCreated()).isEqualTo("1000");
        assertThat(entity.getDateUpdated()).isEqualTo("2000");
        assertThat(entity.getStatusName()).isEqualTo("in progress");
        assertThat(entity.getStatusType()).isEqualTo("open");
        assertThat(entity.getCreatorId()).isEqualTo("creator1");
        assertThat(entity.getListId()).isEqualTo("list1");
        assertThat(entity.getAssignees()).hasSize(2);
        assertThat(entity.getAssignees().get(0).getId()).isEqualTo(10L);
        assertThat(entity.getAssignees().get(1).getId()).isEqualTo(20L);
    }

    @Test
    void toEntity_withNullNestedObjects_setsFieldsNull() {
        var dto = new ClickUpTaskDto(
                "task2", "Simple Task", null,
                null, null, null, null,
                null, null, null, null
        );

        TaskEntity entity = mapper.toEntity(dto);

        assertThat(entity.getStatusName()).isNull();
        assertThat(entity.getStatusType()).isNull();
        assertThat(entity.getCreatorId()).isNull();
        assertThat(entity.getListId()).isNull();
        assertThat(entity.getAssignees()).isNull();
    }

    @Test
    void assigneeToUserEntity_withNullDto_returnsNull() {
        assertThat(mapper.assigneeToUserEntity(null)).isNull();
    }

    @Test
    void assigneeToUserEntity_setsOnlyId() {
        var assignee = new ClickUpTaskDto.AssigneeDto(99L);

        var user = mapper.assigneeToUserEntity(assignee);

        assertThat(user.getId()).isEqualTo(99L);
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
    }
}
