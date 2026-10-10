package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final UserMapper userMapper;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final SpaceMapper spaceMapper;
    private final FolderMapper folderMapper;
    private final ListMapper listMapper;
    private final TaskMapper taskMapper;
    private final TaskEventDetectorService taskEventDetectorService;

    // 1. ZAMANLANMIŞ GÖREV: DB'de token'ı olan tüm kullanıcıların Workspace'lerini senkronize eder
    @Scheduled(fixedRate = 6000)
    @Transactional
    public void syncAllConnectedWorkspaces() {
        List<UserEntity> connectedUsers = userRepository.findByAccessTokenIsNotNull();
        if (connectedUsers.isEmpty()) {
            System.out.println("Henüz OAuth ile giriş yapmış kullanıcı yok, senkronizasyon bekleniyor...");
            return;
        }

        Set<String> syncedWorkspaceIds = new HashSet<>();
        for (UserEntity user : connectedUsers) {
            syncUserWorkspaces(user.getAccessToken(), syncedWorkspaceIds);
        }
    }

    // 2. ANLIK SENKRONİZASYON: Kullanıcı OAuth ile login olduğu anda çağrılır
    @Transactional
    public void syncForSingleUser(String accessToken) {
        syncUserWorkspaces(accessToken, new HashSet<>());
    }

    private void syncUserWorkspaces(String accessToken, Set<String> syncedWorkspaceIds) {
        ClickUpTeamResponseDto teamResponse = clickUpClient.getTeams(accessToken);

        if (teamResponse == null || teamResponse.workspaces() == null) {
            return;
        }

        for (ClickUpTeamResponseDto.ClickUpWorkspaceDto workspaceDto : teamResponse.workspaces()) {
            if (!syncedWorkspaceIds.add(workspaceDto.id())) {
                continue;
            }

            System.out.println("Senkronize edilen Workspace: " + workspaceDto.name());

            // 1. WORKSPACE
            WorkspaceEntity workspace = workspaceRepository.findById(workspaceDto.id())
                    .map(existing -> {
                        existing.setName(workspaceDto.name());
                        return workspaceRepository.save(existing);
                    })
                    .orElseGet(() -> workspaceRepository.save(workspaceMapper.toEntity(workspaceDto)));

            // 2. USERS & MEMBERS
            if (workspaceDto.members() != null) {
                for (ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto memberDto : workspaceDto.members()) {
                    if (memberDto.user() == null) continue;

                    var mappedUser = userMapper.toEntity(memberDto.user());
                    userRepository.findById(mappedUser.getId())
                            .map(existingUser -> {
                                existingUser.setUsername(mappedUser.getUsername());
                                existingUser.setEmail(mappedUser.getEmail());
                                existingUser.setProfilePicture(mappedUser.getProfilePicture());
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> userRepository.save(mappedUser));

                    var memberEntity = workspaceMemberMapper.toEntity(memberDto, workspaceDto.id());
                    workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceDto.id(), mappedUser.getId())
                            .ifPresentOrElse(
                                    existingMember -> {
                                        existingMember.setRoleKey(memberEntity.getRoleKey());
                                        existingMember.setDateJoined(memberEntity.getDateJoined());
                                        workspaceMemberRepository.save(existingMember);
                                    },
                                    () -> workspaceMemberRepository.save(memberEntity)
                            );
                }
            }

            // 3. SPACES
            ClickUpSpaceListResponseDto spaceResponse = clickUpClient.getSpaces(workspaceDto.id(), accessToken);
            if (spaceResponse != null && spaceResponse.spaces() != null) {
                for (ClickUpSpaceListResponseDto.SpaceDto spaceDto : spaceResponse.spaces()) {
                    spaceRepository.findById(spaceDto.id())
                            .map(existingSpace -> {
                                existingSpace.setName(spaceDto.name());
                                existingSpace.setIsPrivate(spaceDto.privateSpace());
                                existingSpace.setIsArchived(spaceDto.archived());
                                return spaceRepository.save(existingSpace);
                            })
                            .orElseGet(() -> spaceRepository.save(spaceMapper.toEntity(spaceDto, workspace)));

                    syncSpaceContents(spaceDto.id(), accessToken);
                }
            }

            // 4. TASKS
            syncTasks(workspaceDto.id(), workspaceDto.id(), accessToken);
        }
    }

    private void syncSpaceContents(String spaceId, String accessToken) {
        ClickUpListResponseDto spaceLists = clickUpClient.getListsBySpace(spaceId, accessToken);
        if (spaceLists != null && spaceLists.lists() != null) {
            for (ClickUpListDto listDto : spaceLists.lists()) {
                var listEntity = listMapper.toEntity(listDto, spaceId, null);
                listRepository.findById(listDto.id())
                        .map(existing -> {
                            existing.setName(listDto.name());
                            existing.setTaskCount(listDto.taskCount());
                            existing.setStartDate(listDto.startDate());
                            existing.setDueDate(listDto.dueDate());
                            return listRepository.save(existing);
                        })
                        .orElseGet(() -> listRepository.save(listEntity));
            }
        }

        ClickUpFolderListResponseDto folderResponse = clickUpClient.getFolders(spaceId, accessToken);
        if (folderResponse != null && folderResponse.folders() != null) {
            for (ClickUpFolderDto folderDto : folderResponse.folders()) {
                var folderEntity = folderMapper.toEntity(folderDto, spaceId);
                folderRepository.findById(folderDto.id())
                        .map(existing -> {
                            existing.setName(folderDto.name());
                            return folderRepository.save(existing);
                        })
                        .orElseGet(() -> folderRepository.save(folderEntity));

                if (folderDto.lists() != null) {
                    for (ClickUpListDto listDto : folderDto.lists()) {
                        var listEntity = listMapper.toEntity(listDto, spaceId, folderDto.id());
                        listRepository.findById(listDto.id())
                                .map(existing -> {
                                    existing.setName(listDto.name());
                                    return listRepository.save(existing);
                                })
                                .orElseGet(() -> listRepository.save(listEntity));
                    }
                }
            }
        }
    }

    private void syncTasks(String teamId, String workspaceId, String accessToken) {
        ClickUpTaskResponseDto taskResponse = clickUpClient.getTasks(teamId, accessToken);
        if (taskResponse == null || taskResponse.tasks() == null) {
            return;
        }

        for (ClickUpTaskDto taskDto : taskResponse.tasks()) {
            var mappedTask = taskMapper.toEntity(taskDto, workspaceId);

            taskRepository.findById(taskDto.id()).ifPresentOrElse(
                    existingTask -> {
                        taskEventDetectorService.detectAndRecordEvents(existingTask, taskDto);

                        existingTask.setName(taskDto.name());
                        existingTask.setWorkspaceId(workspaceId);

                        if (taskDto.status() != null) {
                            existingTask.setStatusName(taskDto.status().status());
                            existingTask.setStatusType(taskDto.status().type());
                            if (taskDto.status().orderIndex() != null) {
                                existingTask.setOrderIndex(String.valueOf(taskDto.status().orderIndex()));
                            }
                        }

                        // TARİH ALANLARININ GÜNCELLENMESİ:
                        existingTask.setDateCreated(taskDto.dateCreated());
                        existingTask.setDateUpdated(taskDto.dateUpdated());
                        existingTask.setDateClosed(taskDto.dateClosed());
                        existingTask.setDateDone(taskDto.dateDone());

                        if (mappedTask.getAssignees() != null) {
                            existingTask.setAssignees(new ArrayList<>(mappedTask.getAssignees()));
                        } else {
                            existingTask.setAssignees(new ArrayList<>());
                        }

                        taskRepository.save(existingTask);
                    },
                    () -> taskRepository.save(mappedTask)
            );
        }
    }}