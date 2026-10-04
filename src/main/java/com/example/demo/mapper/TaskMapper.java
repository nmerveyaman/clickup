package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "statusName", source = "status.status")
    @Mapping(target = "statusType", source = "status.type")
    @Mapping(target = "orderIndex", source = "status.orderIndex")
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "listId", source = "list.id")
    @Mapping(target = "priority", source = "priority.priority")
    @Mapping(target = "blockedByTaskId", expression = "java(extractBlockedByTaskIds(dto))")
    TaskEntity toEntity(ClickUpTaskDto dto);

    default UserEntity assigneeToUserEntity(ClickUpTaskDto.AssigneeDto dto) {
        if (dto == null) return null;
        UserEntity user = new UserEntity();
        user.setId(dto.id());
        return user;
    }
    default String extractBlockedByTaskIds(ClickUpTaskDto dto) {
        if (dto.dependencies() == null || dto.dependencies().isEmpty() || dto.id() == null) {
            return null;
        }
        List<String> blockingIds = dto.dependencies().stream()
                .filter(dep -> dto.id().equals(dep.taskId()) && dep.dependsOn() != null)
                .map(ClickUpTaskDto.DependencyDto::dependsOn)
                .toList();

        return blockingIds.isEmpty() ? null : String.join(",", blockingIds);

    }
}
