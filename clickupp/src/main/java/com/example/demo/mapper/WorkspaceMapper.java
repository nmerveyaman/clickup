package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.WorkspaceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    WorkspaceEntity toEntity(ClickUpTeamResponseDto.ClickUpWorkspaceDto dto);
}
