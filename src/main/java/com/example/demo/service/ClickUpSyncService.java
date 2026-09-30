package com.example.demo.service;
import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.*;

import com.example.demo.entity.SpaceEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkspaceEntity;
import com.example.demo.entity.FolderEntity;
import com.example.demo.entity.ListEntity;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.WorkspaceMemberEntity;

import com.example.demo.repository.SpaceRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkspaceMemberRepository;
import com.example.demo.repository.WorkspaceRepository;
import com.example.demo.repository.FolderRepository;
import com.example.demo.repository.ListRepository;
import com.example.demo.repository.TaskRepository;

import java.util.List;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClickUpSyncService {

    private final ClickUpClient clickUpClient;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SpaceRepository spaceRepository;
    private final FolderRepository folderRepository;
    private final ListRepository listRepository;
    private final TaskRepository taskRepository;
    @Transactional
    public void syncClickUpData() {
        System.out.println("Starting ClickUp data synchronization...");
        ClickUpTeamResponseDto teamResponse = clickUpClient.getTeams();

        if (teamResponse == null || teamResponse.workspaces() == null) {
            System.out.println("Team response or workspaces list is null!");
            return;
        }

        System.out.println("ClickUp Workspaces Count: " + teamResponse.workspaces().size());

        for (ClickUpTeamResponseDto.ClickUpWorkspaceDto workspaceDto : teamResponse.workspaces()) {

            // 1. Workspace Kaydı
            WorkspaceEntity workspace = new WorkspaceEntity();
            workspace.setId(workspaceDto.id());
            workspace.setName(workspaceDto.name());
            workspaceRepository.save(workspace);
            System.out.println("Saved Workspace: " + workspace.getName());

            // 2. Workspace Üyeleri ve Kullanıcılar
            if (workspaceDto.members() != null) {
                for (ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto workspaceMemberDto : workspaceDto.members()) {

                    if (workspaceMemberDto.user() == null) {
                        System.out.println("Skipping workspace member without user information.");
                        continue;
                    }

                    ClickUpTeamResponseDto.ClickUpUserDto userDto = workspaceMemberDto.user();
                    UserEntity user = new UserEntity();
                    user.setId(userDto.id());
                    user.setUsername(userDto.username());
                    user.setEmail(userDto.email());
                    user.setProfilePicture(userDto.profilePicture());
                    userRepository.save(user);

                    WorkspaceMemberEntity workspaceMember = new WorkspaceMemberEntity();
                    workspaceMember.setWorkspaceId(workspaceDto.id());
                    workspaceMember.setUserId(userDto.id());
                    workspaceMember.setRoleKey(userDto.roleKey());
                    workspaceMember.setDateJoined(userDto.dateJoined());
                    workspaceMember.setDateInvited(userDto.dateInvited());

                    if (workspaceMemberDto.invitedBy() != null) {
                        ClickUpTeamResponseDto.ClickUpInvitedByDto inviter = workspaceMemberDto.invitedBy();
                        workspaceMember.setInvitedByUserId(inviter.id());
                    }

                    workspaceMemberRepository.save(workspaceMember);
                }
            }

            // 3. Space Kaydı
            ClickUpSpaceListResponseDto spaceResponse = clickUpClient.getSpaces(workspaceDto.id());
            if (spaceResponse != null && spaceResponse.spaces() != null) {
                for (ClickUpSpaceListResponseDto.SpaceDto spaceDto : spaceResponse.spaces()) {
                    SpaceEntity space = new SpaceEntity();
                    space.setId(spaceDto.id());
                    space.setName(spaceDto.name());
                    space.setWorkspace(workspace);

                    spaceRepository.save(space);
                    System.out.println("Saved Space: " + space.getName());
                    syncSpaceContents(spaceDto.id());
                }
            }
            syncTasks(workspaceDto.id());
        }
            //4.task


    }

    private void syncSpaceContents(String spaceId) {
        ClickUpListResponseDto spaceLists = clickUpClient.getListsBySpace(spaceId);
        if (spaceLists != null && spaceLists.lists() != null) {
            for (ClickUpListDto listDto : spaceLists.lists()) {
                saveListEntity(listDto, spaceId, null);
            }
        }

        ClickUpFolderListResponseDto folderResponse = clickUpClient.getFolders(spaceId);
        if (folderResponse != null && folderResponse.folders() != null) {
            for (ClickUpFolderDto folderDto : folderResponse.folders()) {
                FolderEntity folderEntity = new FolderEntity();
                folderEntity.setId(folderDto.id());
                folderEntity.setName(folderDto.name());
                folderEntity.setSpaceId(spaceId);
                folderEntity.setHidden(folderDto.hidden());
                folderRepository.save(folderEntity);

                if (folderDto.lists() != null) {
                    for (ClickUpListDto listDto : folderDto.lists()) {
                        saveListEntity(listDto, spaceId, folderDto.id());
                    }
                }
            }
        }
    }

    private void saveListEntity(ClickUpListDto listDto, String spaceId, String folderId) {
        ListEntity listEntity = new ListEntity();
        listEntity.setId(listDto.id());
        listEntity.setName(listDto.name());
        listEntity.setSpaceId(spaceId);
        listEntity.setFolderId(folderId);
        listEntity.setTaskCount(listDto.taskCount());
        listRepository.save(listEntity);
    }

    private void syncTasks(String teamId) {
        ClickUpTaskResponseDto taskResponse = clickUpClient.getTasks(teamId);
        if (taskResponse != null && taskResponse.tasks() != null) {
            for (ClickUpTaskDto taskDto : taskResponse.tasks()) {
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setId(taskDto.id());
                taskEntity.setName(taskDto.name());
                taskEntity.setOrderIndex(taskDto.orderIndex());
                taskEntity.setDateCreated(taskDto.dateCreated());
                taskEntity.setDateUpdated(taskDto.dateUpdated());
                taskEntity.setDateClosed(taskDto.dateClosed());
                taskEntity.setDateDone(taskDto.dateDone());

                if(taskDto.status() != null) {
                    taskEntity.setStatusName(taskDto.status().status());
                    taskEntity.setStatusType(taskDto.status().type());
                }

                if (taskDto.creator() != null) {
                    taskEntity.setCreatorId(taskDto.creator().id());
                }
                if (taskDto.list() != null) {
                    taskEntity.setListId(taskDto.list().id());
                }
                if (taskDto.assignees() != null && !taskDto.assignees().isEmpty()) {
                    List<UserEntity> taskAssignees = new ArrayList<>();

                    for (ClickUpTaskDto.AssigneeDto assigneeDto : taskDto.assignees()) {
                        UserEntity userRef = new UserEntity();
                        userRef.setId(assigneeDto.id());
                        taskAssignees.add(userRef);
                        }
                    taskEntity.setAssignees(taskAssignees);
                    }

                taskRepository.save(taskEntity);
                }

                }

    }
}