package com.example.demo;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.*;
import com.example.demo.repository.*;
import com.example.demo.service.ClickUpSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Transactional
class ClickUpSyncIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private ClickUpClient clickUpClient;

    @Autowired private ClickUpSyncService syncService;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private ListRepository listRepository;
    @Autowired private TaskRepository taskRepository;

    @BeforeEach
    void mockDefaults() {
        when(clickUpClient.getTeams()).thenReturn(null);
    }

    @Test
    void syncClickUpData_persistsWorkspaceAndUser() {
        var userDto = new ClickUpTeamResponseDto.ClickUpUserDto(
                1L, "john", "john@example.com", "pic.jpg",
                "2024-01-01", "2024-01-01", "admin"
        );
        var memberDto = new ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto(userDto, null);
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto(
                "ws1", "Test Workspace", List.of(memberDto)
        );
        when(clickUpClient.getTeams()).thenReturn(new ClickUpTeamResponseDto(List.of(workspaceDto)));
        when(clickUpClient.getSpaces("ws1")).thenReturn(new ClickUpSpaceListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws1")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        assertThat(workspaceRepository.findById("ws1"))
                .isPresent()
                .get()
                .satisfies(ws -> assertThat(ws.getName()).isEqualTo("Test Workspace"));

        assertThat(userRepository.findById(1L))
                .isPresent()
                .get()
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo("john");
                    assertThat(user.getEmail()).isEqualTo("john@example.com");
                    assertThat(user.getProfilePicture()).isEqualTo("pic.jpg");
                });

        assertThat(workspaceMemberRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.getWorkspaceId()).isEqualTo("ws1");
                    assertThat(m.getUserId()).isEqualTo(1L);
                    assertThat(m.getRoleKey()).isEqualTo("admin");
                    assertThat(m.getInvitedByUserId()).isNull();
                });
    }

    @Test
    void syncClickUpData_persistsSpacesWithWorkspaceReference() {
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws2", "WS2", List.of());
        var spaceDto = new ClickUpSpaceListResponseDto.SpaceDto("sp1", "Dev Space", true, false, false);

        when(clickUpClient.getTeams()).thenReturn(new ClickUpTeamResponseDto(List.of(workspaceDto)));
        when(clickUpClient.getSpaces("ws2")).thenReturn(new ClickUpSpaceListResponseDto(List.of(spaceDto)));
        when(clickUpClient.getListsBySpace("sp1")).thenReturn(new ClickUpListResponseDto(List.of()));
        when(clickUpClient.getFolders("sp1")).thenReturn(new ClickUpFolderListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws2")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        assertThat(spaceRepository.findById("sp1"))
                .isPresent()
                .get()
                .satisfies(space -> {
                    assertThat(space.getName()).isEqualTo("Dev Space");
                    assertThat(space.getIsPrivate()).isTrue();
                    assertThat(space.getWorkspace().getId()).isEqualTo("ws2");
                });
    }

    @Test
    void syncClickUpData_persistsFoldersAndLists() {
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws3", "WS3", List.of());
        var listInFolder = new ClickUpListDto("list1", "Sprint List", 10);
        var folderDto = new ClickUpFolderDto("f1", "Sprint Folder", false, List.of(listInFolder));
        var spaceDto = new ClickUpSpaceListResponseDto.SpaceDto("sp2", "Space 2", false, true, false);

        when(clickUpClient.getTeams()).thenReturn(new ClickUpTeamResponseDto(List.of(workspaceDto)));
        when(clickUpClient.getSpaces("ws3")).thenReturn(new ClickUpSpaceListResponseDto(List.of(spaceDto)));
        when(clickUpClient.getListsBySpace("sp2")).thenReturn(new ClickUpListResponseDto(List.of()));
        when(clickUpClient.getFolders("sp2")).thenReturn(new ClickUpFolderListResponseDto(List.of(folderDto)));
        when(clickUpClient.getTasks("ws3")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        assertThat(folderRepository.findById("f1"))
                .isPresent()
                .get()
                .satisfies(f -> {
                    assertThat(f.getName()).isEqualTo("Sprint Folder");
                    assertThat(f.getSpaceId()).isEqualTo("sp2");
                    assertThat(f.getHidden()).isFalse();
                });

        assertThat(listRepository.findById("list1"))
                .isPresent()
                .get()
                .satisfies(l -> {
                    assertThat(l.getName()).isEqualTo("Sprint List");
                    assertThat(l.getSpaceId()).isEqualTo("sp2");
                    assertThat(l.getFolderId()).isEqualTo("f1");
                    assertThat(l.getTaskCount()).isEqualTo(10);
                });
    }

    @Test
    void syncClickUpData_withNullTeamResponse_persistsNothing() {
        when(clickUpClient.getTeams()).thenReturn(null);

        syncService.syncClickUpData();

        assertThat(workspaceRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void syncClickUpData_persistsTaskWithStatus() {
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws4", "WS4", List.of());
        var taskDto = new ClickUpTaskDto(
                "task1", "Fix Bug", "1.0",
                "1700000000000", "1700000001000", null, null,
                new ClickUpTaskDto.StatusDto("in progress", "open"),
                new ClickUpTaskDto.CreatorDto("creator1"),
                new ClickUpTaskDto.ListObjDto("list-x"),
                List.of()
        );

        when(clickUpClient.getTeams()).thenReturn(new ClickUpTeamResponseDto(List.of(workspaceDto)));
        when(clickUpClient.getSpaces("ws4")).thenReturn(new ClickUpSpaceListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws4")).thenReturn(new ClickUpTaskResponseDto(List.of(taskDto)));

        syncService.syncClickUpData();

        assertThat(taskRepository.findById("task1"))
                .isPresent()
                .get()
                .satisfies(task -> {
                    assertThat(task.getName()).isEqualTo("Fix Bug");
                    assertThat(task.getStatusName()).isEqualTo("in progress");
                    assertThat(task.getStatusType()).isEqualTo("open");
                    assertThat(task.getCreatorId()).isEqualTo("creator1");
                    assertThat(task.getListId()).isEqualTo("list-x");
                });
    }
}
