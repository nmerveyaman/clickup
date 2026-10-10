package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "statusName", source = "dto.status.status")
    @Mapping(target = "statusType", source = "dto.status.type")
    @Mapping(target = "orderIndex", source = "dto.status.orderIndex")
    @Mapping(target = "creatorId", source = "dto.creator.id")
    @Mapping(target = "listId", source = "dto.list.id")
    @Mapping(target = "priority", source = "dto.priority.priority")
    @Mapping(target = "workspaceId", source = "workspaceId") // Dışarıdan gelen workspaceId buraya bağlanıyor
    @Mapping(target = "blockedByTaskId", expression = "java(extractBlockedByTaskIds(dto))")
    TaskEntity toEntity(ClickUpTaskDto dto, String workspaceId);

    default String extractBlockedByTaskIds(ClickUpTaskDto dto) {
        return null;
    }
}