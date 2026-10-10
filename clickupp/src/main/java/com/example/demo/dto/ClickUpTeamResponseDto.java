package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ClickUpTeamResponseDto(
        @JsonProperty("teams")
        List<ClickUpWorkspaceDto> workspaces
) {

    public record ClickUpWorkspaceDto(
            String id,
            String name,
            List<ClickUpWorkspaceMemberDto> members
    ) {}

    public record ClickUpWorkspaceMemberDto(
            ClickUpUserDto user,

            @JsonProperty("invited_by") ClickUpInvitedByDto invitedBy
    ) {}

    public record ClickUpUserDto(
            Long id,
            String username,
            String email,
            @JsonProperty("profile_picture") String profilePicture,
            @JsonProperty("date_joined") String dateJoined,
            @JsonProperty("date_invited") String dateInvited,
            @JsonProperty("role_key") String roleKey
    ) {}

    public record ClickUpInvitedByDto(
            Long id,
            String username,
            String email
    ) {}
}