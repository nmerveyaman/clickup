package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickUpSyncServiceTest {

    @Mock private ClickUpClient clickUpClient;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private ListRepository listRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserMapper userMapper;
    @Mock private WorkspaceMapper workspaceMapper;
    @Mock private WorkspaceMemberMapper workspaceMemberMapper;
    @Mock private SpaceMapper spaceMapper;
    @Mock private FolderMapper folderMapper;
    @Mock private ListMapper listMapper;
    @Mock private TaskMapper taskMapper;

    @InjectMocks
    private ClickUpSyncService syncService;

    @Test
    void syncClickUpData_whenResponseNull_doesNothing() {
        when(clickUpClient.getTeams()).thenReturn(null);

        syncService.syncClickUpData();

        verifyNoInteractions(workspaceRepository, userRepository);
    }

    @Test
    void syncClickUpData_savesWorkspaceAndMembers() {
        var userDto = new ClickUpTeamResponseDto.ClickUpUserDto(
                1L, "john", "john@test.com", "pic.jpg", "2024-01-01", "2024-01-01", "admin"
        );
        var memberDto = new ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto(userDto, null);
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws1", "WS", List.of(memberDto));
        var teamResponse = new ClickUpTeamResponseDto(List.of(workspaceDto));

        var workspaceEntity = new WorkspaceEntity("ws1", "WS");
        var userEntity = new UserEntity(1L, "john", "john@test.com", "pic.jpg");
        var memberEntity = new WorkspaceMemberEntity(null, "ws1", 1L, "admin", null, "2024-01-01", "2024-01-01");

        when(clickUpClient.getTeams()).thenReturn(teamResponse);
        when(workspaceMapper.toEntity(workspaceDto)).thenReturn(workspaceEntity);
        when(userMapper.toEntity(userDto)).thenReturn(userEntity);
        when(workspaceMemberMapper.toEntity(memberDto, "ws1")).thenReturn(memberEntity);
        when(clickUpClient.getSpaces("ws1")).thenReturn(new ClickUpSpaceListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws1")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        verify(workspaceRepository).save(workspaceEntity);
        verify(userRepository).save(userEntity);
        verify(workspaceMemberRepository).save(memberEntity);
    }

    @Test
    void syncClickUpData_skipsMemberWithNullUser() {
        var memberDto = new ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto(null, null);
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws1", "WS", List.of(memberDto));
        var teamResponse = new ClickUpTeamResponseDto(List.of(workspaceDto));

        when(clickUpClient.getTeams()).thenReturn(teamResponse);
        when(workspaceMapper.toEntity(workspaceDto)).thenReturn(new WorkspaceEntity("ws1", "WS"));
        when(clickUpClient.getSpaces("ws1")).thenReturn(new ClickUpSpaceListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws1")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        verifyNoInteractions(userRepository, workspaceMemberRepository);
    }

    @Test
    void syncClickUpData_savesFoldersAndLists() {
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws1", "WS", List.of());
        var teamResponse = new ClickUpTeamResponseDto(List.of(workspaceDto));

        var listDto = new ClickUpListDto("list1", "My List", 5);
        var folderDto = new ClickUpFolderDto("f1", "Folder", false, List.of(listDto));
        var folderResponse = new ClickUpFolderListResponseDto(List.of(folderDto));
        var spaceDto = new ClickUpSpaceListResponseDto.SpaceDto("sp1", "Space", false, true, false);
        var spaceResponse = new ClickUpSpaceListResponseDto(List.of(spaceDto));

        var workspaceEntity = new WorkspaceEntity("ws1", "WS");
        var spaceEntity = new SpaceEntity("sp1", workspaceEntity, "Space", false, false);
        var folderEntity = new FolderEntity("f1", "Folder", "sp1", false);
        var listEntity = new ListEntity("list1", "My List", "sp1", "f1", 5);

        when(clickUpClient.getTeams()).thenReturn(teamResponse);
        when(workspaceMapper.toEntity(workspaceDto)).thenReturn(workspaceEntity);
        when(clickUpClient.getSpaces("ws1")).thenReturn(spaceResponse);
        when(spaceMapper.toEntity(spaceDto, workspaceEntity)).thenReturn(spaceEntity);
        when(clickUpClient.getListsBySpace("sp1")).thenReturn(new ClickUpListResponseDto(List.of()));
        when(clickUpClient.getFolders("sp1")).thenReturn(folderResponse);
        when(folderMapper.toEntity(folderDto, "sp1")).thenReturn(folderEntity);
        when(listMapper.toEntity(listDto, "sp1", "f1")).thenReturn(listEntity);
        when(clickUpClient.getTasks("ws1")).thenReturn(new ClickUpTaskResponseDto(List.of()));

        syncService.syncClickUpData();

        verify(spaceRepository).save(spaceEntity);
        verify(folderRepository).save(folderEntity);
        verify(listRepository).save(listEntity);
    }

    @Test
    void syncClickUpData_savesTasks() {
        var workspaceDto = new ClickUpTeamResponseDto.ClickUpWorkspaceDto("ws1", "WS", List.of());
        var teamResponse = new ClickUpTeamResponseDto(List.of(workspaceDto));

        var taskDto = new ClickUpTaskDto("t1", "Task", "1", "100", "200", null, null,
                new ClickUpTaskDto.StatusDto("open", "open"),
                new ClickUpTaskDto.CreatorDto("c1"),
                new ClickUpTaskDto.ListObjDto("l1"),
                List.of());
        var taskResponse = new ClickUpTaskResponseDto(List.of(taskDto));
        var taskEntity = new TaskEntity("t1", "Task", "open", "open", "1", "100", "200", null, null, "c1", "l1", List.of());

        when(clickUpClient.getTeams()).thenReturn(teamResponse);
        when(workspaceMapper.toEntity(workspaceDto)).thenReturn(new WorkspaceEntity("ws1", "WS"));
        when(clickUpClient.getSpaces("ws1")).thenReturn(new ClickUpSpaceListResponseDto(List.of()));
        when(clickUpClient.getTasks("ws1")).thenReturn(taskResponse);
        when(taskMapper.toEntity(taskDto)).thenReturn(taskEntity);

        syncService.syncClickUpData();

        verify(taskRepository).save(taskEntity);
    }
}
