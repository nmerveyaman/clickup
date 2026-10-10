package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.WorkspaceMemberEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceMemberMapperTest {

    private final WorkspaceMemberMapper mapper = new WorkspaceMemberMapperImpl();

    @Test
    void toEntity_mapsNestedUserFieldsAndWorkspaceId() {
        var user = new ClickUpTeamResponseDto.ClickUpUserDto(
                1L, "john", "john@example.com", "pic.jpg",
                "2024-01-01", "2024-01-02", "admin"
        );
        var invitedBy = new ClickUpTeamResponseDto.ClickUpInvitedByDto(2L, "admin", "admin@example.com");
        var member = new ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto(user, invitedBy);

        WorkspaceMemberEntity entity = mapper.toEntity(member, "ws1");

        assertThat(entity.getId()).isNull();
        assertThat(entity.getWorkspaceId()).isEqualTo("ws1");
        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getRoleKey()).isEqualTo("admin");
        assertThat(entity.getDateJoined()).isEqualTo("2024-01-01");
        assertThat(entity.getDateInvited()).isEqualTo("2024-01-02");
        assertThat(entity.getInvitedByUserId()).isEqualTo(2L);
    }

    @Test
    void toEntity_withNullInvitedBy_setsInvitedByUserIdNull() {
        var user = new ClickUpTeamResponseDto.ClickUpUserDto(
                1L, "john", "john@example.com", "pic.jpg", null, null, "member"
        );
        var member = new ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto(user, null);

        WorkspaceMemberEntity entity = mapper.toEntity(member, "ws1");

        assertThat(entity.getInvitedByUserId()).isNull();
        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getWorkspaceId()).isEqualTo("ws1");
    }
}
