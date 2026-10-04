package com.example.demo.mapper;

import com.example.demo.dto.ClickUpListDto;
import com.example.demo.entity.ListEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ListMapper {

    @Mapping(target = "spaceId", source = "spaceId")
    @Mapping(target = "folderId", source = "folderId")
    ListEntity toEntity(ClickUpListDto dto, String spaceId, String folderId);
}
