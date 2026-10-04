package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.WorkspaceMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkspaceMemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspaceId", source = "workspaceId")
    @Mapping(target = "userId", source = "member.user.id")
    @Mapping(target = "roleKey", source = "member.user.roleKey")
    @Mapping(target = "dateJoined", source = "member.user.dateJoined")
    @Mapping(target = "dateInvited", source = "member.user.dateInvited")
    @Mapping(target = "invitedByUserId", source = "member.invitedBy.id")
    WorkspaceMemberEntity toEntity(ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto member, String workspaceId);
}
