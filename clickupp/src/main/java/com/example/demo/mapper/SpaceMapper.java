package com.example.demo.mapper;

import com.example.demo.dto.ClickUpSpaceListResponseDto;
import com.example.demo.entity.SpaceEntity;
import com.example.demo.entity.WorkspaceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpaceMapper {

    @Mapping(target = "id", source = "space.id")
    @Mapping(target = "name", source = "space.name")
    @Mapping(target = "isPrivate", source = "space.privateSpace")
    @Mapping(target = "isArchived", source = "space.archived")
    @Mapping(target = "workspace", source = "workspace")
    SpaceEntity toEntity(ClickUpSpaceListResponseDto.SpaceDto space, WorkspaceEntity workspace);
}