package com.example.demo.service;

import com.example.demo.client.ClickUpClient;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.event.TaskChangedEvent;
import com.example.demo.mapper.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClickUpSyncService {

    @Transactional
    public void syncClickUpData() {
        ClickUpTeamResponseDto teamResponse = clickUpClient.getTeams();
        if (teamResponse == null || teamResponse.workspaces() == null) {
            return;
        }

        for (ClickUpTeamResponseDto.ClickUpWorkspaceDto workspaceDto : teamResponse.workspaces()) {
            WorkspaceEntity workspace = workspaceRepository.findById(workspaceDto.id())
                    .map(existing -> {
                        existing.setName(workspaceDto.name());
                        return workspaceRepository.save(existing);
                    })
                    .orElseGet(() -> workspaceRepository.save(workspaceMapper.toEntity(workspaceDto)));

            if (workspaceDto.members() != null) {
                for (ClickUpTeamResponseDto.ClickUpWorkspaceMemberDto memberDto : workspaceDto.members()) {
                    if (memberDto.user() == null) {
                        continue;
                    }

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

                    ClickUpListResponseDto spaceLists = clickUpClient.getListsBySpace(spaceDto.id());
                    if (spaceLists != null && spaceLists.lists() != null) {
                        for (ClickUpListDto listDto : spaceLists.lists()) {
                            saveOrUpdateList(listDto, spaceDto.id(), null);
                        }
                    }

                    ClickUpFolderListResponseDto folderResponse = clickUpClient.getFolders(spaceDto.id());
                    if (folderResponse != null && folderResponse.folders() != null) {
                        for (ClickUpFolderDto folderDto : folderResponse.folders()) {
                            var folderEntity = folderRepository.findById(folderDto.id())
                                    .map(existing -> {
                                        existing.setName(folderDto.name());
                                        return folderRepository.save(existing);
                                    })
                                    .orElseGet(() -> folderRepository.save(folderMapper.toEntity(folderDto, spaceDto.id())));

                            if (folderDto.lists() != null) {
                                for (ClickUpListDto listDto : folderDto.lists()) {
                                    saveOrUpdateList(listDto, spaceDto.id(), folderDto.id());
                                }
                            }
                        }
                    }
                }
            }

            ClickUpTaskResponseDto taskResponse = clickUpClient.getTasks(workspaceDto.id());
            if (taskResponse == null || taskResponse.tasks() == null) {
                continue;
            }

            for (ClickUpTaskDto taskDto : taskResponse.tasks()) {
                var mappedTask = taskMapper.toEntity(taskDto);
                mappedTask.setWorkspaceId(workspaceDto.id());
                mappedTask.setPriority(extractPriority(taskDto));
                taskRepository.findById(taskDto.id()).ifPresentOrElse(
                        existingTask -> {
                            boolean changed = hasTaskChanged(existingTask, mappedTask);
                            String previousStatus = existingTask.getStatusName();
                            taskEventDetectorService.detectAndRecordEvents(existingTask, taskDto);
                            existingTask.setName(taskDto.name());
                            existingTask.setWorkspaceId(workspaceDto.id());
                            if (taskDto.status() != null) {
                                existingTask.setStatusName(taskDto.status().status());
                                existingTask.setStatusType(taskDto.status().type());
                                if (taskDto.status().orderIndex() != null) {
                                    existingTask.setOrderIndex(String.valueOf(taskDto.status().orderIndex()));
                                }
                            }
                            existingTask.setDateCreated(taskDto.dateCreated());
                            existingTask.setDateUpdated(taskDto.dateUpdated());
                            existingTask.setDateClosed(taskDto.dateClosed());
                            existingTask.setDateDone(taskDto.dateDone());
                            existingTask.setListId(mappedTask.getListId());
                            existingTask.setCreatorId(mappedTask.getCreatorId());
                            existingTask.setPriority(mappedTask.getPriority());
                            existingTask.setBlockedByTaskId(mappedTask.getBlockedByTaskId());
                            if (mappedTask.getAssignees() != null) {
                                existingTask.setAssignees(new ArrayList<>(mappedTask.getAssignees()));
                            } else {
                                existingTask.setAssignees(new ArrayList<>());
                            }
                            TaskEntity savedTask = taskRepository.save(existingTask);
                            if (changed) {
                                publishTaskUpdate(workspaceDto.id(), savedTask, previousStatus, "TASK_UPDATED");
                            }
                        },
                        () -> {
                            TaskEntity savedTask = taskRepository.save(mappedTask);
                            publishTaskUpdate(workspaceDto.id(), savedTask, null, "TASK_CREATED");
                        }
                );
            }
        }
    }

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
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // Space Altındaki Bağımsız Listeler
        ClickUpListResponseDto spaceLists = clickUpClient.getListsBySpace(spaceId, accessToken);
        if (spaceLists != null && spaceLists.lists() != null) {
            for (ClickUpListDto listDto : spaceLists.lists()) {
                saveOrUpdateList(listDto, spaceId, null, accessToken);
            }
        }

        // Folder Altındaki Listeler
        ClickUpFolderListResponseDto folderResponse = clickUpClient.getFolders(spaceId, accessToken);
        if (folderResponse != null && folderResponse.folders() != null) {
            for (ClickUpFolderDto folderDto : folderResponse.folders()) {

                var folderEntity = folderRepository.findById(folderDto.id())
                        .map(existing -> {
                            existing.setName(folderDto.name());
                            return folderRepository.save(existing);
                        })
                        .orElseGet(() -> folderRepository.save(folderMapper.toEntity(folderDto, spaceId)));

                if (folderDto.lists() != null) {
                    for (ClickUpListDto listDto : folderDto.lists()) {
                        saveOrUpdateList(listDto, spaceId, folderDto.id(), accessToken);
                    }
                }
            }
        }
    }

    /**
     * Hem Space hem Folder altındaki listeleri duplike oluşturmadan güvenle kaydeder/günceller.
     */
    private void saveOrUpdateList(ClickUpListDto listDto, String spaceId, String folderId) {
        ListEntity listEntity = listRepository.findById(listDto.id())
                .orElseGet(() -> listMapper.toEntity(listDto, spaceId, folderId));

        List<ClickUpStatusDto> statuses = listDto.statuses();
        if ((statuses == null || statuses.isEmpty())
                && (listEntity.getStatusesJson() == null || listEntity.getStatusesJson().isBlank())) {
            ClickUpListDto listDetails = clickUpClient.getList(listDto.id());
            if (listDetails != null && listDetails.statuses() != null && !listDetails.statuses().isEmpty()) {
                statuses = listDetails.statuses();
            }
        }

        listEntity.setName(listDto.name());
        listEntity.setTaskCount(listDto.taskCount());
        listEntity.setStartDate(listDto.startDate());
        listEntity.setDueDate(listDto.dueDate());
        if (statuses != null && !statuses.isEmpty()) {
            listEntity.setStatusesJson(convertStatusesToJson(statuses));
        }

        listRepository.save(listEntity);
    }

    private void saveOrUpdateList(ClickUpListDto listDto, String spaceId, String folderId, String accessToken) {
        ListEntity listEntity = listRepository.findById(listDto.id())
                .orElseGet(() -> listMapper.toEntity(listDto, spaceId, folderId));

        List<ClickUpStatusDto> statuses = listDto.statuses();
        if ((statuses == null || statuses.isEmpty())
                && (listEntity.getStatusesJson() == null || listEntity.getStatusesJson().isBlank())) {
            ClickUpListDto listDetails = clickUpClient.getList(listDto.id(), accessToken);
            if (listDetails != null && listDetails.statuses() != null && !listDetails.statuses().isEmpty()) {
                statuses = listDetails.statuses();
            }
        }

        listEntity.setName(listDto.name());
        listEntity.setTaskCount(listDto.taskCount());
        listEntity.setStartDate(listDto.startDate());
        listEntity.setDueDate(listDto.dueDate());
        if (statuses != null && !statuses.isEmpty()) {
            listEntity.setStatusesJson(convertStatusesToJson(statuses));
        }

        listRepository.save(listEntity);
    }

    private void syncTasks(String teamId, String workspaceId, String accessToken) {
        ClickUpTaskResponseDto taskResponse = clickUpClient.getTasks(teamId, accessToken);
        if (taskResponse == null || taskResponse.tasks() == null) {
            return;
        }

        for (ClickUpTaskDto taskDto : taskResponse.tasks()) {
            var mappedTask = taskMapper.toEntity(taskDto);
            mappedTask.setWorkspaceId(workspaceId);
            mappedTask.setPriority(extractPriority(taskDto));

            taskRepository.findById(taskDto.id()).ifPresentOrElse(
                    existingTask -> {
                        existingTask.setWorkspaceId(workspaceId);
                        boolean changed = hasTaskChanged(existingTask, mappedTask);
                        String previousStatus = existingTask.getStatusName();
                        taskEventDetectorService.detectAndRecordEvents(existingTask, taskDto);

                        existingTask.setName(taskDto.name());

                        if (taskDto.status() != null) {
                            existingTask.setStatusName(taskDto.status().status());
                            existingTask.setStatusType(taskDto.status().type());
                            if (taskDto.status().orderIndex() != null) {
                                existingTask.setOrderIndex(String.valueOf(taskDto.status().orderIndex()));
                            }
                        }

                        existingTask.setDateCreated(taskDto.dateCreated());
                        existingTask.setDateUpdated(taskDto.dateUpdated());
                        existingTask.setDateClosed(taskDto.dateClosed());
                        existingTask.setDateDone(taskDto.dateDone());
                        existingTask.setListId(mappedTask.getListId());
                        existingTask.setCreatorId(mappedTask.getCreatorId());
                        existingTask.setPriority(mappedTask.getPriority());
                        existingTask.setBlockedByTaskId(mappedTask.getBlockedByTaskId());

                        if (mappedTask.getAssignees() != null) {
                            existingTask.setAssignees(new ArrayList<>(mappedTask.getAssignees()));
                        } else {
                            existingTask.setAssignees(new ArrayList<>());
                        }

                        TaskEntity savedTask = taskRepository.save(existingTask);
                        if (changed) {
                            publishTaskUpdate(workspaceId, savedTask, previousStatus, "TASK_UPDATED");
                        }
                    },
                    () -> {
                        TaskEntity savedTask = taskRepository.save(mappedTask);
                        publishTaskUpdate(workspaceId, savedTask, null, "TASK_CREATED");
                    }
            );
        }
    }

    private boolean hasTaskChanged(TaskEntity existingTask, TaskEntity mappedTask) {
        return !Objects.equals(existingTask.getName(), mappedTask.getName())
                || !Objects.equals(existingTask.getStatusName(), mappedTask.getStatusName())
                || !Objects.equals(existingTask.getStatusType(), mappedTask.getStatusType())
                || !Objects.equals(existingTask.getOrderIndex(), mappedTask.getOrderIndex())
                || !Objects.equals(existingTask.getDateCreated(), mappedTask.getDateCreated())
                || !Objects.equals(existingTask.getDateUpdated(), mappedTask.getDateUpdated())
                || !Objects.equals(existingTask.getDateClosed(), mappedTask.getDateClosed())
                || !Objects.equals(existingTask.getDateDone(), mappedTask.getDateDone())
                || !Objects.equals(existingTask.getListId(), mappedTask.getListId())
                || !Objects.equals(existingTask.getCreatorId(), mappedTask.getCreatorId())
                || !Objects.equals(existingTask.getPriority(), mappedTask.getPriority())
                || !Objects.equals(existingTask.getBlockedByTaskId(), mappedTask.getBlockedByTaskId())
                || !assigneeIds(existingTask).equals(assigneeIds(mappedTask));
    }

    private Set<Long> assigneeIds(TaskEntity task) {
        if (task.getAssignees() == null) {
            return Set.of();
        }
        return task.getAssignees().stream()
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String extractPriority(ClickUpTaskDto taskDto) {
        return taskDto.priority() != null ? taskDto.priority().priority() : null;
    }

    private void publishTaskUpdate(String workspaceId, TaskEntity task, String previousStatus, String eventType) {
        eventPublisher.publishEvent(new TaskChangedEvent(new TaskEventNotification(
                null,
                workspaceId,
                task.getListId(),
                task.getId(),
                task.getName(),
                eventType,
                previousStatus,
                task.getStatusName(),
                task.getDateUpdated() != null
                        ? task.getDateUpdated()
                        : String.valueOf(System.currentTimeMillis()),
                null,
                null
        )));
    }

    private String convertStatusesToJson(List<ClickUpStatusDto> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(statuses);
        } catch (Exception e) {
            throw new IllegalStateException("ClickUp list statüleri JSON'a dönüştürülemedi", e);
        }
    }
}