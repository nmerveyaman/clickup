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

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void syncClickUpData() {
        System.out.println("Starting full ClickUp data synchronization...");
        ClickUpTeamResponseDto teamResponse = clickUpClient.getTeams();

        if (teamResponse == null || teamResponse.workspaces() == null) {
            return;
        }

        for (ClickUpTeamResponseDto.ClickUpWorkspaceDto workspaceDto : teamResponse.workspaces()) {

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

                    var user = userMapper.toEntity(memberDto.user());
                    if (!userRepository.existsById(user.getId())) {
                        userRepository.save(user);
                    }

                    var memberEntity = workspaceMemberMapper.toEntity(memberDto, workspaceDto.id());
                    workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceDto.id(), user.getId())
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
            ClickUpSpaceListResponseDto spaceResponse = clickUpClient.getSpaces(workspaceDto.id());
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

                    syncSpaceContents(spaceDto.id());
                }
            }

            // 4. TASKS
            syncTasks(workspaceDto.id());
        }
    }

    private void syncSpaceContents(String spaceId) {
        ClickUpListResponseDto spaceLists = clickUpClient.getListsBySpace(spaceId);
        if (spaceLists != null && spaceLists.lists() != null) {
            for (ClickUpListDto listDto : spaceLists.lists()) {
                var listEntity = listMapper.toEntity(listDto, spaceId, null);
                listRepository.findById(listDto.id())
                        .map(existing -> {
                            existing.setName(listDto.name());
                            existing.setTaskCount(listDto.taskCount());
                            return listRepository.save(existing);
                        })
                        .orElseGet(() -> listRepository.save(listEntity));
            }
        }

        ClickUpFolderListResponseDto folderResponse = clickUpClient.getFolders(spaceId);
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

    private void syncTasks(String teamId) {
        ClickUpTaskResponseDto taskResponse = clickUpClient.getTasks(teamId);
        if (taskResponse == null || taskResponse.tasks() == null) {
            return;
        }

        for (ClickUpTaskDto taskDto : taskResponse.tasks()) {
            var mappedTask = taskMapper.toEntity(taskDto);

            taskRepository.findById(taskDto.id()).ifPresentOrElse(
                    existingTask -> {
                        taskEventDetectorService.detectAndRecordEvents(existingTask, taskDto);

                        existingTask.setName(taskDto.name());
                        if (taskDto.status() != null) {
                            existingTask.setStatusName(taskDto.status().status());
                            existingTask.setStatusType(taskDto.status().type());
                        }

                        if (mappedTask.getAssignees() != null) {
                            existingTask.setAssignees(new ArrayList<>(mappedTask.getAssignees()));
                        } else {
                            existingTask.setAssignees(new ArrayList<>());
                        }

                        taskRepository.save(existingTask);
                    },
                    () -> {
                        taskRepository.save(mappedTask);
                    }
            );
        }
    }
}