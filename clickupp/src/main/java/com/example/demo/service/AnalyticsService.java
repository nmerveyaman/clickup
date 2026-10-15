package com.example.demo.service;

import com.example.demo.dto.ClickUpStatusDto;
import com.example.demo.dto.SprintAnalyticsResponse;
import com.example.demo.dto.TaskAnalyticsResponse;
import com.example.demo.dto.UserGanttAnalyticsResponse;
import com.example.demo.dto.ClickUpTeamResponseDto;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import com.example.demo.entity.ListEntity;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.TaskEventEntity;
import com.example.demo.enums.TaskEventType;
import com.example.demo.repository.TaskEventRepository;
import com.example.demo.repository.TaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskEventRepository taskEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sprint / Listenin resmi (ListEntity.startDate) veya akıllı fallback (Native SQL) başlangıç tarihini hesaplar.
     */
    public long resolveEffectiveSprintStartDate(ListEntity listEntity) {
        if (listEntity == null) {
            return -1L;
        }

        // 1. ÖNCELİK: List seviyesinde tanımlı startDate var mı?
        if (listEntity.getStartDate() != null && listEntity.getStartDate() > 0) {
            return listEntity.getStartDate();
        }

        if (listEntity.getId() == null) {
            return -1L;
        }

        // 2. ÖNCELİK: Veritabanı sorgusu (Event -> Task date_created)
        return taskRepository.findEffectiveSprintStartDate(listEntity.getId())
                .orElse(-1L);
    }

    private String resolveStatusType(String statusName, Map<String, String> knownStatusTypes) {
        if (statusName == null) return "custom";
        String lower = statusName.toLowerCase(Locale.ROOT).trim();

        if (knownStatusTypes != null && knownStatusTypes.containsKey(lower)) {
            return knownStatusTypes.get(lower);
        }

        if (lower.equals("to do") || lower.equals("todo") || lower.equals("open") || lower.equals("yapılacaklar")) {
            return "open";
        }
        if (lower.equals("complete") || lower.equals("completed") || lower.equals("closed") || lower.equals("done") || lower.equals("tamamlandı")) {
            return "closed";
        }
        if (lower.equals("backlog") || lower.equals("unstarted")) {
            return "unstarted";
        }

        return "custom";
    }

    private boolean sameStatus(String first, String second) {
        if (first == null || second == null) {
            return Objects.equals(first, second);
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private static class StatusSegment {
        String statusName;
        long durationMillis = 0;
        Map<Long, Long> assigneeMillis = new HashMap<>();

        StatusSegment(String statusName) {
            this.statusName = statusName;
        }

        void addDuration(long duration, Long assigneeId) {
            this.durationMillis += duration;
            if (assigneeId != null) {
                this.assigneeMillis.merge(assigneeId, duration, Long::sum);
            }
        }
    }

    private static class ColumnDetailAccumulator {
        String statusName;
        String statusType;
        long durationMillis;
        Map<Long, Long> assigneeMillis = new HashMap<>();

        ColumnDetailAccumulator(String statusName, String statusType) {
            this.statusName = statusName;
            this.statusType = statusType;
        }

        void add(StatusSegment segment) {
            durationMillis += segment.durationMillis;
            segment.assigneeMillis.forEach((userId, duration) ->
                    assigneeMillis.merge(userId, duration, Long::sum));
        }
    }

    public TaskAnalyticsResponse getComprehensiveAnalytics(
            TaskEntity task,
            List<TaskEventEntity> events,
            ListEntity listEntity
    ) {
        Map<String, String> knownStatusTypes = new HashMap<>();
        if (listEntity != null && listEntity.getStatusesJson() != null && !listEntity.getStatusesJson().isBlank()) {
            try {
                List<ClickUpStatusDto> statuses = objectMapper.readValue(
                        listEntity.getStatusesJson(),
                        new TypeReference<List<ClickUpStatusDto>>() {}
                );
                if (statuses != null) {
                    for (ClickUpStatusDto status : statuses) {
                        if (status.status() != null && status.type() != null) {
                            knownStatusTypes.put(
                                    status.status().toLowerCase(Locale.ROOT).trim(),
                                    status.type().toLowerCase(Locale.ROOT)
                            );
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Liste statüleri okunamadı: " + listEntity.getId(), e);
            }
        }

        if (task.getStatusName() != null && task.getStatusType() != null) {
            knownStatusTypes.put(
                    task.getStatusName().toLowerCase(Locale.ROOT).trim(),
                    task.getStatusType().toLowerCase(Locale.ROOT)
            );
        }

        Long effectiveSprintStart = listEntity == null
                ? null
                : resolveEffectiveSprintStartDate(listEntity);
        return getComprehensiveAnalytics(task, events, knownStatusTypes, effectiveSprintStart);
    }

    public TaskAnalyticsResponse getComprehensiveAnalytics(TaskEntity task, List<TaskEventEntity> events, Map<String, String> knownStatusTypes, Long effectiveSprintStart) {
        TaskAnalyticsResponse response = new TaskAnalyticsResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setCurrentStatus(task.getStatusName());
        response.setPriority(task.getPriority());
        response.setStartDate(task.getDateCreated());

        List<TaskEventEntity> safeEvents = (events == null) ? Collections.emptyList() : events;

        String currentTaskType = resolveStatusType(task.getStatusName(), knownStatusTypes);
        boolean isCurrentlyCompleted = "closed".equalsIgnoreCase(currentTaskType) || "done".equalsIgnoreCase(currentTaskType);
        String realEndDate = isCurrentlyCompleted
                ? (task.getDateDone() != null ? task.getDateDone() : task.getDateClosed())
                : null;
        response.setEndDate(realEndDate);

        int rollbackCount = (int) safeEvents.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getEventType() == TaskEventType.STATUS_ROLLBACK)
                .count();
        response.setRollbackCount(rollbackCount);

        response.setBlockedByTaskId(task.getBlockedByTaskId());

        // Scope Creep Hesabı
        boolean scopeCreepDetected = false;
        if (task.getDateCreated() != null && effectiveSprintStart != null && effectiveSprintStart != -1L) {
            long taskCreatedMillis = parseTimestamp(task.getDateCreated());
            if (taskCreatedMillis > effectiveSprintStart) {
                scopeCreepDetected = true;
            }
        }
        response.setScopeCreep(scopeCreepDetected);

        long taskCreatedMillis = parseTimestamp(task.getDateCreated());
        if (taskCreatedMillis <= 0L) {
            return response;
        }

        long parsedEndTimestamp = realEndDate != null ? parseTimestamp(realEndDate) : -1L;
        long taskEndTimestamp = parsedEndTimestamp > 0L
                ? Math.max(taskCreatedMillis, parsedEndTimestamp)
                : System.currentTimeMillis();
        response.setTotalTaskDuration(formatDuration(taskEndTimestamp - taskCreatedMillis));

        List<TaskEventEntity> sortedEvents = safeEvents.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getTimestamp() != null
                        && parseTimestamp(event.getTimestamp()) > 0L
                        && parseTimestamp(event.getTimestamp()) <= taskEndTimestamp)
                .sorted(Comparator.comparing(event -> parseTimestamp(event.getTimestamp())))
                .toList();

        List<StatusSegment> segments = new ArrayList<>();
        long lastTimestamp = taskCreatedMillis;
        long currentStatusStartedAt = taskCreatedMillis;
        Long currentAssignee = getInitialAssignee(task);

        String currentStatus = task.getStatusName();
        if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
            currentStatus = sortedEvents.get(0).getFromStatus();
        }

        StatusSegment activeSegment = currentStatus != null ? new StatusSegment(currentStatus) : null;

        for (TaskEventEntity event : sortedEvents) {
            long eventTimestamp = parseTimestamp(event.getTimestamp());
            if (eventTimestamp <= 0L || eventTimestamp < lastTimestamp) {
                if (event.getToStatus() != null) {
                    currentStatus = event.getToStatus();
                }
                if (event.getToAssignee() != null) {
                    currentAssignee = event.getToAssignee();
                }
                continue;
            }

            long duration = Math.max(0L, eventTimestamp - lastTimestamp);

            if (activeSegment != null) {
                activeSegment.addDuration(duration, currentAssignee);
            }

            if (event.getToStatus() != null && !sameStatus(event.getToStatus(), currentStatus)) {
                if (activeSegment != null) {
                    segments.add(activeSegment);
                }
                currentStatus = event.getToStatus();
                currentStatusStartedAt = eventTimestamp;
                activeSegment = new StatusSegment(currentStatus);
            }

            if (event.getToAssignee() != null) {
                currentAssignee = event.getToAssignee();
            }

            lastTimestamp = eventTimestamp;
        }

        long endTimestamp = taskEndTimestamp;
        if (activeSegment != null && endTimestamp > lastTimestamp) {
            long finalDuration = Math.max(0L, endTimestamp - lastTimestamp);
            activeSegment.addDuration(finalDuration, currentAssignee);
            segments.add(activeSegment);
        } else if (activeSegment != null && segments.isEmpty()) {
            segments.add(activeSegment);
        }

        long twoDaysMillis = 2L * 24L * 60L * 60L * 1000L;
        boolean isStalled = !isCurrentlyCompleted
                && activeSegment != null
                && sameStatus(currentStatus, task.getStatusName())
                && "custom".equalsIgnoreCase(resolveStatusType(task.getStatusName(), knownStatusTypes))
                && endTimestamp - currentStatusStartedAt > twoDaysMillis;
        response.setStalled(isStalled);

        long totalActiveMillis = 0;
        long totalWaitingMillis = 0;
        Map<String, ColumnDetailAccumulator> columnDetailAccumulators = new LinkedHashMap<>();
        List<TaskAnalyticsResponse.ColumnVisit> columnHistory = new ArrayList<>();
        Map<String, Integer> columnVisitCounts = new HashMap<>();
        Map<Long, Long> rawUserActiveMillis = new HashMap<>();
        Map<Long, Long> rawUserWaitingMillis = new HashMap<>();

        for (StatusSegment seg : segments) {
            String rawStatusName = seg.statusName;
            if (rawStatusName == null) {
                continue;
            }
            String lowerName = rawStatusName.toLowerCase(Locale.ROOT).trim();
            String type = resolveStatusType(rawStatusName, knownStatusTypes);

            if ("custom".equalsIgnoreCase(type)) {
                totalActiveMillis += seg.durationMillis;
                for (Map.Entry<Long, Long> assigneeEntry : seg.assigneeMillis.entrySet()) {
                    rawUserActiveMillis.merge(assigneeEntry.getKey(), assigneeEntry.getValue(), Long::sum);
                }
            } else if ("unstarted".equalsIgnoreCase(type)) {
                totalWaitingMillis += seg.durationMillis;
                for (Map.Entry<Long, Long> assigneeEntry : seg.assigneeMillis.entrySet()) {
                    rawUserWaitingMillis.merge(assigneeEntry.getKey(), assigneeEntry.getValue(), Long::sum);
                }
            }

            Map<Long, String> visitAssigneeTimes = new HashMap<>();
            seg.assigneeMillis.forEach((userId, duration) ->
                    visitAssigneeTimes.put(userId, formatDuration(duration)));
            int visitNumber = columnVisitCounts.merge(lowerName, 1, Integer::sum);
            columnHistory.add(new TaskAnalyticsResponse.ColumnVisit(
                    rawStatusName,
                    visitNumber,
                    type,
                    formatDuration(seg.durationMillis),
                    visitAssigneeTimes
            ));

            columnDetailAccumulators.computeIfAbsent(
                    lowerName,
                    key -> new ColumnDetailAccumulator(rawStatusName, type)
            ).add(seg);
        }

        Map<String, TaskAnalyticsResponse.ColumnDetail> columnDetailsMap = new LinkedHashMap<>();
        for (ColumnDetailAccumulator accumulator : columnDetailAccumulators.values()) {
            Map<Long, String> formattedAssigneeTimes = new HashMap<>();
            accumulator.assigneeMillis.forEach((userId, duration) ->
                    formattedAssigneeTimes.put(userId, formatDuration(duration)));

            TaskAnalyticsResponse.ColumnDetail detail = new TaskAnalyticsResponse.ColumnDetail();
            detail.setStatusType(accumulator.statusType);
            detail.setTotalDurationFormatted(formatDuration(accumulator.durationMillis));
            detail.setAssigneeTimes(formattedAssigneeTimes);
            columnDetailsMap.put(accumulator.statusName, detail);
        }

        Map<Long, String> formattedUserActiveTimes = new HashMap<>();
        for (Map.Entry<Long, Long> entry : rawUserActiveMillis.entrySet()) {
            formattedUserActiveTimes.put(entry.getKey(), formatDuration(entry.getValue()));
        }
        response.setUserActiveWorkingTimes(formattedUserActiveTimes);

        Map<Long, String> formattedUserWaitingTimes = new HashMap<>();
        for (Map.Entry<Long, Long> entry : rawUserWaitingMillis.entrySet()) {
            formattedUserWaitingTimes.put(entry.getKey(), formatDuration(entry.getValue()));
        }
        response.setUserWaitingTimes(formattedUserWaitingTimes);

        response.setTotalActiveWorkingTime(formatDuration(totalActiveMillis));
        response.setTotalWaitingTime(formatDuration(totalWaitingMillis));
        response.setColumnDetails(columnDetailsMap);
        response.setColumnHistory(columnHistory);

        return response;
    }

    public SprintAnalyticsResponse getSprintComprehensiveAnalytics(ListEntity listEntity, List<TaskEntity> tasks) {
        SprintAnalyticsResponse sprintResponse = new SprintAnalyticsResponse();
        if (listEntity != null) {
            sprintResponse.setListId(listEntity.getId());
            sprintResponse.setListName(listEntity.getName());
            sprintResponse.setSprintDueDate(listEntity.getDueDate());
        }

        List<ClickUpStatusDto> availableColumns = new ArrayList<>();
        if (listEntity != null && listEntity.getStatusesJson() != null && !listEntity.getStatusesJson().isBlank()) {
            try {
                availableColumns = objectMapper.readValue(
                        listEntity.getStatusesJson(),
                        new TypeReference<List<ClickUpStatusDto>>() {}
                );

                if (availableColumns != null) {
                    availableColumns.sort(Comparator.comparing(
                            ClickUpStatusDto::orderIndex,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ));
                } else {
                    availableColumns = Collections.emptyList();
                }
            } catch (Exception e) {
                System.err.println("statusesJson okuma hatası: " + e.getMessage());
                availableColumns = Collections.emptyList();
            }
        }
        sprintResponse.setAvailableColumns(availableColumns);

        if (tasks == null || tasks.isEmpty()) {
            sprintResponse.setTotalTasks(0);
            sprintResponse.setCompletionRatio("0 / 0");
            sprintResponse.setTotalSprintTaskDuration("0 Saniye");
            sprintResponse.setTotalSprintActiveWorkingTime("0 Saniye");
            sprintResponse.setTotalSprintWaitingTime("0 Saniye");
            sprintResponse.setScopeCreepTasks(Collections.emptyList());
            sprintResponse.setStalledTasks(Collections.emptyList());
            sprintResponse.setBlockedTasks(Collections.emptyList());
            sprintResponse.setUserTotalActiveWorkingTimes(Collections.emptyMap());
            sprintResponse.setUserTotalWaitingTimes(Collections.emptyMap());
            sprintResponse.setTaskAnalyticsList(Collections.emptyList());
            sprintResponse.setUsersMap(Collections.emptyMap());
            sprintResponse.setUserTaskCounts(Collections.emptyMap());
            sprintResponse.setTaskCountPerColumn(Collections.emptyMap());
            return sprintResponse;
        }

        // 1. Sprint Başlangıç Tarihini TEK BİR KEZ Hesapla
        long effectiveStart = resolveEffectiveSprintStartDate(listEntity);
        sprintResponse.setEffectiveSprintStartDate(effectiveStart != -1L ? effectiveStart : null);

        // 2. N+1 Önlemek İçin Event'leri Tek Sorguda Çek ve Grupla
        List<String> taskIds = tasks.stream().map(TaskEntity::getId).filter(Objects::nonNull).toList();
        List<TaskEventEntity> allEvents = taskEventRepository.findByTaskIdIn(taskIds);
        Map<String, List<TaskEventEntity>> eventsByTaskId = (allEvents != null) ? allEvents.stream()
                .collect(Collectors.groupingBy(TaskEventEntity::getTaskId)) : Collections.emptyMap();

        // 3. Statü Tiplerini TEK BİR KEZ Haritala
        Map<String, String> knownStatusTypes = new HashMap<>();
        for (ClickUpStatusDto status : availableColumns) {
            if (status.status() != null && status.type() != null) {
                knownStatusTypes.put(
                        status.status().toLowerCase(Locale.ROOT).trim(),
                        status.type().toLowerCase(Locale.ROOT)
                );
            }
        }
        for (TaskEntity t : tasks) {
            if (t.getStatusName() != null && t.getStatusType() != null) {
                knownStatusTypes.put(t.getStatusName().toLowerCase().trim(), t.getStatusType().toLowerCase());
            }
        }

        int completedCount = 0;
        int totalRollbacks = 0;

        List<SprintAnalyticsResponse.TaskSummaryDto> scopeCreepList = new ArrayList<>();
        List<SprintAnalyticsResponse.TaskSummaryDto> stalledList = new ArrayList<>();
        List<SprintAnalyticsResponse.TaskSummaryDto> blockedList = new ArrayList<>();

        long sprintTotalTaskDurationMillis = 0;
        long sprintTotalActiveMillis = 0;
        long sprintTotalWaitingMillis = 0;
        Map<String, Long> customColumnTotalsMillis = new LinkedHashMap<>();
        Map<Long, List<long[]>> userActiveIntervalsMap = new HashMap<>();
        Map<Long, List<long[]>> userWaitingIntervalsMap = new HashMap<>();
        Set<Long> uniqueSprintAssignees = new HashSet<>();
        List<TaskAnalyticsResponse> taskResponses = new ArrayList<>();

        Map<String, Integer> taskCountPerColumn = new HashMap<>();
        Map<String, Integer> userTaskCounts = new HashMap<>();

        for (TaskEntity task : tasks) {
            if (task.getStatusName() != null) {
                String cleanStatus = task.getStatusName().toLowerCase(Locale.ROOT).trim();
                taskCountPerColumn.put(cleanStatus, taskCountPerColumn.getOrDefault(cleanStatus, 0) + 1);
            }

            if (task.getAssignees() != null) {
                task.getAssignees().forEach(u -> {
                    if (u != null && u.getId() != null) {
                        uniqueSprintAssignees.add(u.getId());
                        String userIdStr = String.valueOf(u.getId());
                        userTaskCounts.put(userIdStr, userTaskCounts.getOrDefault(userIdStr, 0) + 1);
                    }
                });
            }

            List<TaskEventEntity> taskEvents = eventsByTaskId.getOrDefault(task.getId(), Collections.emptyList());

            TaskAnalyticsResponse taskAnalytics = getComprehensiveAnalytics(task, taskEvents, knownStatusTypes, effectiveStart);
            taskResponses.add(taskAnalytics);

            String currentType = resolveStatusType(task.getStatusName(), knownStatusTypes);
            if ("closed".equalsIgnoreCase(currentType) || "done".equalsIgnoreCase(currentType) || taskAnalytics.getEndDate() != null) {
                completedCount++;
            }

            SprintAnalyticsResponse.TaskSummaryDto summary = new SprintAnalyticsResponse.TaskSummaryDto(
                    task.getId(), task.getName(), task.getStatusName()
            );

            if (taskAnalytics.isScopeCreep()) {
                scopeCreepList.add(summary);
            }
            if (taskAnalytics.isStalled()) {
                stalledList.add(summary);
            }
            if (task.getBlockedByTaskId() != null && !task.getBlockedByTaskId().isBlank()) {
                blockedList.add(summary);
            }

            totalRollbacks += taskAnalytics.getRollbackCount();

            long taskCreatedMillis = parseTimestamp(task.getDateCreated());
            if (taskCreatedMillis > 0L) {
                long parsedTaskEnd = taskAnalytics.getEndDate() != null
                        ? parseTimestamp(taskAnalytics.getEndDate())
                        : -1L;
                long taskEndTimestamp = parsedTaskEnd > 0L
                        ? Math.max(taskCreatedMillis, parsedTaskEnd)
                        : System.currentTimeMillis();
                sprintTotalTaskDurationMillis += taskEndTimestamp - taskCreatedMillis;

                List<TaskEventEntity> sortedEvents = taskEvents.stream()
                        .filter(Objects::nonNull)
                        .filter(event -> event.getTimestamp() != null
                                && parseTimestamp(event.getTimestamp()) > 0L
                                && parseTimestamp(event.getTimestamp()) <= taskEndTimestamp)
                        .sorted(Comparator.comparing(event -> parseTimestamp(event.getTimestamp())))
                        .toList();

                long lastTimestamp = taskCreatedMillis;
                Long currentAssignee = getInitialAssignee(task);
                if (currentAssignee != null) {
                    uniqueSprintAssignees.add(currentAssignee);
                }

                String currentStatus = task.getStatusName();
                if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
                    currentStatus = sortedEvents.get(0).getFromStatus();
                }

                for (TaskEventEntity event : sortedEvents) {
                    long eventTimestamp = parseTimestamp(event.getTimestamp());
                    if (eventTimestamp <= 0L || eventTimestamp < lastTimestamp) {
                        if (event.getToStatus() != null) {
                            currentStatus = event.getToStatus();
                        }
                        if (event.getToAssignee() != null) {
                            currentAssignee = event.getToAssignee();
                            uniqueSprintAssignees.add(currentAssignee);
                        }
                        continue;
                    }

                    long duration = Math.max(0L, eventTimestamp - lastTimestamp);
                    if (currentStatus != null && duration > 0L) {
                        String type = resolveStatusType(currentStatus, knownStatusTypes);
                        if ("custom".equalsIgnoreCase(type)) {
                            String cleanStatus = currentStatus.toLowerCase(Locale.ROOT).trim();
                            customColumnTotalsMillis.merge(cleanStatus, duration, Long::sum);
                            sprintTotalActiveMillis += duration;

                            if (currentAssignee != null) {
                                userActiveIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
                                        .add(new long[]{lastTimestamp, eventTimestamp});
                            }
                        } else if ("unstarted".equalsIgnoreCase(type)) {
                            sprintTotalActiveMillis += duration;
                            sprintTotalWaitingMillis += duration;
                            if (currentAssignee != null) {
                                userWaitingIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
                                        .add(new long[]{lastTimestamp, eventTimestamp});
                            }
                        }
                    }

                    if (event.getToStatus() != null) currentStatus = event.getToStatus();
                    if (event.getToAssignee() != null) {
                        currentAssignee = event.getToAssignee();
                        uniqueSprintAssignees.add(currentAssignee);
                    }
                    lastTimestamp = eventTimestamp;
                }

                if (currentStatus != null && taskEndTimestamp > lastTimestamp) {
                    long finalDuration = Math.max(0L, taskEndTimestamp - lastTimestamp);
                    String type = resolveStatusType(currentStatus, knownStatusTypes);

                    if ("custom".equalsIgnoreCase(type) && finalDuration > 0L) {
                        String cleanStatus = currentStatus.toLowerCase(Locale.ROOT).trim();
                        customColumnTotalsMillis.merge(cleanStatus, finalDuration, Long::sum);
                        sprintTotalActiveMillis += finalDuration;

                        if (currentAssignee != null) {
                            userActiveIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
                                    .add(new long[]{lastTimestamp, taskEndTimestamp});
                        }
                    } else if ("unstarted".equalsIgnoreCase(type) && finalDuration > 0L) {
                        sprintTotalActiveMillis += finalDuration;
                        sprintTotalWaitingMillis += finalDuration;
                        if (currentAssignee != null) {
                            userWaitingIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
                                    .add(new long[]{lastTimestamp, taskEndTimestamp});
                        }
                    }
                }
            }
        }

        Map<Long, ClickUpTeamResponseDto.ClickUpUserDto> usersMap = new HashMap<>();
        if (userRepository != null && !uniqueSprintAssignees.isEmpty()) {
            List<UserEntity> sprintUsers = userRepository.findAllById(uniqueSprintAssignees);
            if (sprintUsers != null) {
                for (UserEntity user : sprintUsers) {
                    if (user != null && user.getId() != null) {
                        usersMap.put(user.getId(), new ClickUpTeamResponseDto.ClickUpUserDto(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getProfilePicture(),
                                null, null, null
                        ));
                    }
                }
            }
        }
        sprintResponse.setUsersMap(usersMap);
        sprintResponse.setUserTaskCounts(userTaskCounts);
        sprintResponse.setTaskCountPerColumn(taskCountPerColumn);

        int totalTasks = tasks.size();
        sprintResponse.setTotalTasks(totalTasks);
        sprintResponse.setCompletedTasks(completedCount);
        sprintResponse.setCompletionRatio(completedCount + " / " + totalTasks);
        sprintResponse.setTotalActiveAssignees(uniqueSprintAssignees.size());

        sprintResponse.setScopeCreepTaskCount(scopeCreepList.size());
        sprintResponse.setScopeCreepTasks(scopeCreepList);

        sprintResponse.setStalledTaskCount(stalledList.size());
        sprintResponse.setStalledTasks(stalledList);

        sprintResponse.setBlockedTaskCount(blockedList.size());
        sprintResponse.setBlockedTasks(blockedList);

        sprintResponse.setTotalRollbackCount(totalRollbacks);
        sprintResponse.setTotalSprintTaskDuration(formatDuration(sprintTotalTaskDurationMillis));
        sprintResponse.setTotalSprintActiveWorkingTime(formatDuration(sprintTotalActiveMillis));
        sprintResponse.setTotalSprintWaitingTime(formatDuration(sprintTotalWaitingMillis));

        String bottleneck = null;
        long maxCustomMillis = -1L;
        for (Map.Entry<String, Long> entry : customColumnTotalsMillis.entrySet()) {
            if (entry.getValue() > maxCustomMillis && entry.getValue() > 0) {
                maxCustomMillis = entry.getValue();
                bottleneck = entry.getKey();
            }
        }
        sprintResponse.setBottleneckColumn(bottleneck);

        Map<Long, String> formattedUserTotals = new HashMap<>();
        for (Map.Entry<Long, List<long[]>> entry : userActiveIntervalsMap.entrySet()) {
            long netUserMillis = calculateMergedDurationMillis(entry.getValue());
            formattedUserTotals.put(entry.getKey(), formatDuration(netUserMillis));
        }
        sprintResponse.setUserTotalActiveWorkingTimes(formattedUserTotals);

        Map<Long, String> formattedUserWaitingTotals = new HashMap<>();
        for (Map.Entry<Long, List<long[]>> entry : userWaitingIntervalsMap.entrySet()) {
            long netUserMillis = calculateMergedDurationMillis(entry.getValue());
            formattedUserWaitingTotals.put(entry.getKey(), formatDuration(netUserMillis));
        }
        sprintResponse.setUserTotalWaitingTimes(formattedUserWaitingTotals);
        sprintResponse.setTaskAnalyticsList(taskResponses);

        return sprintResponse;
    }

    private Long getInitialAssignee(TaskEntity task) {
        if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
            return task.getAssignees().get(0).getId();
        }
        return null;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) {
            return "0 Saniye";
        }
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        long remainingHours = hours % 24;
        long remainingMinutes = minutes % 60;
        long remainingSeconds = seconds % 60;

        if (days > 0) {
            return String.format("%d Gün %d Saat %d Dakika %d Saniye", days, remainingHours, remainingMinutes, remainingSeconds);
        } else if (hours > 0) {
            return String.format("%d Saat %d Dakika %d Saniye", hours, remainingMinutes, remainingSeconds);
        } else if (minutes > 0) {
            return String.format("%d Dakika %d Saniye", minutes, remainingSeconds);
        } else {
            return String.format("%d Saniye", Math.max(1, remainingSeconds));
        }
    }

    private long parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) {
            return -1L;
        }

        try {
            long parsed = Long.parseLong(timestampStr.trim());
            return parsed > 0 ? parsed : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private long calculateMergedDurationMillis(List<long[]> intervals) {
        if (intervals == null || intervals.isEmpty()) return 0L;

        List<long[]> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingLong(a -> a[0]));

        List<long[]> merged = new ArrayList<>();
        long[] current = sorted.get(0).clone();
        merged.add(current);

        for (int i = 1; i < sorted.size(); i++) {
            long[] next = sorted.get(i);
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                current = next.clone();
                merged.add(current);
            }
        }

        long netTotalMillis = 0L;
        for (long[] interval : merged) {
            netTotalMillis += Math.max(0L, interval[1] - interval[0]);
        }
        return netTotalMillis;
    }

    public UserGanttAnalyticsResponse getUserGanttAnalytics(Long userId, String listId, List<TaskEntity> tasks) {
        UserGanttAnalyticsResponse response = new UserGanttAnalyticsResponse();
        response.setUserId(userId);
        response.setListId(listId);

        if (tasks == null || tasks.isEmpty()) {
            response.setNetTotalActiveWorkingTime("0 Saniye");
            response.setRawTotalTaskWorkingTime("0 Saniye");
            response.setTaskTimelines(Collections.emptyList());
            return response;
        }

        List<String> taskIds = tasks.stream().map(TaskEntity::getId).filter(Objects::nonNull).toList();
        List<TaskEventEntity> allEvents = taskEventRepository.findByTaskIdIn(taskIds);
        Map<String, List<TaskEventEntity>> eventsByTaskId = (allEvents != null) ? allEvents.stream()
                .collect(Collectors.groupingBy(TaskEventEntity::getTaskId)) : Collections.emptyMap();

        Map<String, String> knownStatusTypes = new HashMap<>();
        for (TaskEntity t : tasks) {
            if (t.getStatusName() != null && t.getStatusType() != null) {
                knownStatusTypes.put(t.getStatusName().toLowerCase().trim(), t.getStatusType().toLowerCase());
            }
        }

        List<long[]> allUserActiveIntervals = new ArrayList<>();
        long rawTotalMillis = 0L;
        List<UserGanttAnalyticsResponse.UserTaskGanttDto> taskTimelines = new ArrayList<>();

        for (TaskEntity task : tasks) {
            long taskCreatedMillis = parseTimestamp(task.getDateCreated());
            if (taskCreatedMillis <= 0L) continue;

            List<TaskEventEntity> sortedEvents = eventsByTaskId.getOrDefault(task.getId(), Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(event -> event.getTimestamp() != null && parseTimestamp(event.getTimestamp()) > 0L)
                    .sorted(Comparator.comparing(event -> parseTimestamp(event.getTimestamp())))
                    .toList();

            String currentTaskType = resolveStatusType(task.getStatusName(), knownStatusTypes);
            boolean isCurrentlyCompleted = "closed".equalsIgnoreCase(currentTaskType) || "done".equalsIgnoreCase(currentTaskType);
            String realEndDate = isCurrentlyCompleted
                    ? (task.getDateDone() != null ? task.getDateDone() : task.getDateClosed())
                    : null;

            long lastTimestamp = taskCreatedMillis;
            Long currentAssignee = getInitialAssignee(task);
            String currentStatus = task.getStatusName();
            if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
                currentStatus = sortedEvents.get(0).getFromStatus();
            }

            List<UserGanttAnalyticsResponse.WorkIntervalDto> userIntervalsForThisTask = new ArrayList<>();
            long userTaskTotalMillis = 0L;

            for (TaskEventEntity event : sortedEvents) {
                long eventTimestamp = parseTimestamp(event.getTimestamp());
                if (eventTimestamp <= 0L || eventTimestamp < lastTimestamp) {
                    if (event.getToStatus() != null) currentStatus = event.getToStatus();
                    if (event.getToAssignee() != null) currentAssignee = event.getToAssignee();
                    continue;
                }

                if (eventTimestamp > lastTimestamp && currentStatus != null && Objects.equals(currentAssignee, userId)) {
                    String type = resolveStatusType(currentStatus, knownStatusTypes);
                    if ("custom".equalsIgnoreCase(type)) {
                        long duration = eventTimestamp - lastTimestamp;
                        if (duration > 0L) {
                            userIntervalsForThisTask.add(new UserGanttAnalyticsResponse.WorkIntervalDto(
                                    currentStatus, lastTimestamp, eventTimestamp, formatDuration(duration), duration
                            ));
                            allUserActiveIntervals.add(new long[]{lastTimestamp, eventTimestamp});
                            userTaskTotalMillis += duration;
                            rawTotalMillis += duration;
                        }
                    }
                }

                if (event.getToStatus() != null) currentStatus = event.getToStatus();
                if (event.getToAssignee() != null) currentAssignee = event.getToAssignee();
                lastTimestamp = eventTimestamp;
            }

            long endTimestamp = (realEndDate != null) ? parseTimestamp(realEndDate) : System.currentTimeMillis();
            if (endTimestamp > lastTimestamp && currentStatus != null && Objects.equals(currentAssignee, userId)) {
                String type = resolveStatusType(currentStatus, knownStatusTypes);
                if ("custom".equalsIgnoreCase(type)) {
                    long duration = endTimestamp - lastTimestamp;
                    if (duration > 0L) {
                        userIntervalsForThisTask.add(new UserGanttAnalyticsResponse.WorkIntervalDto(
                                currentStatus, lastTimestamp, endTimestamp, formatDuration(duration), duration
                        ));
                        allUserActiveIntervals.add(new long[]{lastTimestamp, endTimestamp});
                        userTaskTotalMillis += duration;
                        rawTotalMillis += duration;
                    }
                }
            }

            if (!userIntervalsForThisTask.isEmpty()) {
                long firstStart = userIntervalsForThisTask.get(0).getStartTimestamp();
                long lastEnd = userIntervalsForThisTask.get(userIntervalsForThisTask.size() - 1).getEndTimestamp();

                taskTimelines.add(new UserGanttAnalyticsResponse.UserTaskGanttDto(
                        task.getId(),
                        task.getName(),
                        task.getStatusName(),
                        formatDuration(userTaskTotalMillis),
                        firstStart,
                        lastEnd,
                        userIntervalsForThisTask
                ));
            }
        }

        taskTimelines.sort(Comparator.comparingLong(UserGanttAnalyticsResponse.UserTaskGanttDto::getFirstStartedAt));

        long netMergedMillis = calculateMergedDurationMillis(allUserActiveIntervals);

        response.setNetTotalActiveMillis(netMergedMillis);
        response.setNetTotalActiveWorkingTime(formatDuration(netMergedMillis));
        response.setRawTotalTaskWorkingTime(formatDuration(rawTotalMillis));
        response.setTotalAssignedTasksCount(taskTimelines.size());
        response.setTaskTimelines(taskTimelines);

        return response;
    }
}