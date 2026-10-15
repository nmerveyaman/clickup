package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "statusName", source = "dto.status.status")
    @Mapping(target = "statusType", source = "dto.status.type")
    @Mapping(target = "orderIndex", source = "dto.orderIndex")
    @Mapping(target = "creatorId", source = "dto.creator.id")
    @Mapping(target = "listId", source = "dto.list.id")
    @Mapping(target = "priority", source = "dto.priority.priority")
    @Mapping(target = "assignees", source = "dto.assignees")
    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "blockedByTaskId", expression = "java(extractBlockedByTaskIds(dto))")
    TaskEntity toEntity(ClickUpTaskDto dto);

    default TaskEntity toEntity(ClickUpTaskDto dto, String workspaceId) {
        if (dto == null) {
            return null;
        }
        TaskEntity entity = toEntity(dto);
        entity.setWorkspaceId(workspaceId);
        return entity;
    }

    default UserEntity assigneeToUserEntity(ClickUpTaskDto.AssigneeDto assigneeDto) {
        if (assigneeDto == null) {
            return null;
        }
        UserEntity user = new UserEntity();
        user.setId(assigneeDto.id());
        return user;
    }

    default String extractBlockedByTaskIds(ClickUpTaskDto dto) {
        if (dto == null || dto.dependencies() == null || dto.dependencies().isEmpty()) {
            return null;
        }
        return dto.dependencies().stream()
                .map(ClickUpTaskDto.DependencyDto::dependsOn)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}