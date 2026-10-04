package com.example.demo.mapper;

import com.example.demo.dto.ClickUpSpaceListResponseDto;
import com.example.demo.entity.SpaceEntity;
import com.example.demo.entity.WorkspaceEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceMapperTest {

    private final SpaceMapper mapper = new SpaceMapperImpl();

    @Test
    void toEntity_mapsRenamedFieldsAndWorkspace() {
        var spaceDto = new ClickUpSpaceListResponseDto.SpaceDto(
                "space1", "My Space", true, false, true
        );
        var workspace = new WorkspaceEntity("ws1", "Workspace");

        SpaceEntity entity = mapper.toEntity(spaceDto, workspace);

        assertThat(entity.getId()).isEqualTo("space1");
        assertThat(entity.getName()).isEqualTo("My Space");
        assertThat(entity.getIsPrivate()).isTrue();
        assertThat(entity.getIsArchived()).isTrue();
        assertThat(entity.getWorkspace()).isSameAs(workspace);
    }

    @Test
    void toEntity_withNullFlags_mapsNullValues() {
        var spaceDto = new ClickUpSpaceListResponseDto.SpaceDto(
                "space2", "Space", null, null, null
        );

        SpaceEntity entity = mapper.toEntity(spaceDto, null);

        assertThat(entity.getIsPrivate()).isNull();
        assertThat(entity.getIsArchived()).isNull();
        assertThat(entity.getWorkspace()).isNull();
    }
}
