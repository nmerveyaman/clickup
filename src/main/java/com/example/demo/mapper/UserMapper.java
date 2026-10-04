package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserEntity toEntity(ClickUpTeamResponseDto.ClickUpUserDto dto);
}
