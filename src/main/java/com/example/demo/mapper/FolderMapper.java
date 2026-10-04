package com.example.demo.mapper;

import com.example.demo.dto.ClickUpFolderDto;
import com.example.demo.entity.FolderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FolderMapper {

    @Mapping(target = "spaceId", source = "spaceId")
    FolderEntity toEntity(ClickUpFolderDto dto, String spaceId);
}
