package com.example.demo.mapper;

import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void toEntity_mapsAllFields() {
        var dto = new ClickUpTeamResponseDto.ClickUpUserDto(
                1L, "john", "john@example.com", "pic.jpg",
                "2024-01-01", "2024-01-02", "admin"
        );

        UserEntity entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUsername()).isEqualTo("john");
        assertThat(entity.getEmail()).isEqualTo("john@example.com");
        assertThat(entity.getProfilePicture()).isEqualTo("pic.jpg");
    }

    @Test
    void toEntity_withNullDto_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
