package com.example.demo.service;

import com.example.demo.dto.SprintAnalyticsResponse;
import com.example.demo.dto.TaskAnalyticsResponse;
import com.example.demo.dto.UserGanttAnalyticsResponse;
import com.example.demo.entity.ListEntity;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.TaskEventEntity;
import com.example.demo.enums.TaskEventType;
import com.example.demo.repository.TaskEventRepository;
import com.example.demo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final TaskEventRepository taskEventRepository;

    /**
     * Sprint / Listenin resmi veya akıllı fallback başlangıç tarihini hesaplar.
     * 1. Öncelik: ListEntity üzerinde ClickUp'tan gelen resmi startDate varsa o kullanılır.
     * 2. Fallback A: Listedeki tüm task'lardan statusType = 'open' (To-Do) olan kolon isimleri tespit edilir.
     *    Ardından listedeki tüm task event'leri taranarak bu 'open' kolonlarına yapılan İLK geçiş anı bulunur.
     * 3. Fallback B: Eğer hiç event yoksa (görevler doğrudan 'open' olarak açıldıysa),
     *    listedeki 'unstarted' olmayan görevlerin en erken oluşturulma tarihi (dateCreated) baz alınır.
     */
    public long resolveEffectiveSprintStartDate(ListEntity listEntity, TaskEntity currentTask) {
        if (listEntity != null && listEntity.getStartDate() != null) {
            return listEntity.getStartDate();
        }

        String listId = (listEntity != null) ? listEntity.getId() : currentTask.getListId();
        if (listId == null) {
            return -1L;
        }

        List<TaskEntity> listTasks = taskRepository.findByListId(listId);
        if (listTasks == null || listTasks.isEmpty()) {
            return -1L;
        }

        Set<String> openStatusNames = listTasks.stream()
                .filter(t -> "open".equalsIgnoreCase(t.getStatusType()) && t.getStatusName() != null)
                .map(t -> t.getStatusName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> taskIds = listTasks.stream()
                .map(TaskEntity::getId)
                .toList();

        List<TaskEventEntity> allListEvents = taskEventRepository.findByTaskIdIn(taskIds);

        if (allListEvents != null && !allListEvents.isEmpty() && !openStatusNames.isEmpty()) {
            Optional<Long> earliestOpenTransition = allListEvents.stream()
                    .filter(e -> e.getToStatus() != null && openStatusNames.contains(e.getToStatus().toLowerCase()))
                    .map(e -> parseTimestamp(e.getTimestamp()))
                    .min(Long::compareTo);

            if (earliestOpenTransition.isPresent()) {
                return earliestOpenTransition.get();
            }
        }

        return listTasks.stream()
                .filter(t -> t.getDateCreated() != null && !"unstarted".equalsIgnoreCase(t.getStatusType()))
                .map(t -> parseTimestamp(t.getDateCreated()))
                .min(Long::compareTo)
                .orElseGet(() -> listTasks.stream()
                        .filter(t -> t.getDateCreated() != null)
                        .map(t -> parseTimestamp(t.getDateCreated()))
                        .min(Long::compareTo)
                        .orElse(-1L));
    }

    /**
     * Statü adının gerçek tipini (open, custom, closed, unstarted) bulur.
     * Önce veritabanındaki diğer task'lardan öğrenir, bulamazsa isimden çözer.
     */
    private String resolveStatusType(String statusName, Map<String, String> knownStatusTypes) {
        if (statusName == null) return "custom";
        String lower = statusName.toLowerCase().trim();

        if (knownStatusTypes.containsKey(lower)) {
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

    /**
     * Kronolojik her bir statü ziyaretini (segment) tutan iç yardımcı sınıf
     */
    private static class StatusSegment {
        String statusName;
        long durationMillis = 0;
        Map<Long, Long> assigneeMillis = new HashMap<>(); // Düzeltildi

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

    /**
     * 1. Kolon bazlı toplam süreler (Formatlı: "X Gün Y Saat Z Dakika Saniye")
     */
    public Map<String, String> calculateFormattedTimeSpentPerColumn(TaskEntity task, List<TaskEventEntity> events) {
        Map<String, Long> millisMap = calculateTimeSpentMillis(task, events);
        Map<String, String> formattedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : millisMap.entrySet()) {
            formattedMap.put(entry.getKey(), formatDuration(entry.getValue()));
        }
        return formattedMap;
    }

    /**
     * 2. DETAYLI ANALİZ: Hangi kolonda kim ne kadar süre harcadı?
     */
    public Map<String, Map<Long, String>> calculateTimeSpentPerColumnAndAssignee(TaskEntity task, List<TaskEventEntity> events) {
        Map<String, Map<Long, Long>> rawMap = new LinkedHashMap<>();

        if (task.getDateCreated() == null) return new HashMap<>();

        long lastTimestamp = parseTimestamp(task.getDateCreated());
        String currentStatus = task.getStatusName();
        Long currentAssignee = getInitialAssignee(task);

        List<TaskEventEntity> sortedEvents = events.stream()
                .sorted(Comparator.comparing(TaskEventEntity::getTimestamp))
                .toList();

        if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
            currentStatus = sortedEvents.get(0).getFromStatus();
        }

        for (TaskEventEntity event : sortedEvents) {
            long eventTimestamp = parseTimestamp(event.getTimestamp());
            long duration = eventTimestamp - lastTimestamp;

            if (duration > 0 && currentStatus != null && currentAssignee != null) {
                rawMap.computeIfAbsent(currentStatus, k -> new HashMap<>())
                        .merge(currentAssignee, duration, Long::sum);
            }

            if (event.getToStatus() != null) {
                currentStatus = event.getToStatus();
            }
            if (event.getToAssignee() != null) {
                currentAssignee = event.getToAssignee();
            }

            lastTimestamp = eventTimestamp;
        }

        long endTimestamp = getEndTimestamp(task);
        if (currentStatus != null && currentAssignee != null && endTimestamp > lastTimestamp) {
            long finalDuration = endTimestamp - lastTimestamp;
            rawMap.computeIfAbsent(currentStatus, k -> new HashMap<>())
                    .merge(currentAssignee, finalDuration, Long::sum);
        }

        Map<String, Map<Long, String>> formattedResult = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Long, Long>> statusEntry : rawMap.entrySet()) {
            Map<Long, String> assigneeFormatted = new HashMap<>();
            for (Map.Entry<Long, Long> assigneeEntry : statusEntry.getValue().entrySet()) {
                assigneeFormatted.put(assigneeEntry.getKey(), formatDuration(assigneeEntry.getValue()));
            }
            formattedResult.put(statusEntry.getKey(), assigneeFormatted);
        }

        return formattedResult;
    }

    private Map<String, Long> calculateTimeSpentMillis(TaskEntity task, List<TaskEventEntity> events) {
        Map<String, Long> timeSpentMap = new LinkedHashMap<>();
        if (task.getDateCreated() == null) return timeSpentMap;

        long lastTimestamp = parseTimestamp(task.getDateCreated());
        List<TaskEventEntity> sortedEvents = events.stream()
                .sorted(Comparator.comparing(TaskEventEntity::getTimestamp))
                .toList();

        for (TaskEventEntity event : sortedEvents) {
            long eventTimestamp = parseTimestamp(event.getTimestamp());
            long duration = eventTimestamp - lastTimestamp;

            if (duration > 0 && event.getFromStatus() != null) {
                timeSpentMap.put(event.getFromStatus(),
                        timeSpentMap.getOrDefault(event.getFromStatus(), 0L) + duration);
            }
            lastTimestamp = eventTimestamp;
        }

        long endTimestamp = getEndTimestamp(task);
        if (task.getStatusName() != null && endTimestamp > lastTimestamp) {
            long finalDuration = endTimestamp - lastTimestamp;
            timeSpentMap.put(task.getStatusName(),
                    timeSpentMap.getOrDefault(task.getStatusName(), 0L) + finalDuration);
        }

        return timeSpentMap;
    }

    private long getEndTimestamp(TaskEntity task) {
        if (task.getDateDone() != null) return parseTimestamp(task.getDateDone());
        if (task.getDateClosed() != null) return parseTimestamp(task.getDateClosed());
        return System.currentTimeMillis();
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
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    public TaskAnalyticsResponse getComprehensiveAnalytics(TaskEntity task, List<TaskEventEntity> events) {
        return getComprehensiveAnalytics(task, events, null);
    }

    /**
     * 3. TEKİL GÖREV KAPSAMLI ANALİZİ (Sıralı Adım Geçmişi, Doğru StatusType ve Scope Creep Entegreli)
     */
    public TaskAnalyticsResponse getComprehensiveAnalytics(TaskEntity task, List<TaskEventEntity> events, ListEntity listEntity) {
        TaskAnalyticsResponse response = new TaskAnalyticsResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setStartDate(task.getDateCreated());

        // A. Bilinen tüm statü tiplerini o listedeki task'lardan dinamik olarak öğren
        Map<String, String> knownStatusTypes = new HashMap<>();
        if (task.getListId() != null) {
            List<TaskEntity> listTasks = taskRepository.findByListId(task.getListId());
            if (listTasks != null) {
                for (TaskEntity t : listTasks) {
                    if (t.getStatusName() != null && t.getStatusType() != null) {
                        knownStatusTypes.put(t.getStatusName().toLowerCase().trim(), t.getStatusType().toLowerCase());
                    }
                }
            }
        }
        if (task.getStatusName() != null && task.getStatusType() != null) {
            knownStatusTypes.put(task.getStatusName().toLowerCase().trim(), task.getStatusType().toLowerCase());
        }

        // B. Task şu an gerçekten kapalı mı kontrol et (Rollback yapıldıysa endDate null olmalı)
        String currentTaskType = resolveStatusType(task.getStatusName(), knownStatusTypes);
        boolean isCurrentlyCompleted = "closed".equalsIgnoreCase(currentTaskType) || "done".equalsIgnoreCase(currentTaskType);
        String realEndDate = isCurrentlyCompleted
                ? (task.getDateDone() != null ? task.getDateDone() : task.getDateClosed())
                : null;
        response.setEndDate(realEndDate);

        // C. Rollback Sayısı
        int rollbackCount = (int) events.stream()
                .filter(e -> e.getEventType() == TaskEventType.STATUS_ROLLBACK)
                .count();
        response.setRollbackCount(rollbackCount);

        // D. Dependency
        response.setBlockedByTaskId(task.getBlockedByTaskId());

        // E. Akıllı Scope Creep Tespiti
        boolean scopeCreepDetected = false;
        if (task.getDateCreated() != null) {
            long taskCreatedMillis = parseTimestamp(task.getDateCreated());
            long effectiveSprintStart = resolveEffectiveSprintStartDate(listEntity, task);

            if (effectiveSprintStart != -1L && taskCreatedMillis > effectiveSprintStart) {
                scopeCreepDetected = true;
            }
        }
        response.setScopeCreep(scopeCreepDetected);

        // F. Stalled Uyarısı
        boolean isStalled = false;
        if (!events.isEmpty() && !isCurrentlyCompleted) {
            String lastEventTime = events.get(events.size() - 1).getTimestamp();
            long diff = System.currentTimeMillis() - parseTimestamp(lastEventTime);
            long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;
            if (diff > threeDaysMillis) {
                isStalled = true;
            }
        }
        response.setStalled(isStalled);

        if (task.getDateCreated() == null) {
            return response;
        }

        List<TaskEventEntity> sortedEvents = events.stream()
                .sorted(Comparator.comparing(TaskEventEntity::getTimestamp))
                .toList();

        // G. KRONOLOJİK ADIM (SEGMENT) HESAPLAMASI (A -> B -> C -> B sırasını korur, üstüne yazmaz!)
        List<StatusSegment> segments = new ArrayList<>();

        long lastTimestamp = parseTimestamp(task.getDateCreated());
        Long currentAssignee = getInitialAssignee(task);

        String currentStatus = task.getStatusName();
        if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
            currentStatus = sortedEvents.get(0).getFromStatus();
        }

        StatusSegment activeSegment = currentStatus != null ? new StatusSegment(currentStatus) : null;

        for (TaskEventEntity event : sortedEvents) {
            long eventTimestamp = parseTimestamp(event.getTimestamp());
            long duration = Math.max(0, eventTimestamp - lastTimestamp);

            if (activeSegment != null) {
                activeSegment.addDuration(duration, currentAssignee);
            }

            if (event.getToStatus() != null && !event.getToStatus().equalsIgnoreCase(currentStatus)) {
                if (activeSegment != null) {
                    segments.add(activeSegment);
                }
                currentStatus = event.getToStatus();
                activeSegment = new StatusSegment(currentStatus);
            }

            if (event.getToAssignee() != null) {
                currentAssignee = event.getToAssignee();
            }

            lastTimestamp = eventTimestamp;
        }

        long endTimestamp = (realEndDate != null) ? parseTimestamp(realEndDate) : System.currentTimeMillis();
        if (activeSegment != null) {
            long finalDuration = Math.max(0, endTimestamp - lastTimestamp);
            activeSegment.addDuration(finalDuration, currentAssignee);
            segments.add(activeSegment);
        }

        // H. Segmentleri LinkedHashMap'e aktar (Sırayı korur, tekrar eden kolonları "(2)", "(3)" diye ayırır)
        long totalActiveMillis = 0;
        Map<String, TaskAnalyticsResponse.ColumnDetail> columnDetailsMap = new LinkedHashMap<>();
        Map<Long, Long> rawUserActiveMillis = new HashMap<>();
        Map<String, Integer> statusVisitCount = new HashMap<>();

        for (StatusSegment seg : segments) {
            String rawStatusName = seg.statusName;
            String lowerName = rawStatusName.toLowerCase().trim();
            String type = resolveStatusType(rawStatusName, knownStatusTypes);

            if ("custom".equalsIgnoreCase(type)) {
                totalActiveMillis += seg.durationMillis;
                for (Map.Entry<Long, Long> assigneeEntry : seg.assigneeMillis.entrySet()) {
                    rawUserActiveMillis.merge(assigneeEntry.getKey(), assigneeEntry.getValue(), Long::sum);
                }
            }

            int visitNum = statusVisitCount.getOrDefault(lowerName, 0) + 1;
            statusVisitCount.put(lowerName, visitNum);

            String displayKey = (visitNum == 1) ? rawStatusName : rawStatusName + " (" + visitNum + ")";

            Map<Long, String> formattedAssigneeTimes = new HashMap<>();
            for (Map.Entry<Long, Long> assigneeEntry : seg.assigneeMillis.entrySet()) {
                formattedAssigneeTimes.put(assigneeEntry.getKey(), formatDuration(assigneeEntry.getValue()));
            }

            TaskAnalyticsResponse.ColumnDetail detail = new TaskAnalyticsResponse.ColumnDetail();
            detail.setStatusType(type);
            detail.setTotalDurationFormatted(formatDuration(seg.durationMillis));
            detail.setAssigneeTimes(formattedAssigneeTimes);

            columnDetailsMap.put(displayKey, detail);
        }

        Map<Long, String> formattedUserActiveTimes = new HashMap<>();
        for (Map.Entry<Long, Long> entry : rawUserActiveMillis.entrySet()) {
            formattedUserActiveTimes.put(entry.getKey(), formatDuration(entry.getValue()));
        }
        response.setUserActiveWorkingTimes(formattedUserActiveTimes);

        response.setTotalActiveWorkingTime(formatDuration(totalActiveMillis));
        response.setColumnDetails(columnDetailsMap);

        return response;
    }

    /**
     * 4. SPRINT / LİSTE BAZLI TOPLU ÖZET ANALİZ (Önyüz İhtiyaçlarına Göre Özelleştirilmiş + Interval Merging)
     */
    public SprintAnalyticsResponse getSprintComprehensiveAnalytics(ListEntity listEntity, List<TaskEntity> tasks) {
        SprintAnalyticsResponse sprintResponse = new SprintAnalyticsResponse();
        sprintResponse.setListId(listEntity.getId());
        sprintResponse.setListName(listEntity.getName());
        sprintResponse.setSprintDueDate(listEntity.getDueDate());

        if (tasks == null || tasks.isEmpty()) {
            sprintResponse.setTotalTasks(0);
            sprintResponse.setCompletionRatio("0 / 0");
            sprintResponse.setTotalSprintActiveWorkingTime("0 Saniye");
            sprintResponse.setScopeCreepTasks(Collections.emptyList());
            sprintResponse.setStalledTasks(Collections.emptyList());
            sprintResponse.setBlockedTasks(Collections.emptyList());
            sprintResponse.setUserTotalActiveWorkingTimes(new HashMap<>());
            sprintResponse.setTaskAnalyticsList(Collections.emptyList());
            return sprintResponse;
        }

        // Sprint başlangıç tarihini belirle
        long effectiveStart = resolveEffectiveSprintStartDate(listEntity, tasks.get(0));
        sprintResponse.setEffectiveSprintStartDate(effectiveStart != -1L ? effectiveStart : null);

        // N+1 sorgu problemini önlemek için tüm task'ların event'lerini TEK SORGUDA çekip grupla
        List<String> taskIds = tasks.stream().map(TaskEntity::getId).toList();
        List<TaskEventEntity> allEvents = taskEventRepository.findByTaskIdIn(taskIds);
        Map<String, List<TaskEventEntity>> eventsByTaskId = allEvents.stream()
                .collect(Collectors.groupingBy(TaskEventEntity::getTaskId));

        // Bilinen statü tiplerini tüm sprint görevlerinden öğren
        Map<String, String> knownStatusTypes = new HashMap<>();
        for (TaskEntity t : tasks) {
            if (t.getStatusName() != null && t.getStatusType() != null) {
                knownStatusTypes.put(t.getStatusName().toLowerCase().trim(), t.getStatusType().toLowerCase());
            }
        }

        int completedCount = 0;
        int inProgressCustomCount = 0;
        int todoCount = 0;
        int totalRollbacks = 0;

        List<SprintAnalyticsResponse.TaskSummaryDto> scopeCreepList = new ArrayList<>();
        List<SprintAnalyticsResponse.TaskSummaryDto> stalledList = new ArrayList<>();
        List<SprintAnalyticsResponse.TaskSummaryDto> blockedList = new ArrayList<>();

        long sprintTotalActiveMillis = 0;
        Map<String, Long> customColumnTotalsMillis = new LinkedHashMap<>();
        Map<Long, List<long[]>> userActiveIntervalsMap = new HashMap<>(); // Düzeltildi: Kesişen aralıkları tutar
        Set<Long> uniqueSprintAssignees = new HashSet<>();
        List<TaskAnalyticsResponse> taskResponses = new ArrayList<>();

        for (TaskEntity task : tasks) {
            if (task.getAssignees() != null) {
                task.getAssignees().forEach(u -> uniqueSprintAssignees.add(u.getId()));
            }

            List<TaskEventEntity> taskEvents = eventsByTaskId.getOrDefault(task.getId(), Collections.emptyList());
            TaskAnalyticsResponse taskAnalytics = getComprehensiveAnalytics(task, taskEvents, listEntity);
            taskResponses.add(taskAnalytics);

            // 1. Görevin güncel statü tipine göre sayaçları artır (Closed / Custom / Open)
            String currentType = resolveStatusType(task.getStatusName(), knownStatusTypes);
            if ("closed".equalsIgnoreCase(currentType) || "done".equalsIgnoreCase(currentType) || taskAnalytics.getEndDate() != null) {
                completedCount++;
            } else if ("custom".equalsIgnoreCase(currentType)) {
                inProgressCustomCount++;
            } else {
                todoCount++;
            }

            // 2. Risk listelerini doldur (Hangileri Stalled, Scope Creep veya Blocked?)
            SprintAnalyticsResponse.TaskSummaryDto summary = new SprintAnalyticsResponse.TaskSummaryDto(
                    task.getId(), task.getName(), task.getStatusName()
            );

            if (taskAnalytics.isScopeCreep()) {
                scopeCreepList.add(summary);
            }
            if (taskAnalytics.isStalled()) {
                stalledList.add(summary);
            }
            if (taskAnalytics.getBlockedByTaskId() != null && !taskAnalytics.getBlockedByTaskId().isBlank()) {
                blockedList.add(summary);
            }

            totalRollbacks += taskAnalytics.getRollbackCount();

            // 3. Aktif çalışma süresi, darboğaz (bottleneck) ve kullanıcı eforlarını hesapla
            if (task.getDateCreated() != null) {
                List<TaskEventEntity> sortedEvents = taskEvents.stream()
                        .sorted(Comparator.comparing(TaskEventEntity::getTimestamp))
                        .toList();

                long lastTimestamp = parseTimestamp(task.getDateCreated());
                Long currentAssignee = getInitialAssignee(task);
                if (currentAssignee != null) uniqueSprintAssignees.add(currentAssignee);

                String currentStatus = task.getStatusName();
                if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
                    currentStatus = sortedEvents.get(0).getFromStatus();
                }

                for (TaskEventEntity event : sortedEvents) {
                    long eventTimestamp = parseTimestamp(event.getTimestamp());
                    long duration = Math.max(0, eventTimestamp - lastTimestamp);

                    if (currentStatus != null) {
                        String type = resolveStatusType(currentStatus, knownStatusTypes);
                        if ("custom".equalsIgnoreCase(type)) {
                            String cleanStatus = currentStatus.toLowerCase().trim();
                            customColumnTotalsMillis.merge(cleanStatus, duration, Long::sum);
                            sprintTotalActiveMillis += duration;

                            if (currentAssignee != null && eventTimestamp > lastTimestamp) {
                                userActiveIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
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

                long endTimestamp = (taskAnalytics.getEndDate() != null)
                        ? parseTimestamp(taskAnalytics.getEndDate())
                        : System.currentTimeMillis();

                if (currentStatus != null && endTimestamp > lastTimestamp) {
                    long finalDuration = endTimestamp - lastTimestamp;
                    String type = resolveStatusType(currentStatus, knownStatusTypes);

                    if ("custom".equalsIgnoreCase(type)) {
                        String cleanStatus = currentStatus.toLowerCase().trim();
                        customColumnTotalsMillis.merge(cleanStatus, finalDuration, Long::sum);
                        sprintTotalActiveMillis += finalDuration;

                        if (currentAssignee != null && endTimestamp > lastTimestamp) {
                            userActiveIntervalsMap.computeIfAbsent(currentAssignee, k -> new ArrayList<>())
                                    .add(new long[]{lastTimestamp, endTimestamp});
                        }
                    }
                }
            }
        }

        int totalTasks = tasks.size();
        sprintResponse.setTotalTasks(totalTasks);
        sprintResponse.setCompletedTasks(completedCount);
        sprintResponse.setInProgressTasks(inProgressCustomCount);
        sprintResponse.setTodoTasks(todoCount);
        sprintResponse.setCompletionRatio(completedCount + " / " + totalTasks);
        sprintResponse.setTotalActiveAssignees(uniqueSprintAssignees.size());

        sprintResponse.setScopeCreepTaskCount(scopeCreepList.size());
        sprintResponse.setScopeCreepTasks(scopeCreepList);

        sprintResponse.setStalledTaskCount(stalledList.size());
        sprintResponse.setStalledTasks(stalledList);

        sprintResponse.setBlockedTaskCount(blockedList.size());
        sprintResponse.setBlockedTasks(blockedList);

        sprintResponse.setTotalRollbackCount(totalRollbacks);
        sprintResponse.setTotalSprintActiveWorkingTime(formatDuration(sprintTotalActiveMillis));

        // Darboğaz (Bottleneck): Sadece "custom" statüler içinde en çok vakit harcanan kolon
        String bottleneck = null;
        long maxCustomMillis = -1L;
        for (Map.Entry<String, Long> entry : customColumnTotalsMillis.entrySet()) {
            if (entry.getValue() > maxCustomMillis && entry.getValue() > 0) {
                maxCustomMillis = entry.getValue();
                bottleneck = entry.getKey();
            }
        }
        sprintResponse.setBottleneckColumn(bottleneck);

        // Kullanıcıların çakışmalardan arındırılmış NET toplam aktif çalışma süreleri (Interval Merging)
        Map<Long, String> formattedUserTotals = new HashMap<>();
        for (Map.Entry<Long, List<long[]>> entry : userActiveIntervalsMap.entrySet()) {
            long netUserMillis = calculateMergedDurationMillis(entry.getValue());
            formattedUserTotals.put(entry.getKey(), formatDuration(netUserMillis));
        }
        sprintResponse.setUserTotalActiveWorkingTimes(formattedUserTotals);
        sprintResponse.setTaskAnalyticsList(taskResponses);

        return sprintResponse;
    }

    /**
     * Kesişen (Paralel) Zaman Aralıklarını Birleştirme (Interval Merging) Algoritması.
     * Bir kullanıcının aynı anda birden fazla task üzerinde çalıştığı çakışan süreleri tekilleştirir.
     */
    private long calculateMergedDurationMillis(List<long[]> intervals) {
        if (intervals == null || intervals.isEmpty()) return 0L;

        // Başlangıç zamanına göre sırala
        List<long[]> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingLong(a -> a[0]));

        List<long[]> merged = new ArrayList<>();
        long[] current = sorted.get(0).clone();
        merged.add(current);

        for (int i = 1; i < sorted.size(); i++) {
            long[] next = sorted.get(i);
            if (next[0] <= current[1]) {
                // Çakışma (Overlap) var! Bitiş zamanını büyük olanla güncelle (Birleştir)
                current[1] = Math.max(current[1], next[1]);
            } else {
                // Çakışma yok, yeni bağımsız zaman aralığı olarak ekle
                current = next.clone();
                merged.add(current);
            }
        }

        // Birleştirilmiş net zaman aralıklarının toplam süresini hesapla
        long netTotalMillis = 0L;
        for (long[] interval : merged) {
            netTotalMillis += Math.max(0L, interval[1] - interval[0]);
        }
        return netTotalMillis;
    }

    /**
     * 5. KULLANICI BAZLI GANTT CHART VE NET ÇALIŞMA SÜRESİ ANALİZİ
     * Kullanıcının bir sprintte (veya tüm tasklarda) hangi görevlerde, hangi tarih aralıklarında çalıştığını
     * ve paralel çakışmalar elendikten sonraki gerçek çalışma süresini döner.
     */
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

        // Tüm task'ların event'lerini tek sorguda çek
        List<String> taskIds = tasks.stream().map(TaskEntity::getId).toList();
        List<TaskEventEntity> allEvents = taskEventRepository.findByTaskIdIn(taskIds);
        Map<String, List<TaskEventEntity>> eventsByTaskId = allEvents.stream()
                .collect(Collectors.groupingBy(TaskEventEntity::getTaskId));

        // Bilinen statü tiplerini öğren
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
            if (task.getDateCreated() == null) continue;

            List<TaskEventEntity> sortedEvents = eventsByTaskId.getOrDefault(task.getId(), Collections.emptyList())
                    .stream()
                    .sorted(Comparator.comparing(TaskEventEntity::getTimestamp))
                    .toList();

            String currentTaskType = resolveStatusType(task.getStatusName(), knownStatusTypes);
            boolean isCurrentlyCompleted = "closed".equalsIgnoreCase(currentTaskType) || "done".equalsIgnoreCase(currentTaskType);
            String realEndDate = isCurrentlyCompleted
                    ? (task.getDateDone() != null ? task.getDateDone() : task.getDateClosed())
                    : null;

            long lastTimestamp = parseTimestamp(task.getDateCreated());
            Long currentAssignee = getInitialAssignee(task);
            String currentStatus = task.getStatusName();
            if (!sortedEvents.isEmpty() && sortedEvents.get(0).getFromStatus() != null) {
                currentStatus = sortedEvents.get(0).getFromStatus();
            }

            List<UserGanttAnalyticsResponse.WorkIntervalDto> userIntervalsForThisTask = new ArrayList<>();
            long userTaskTotalMillis = 0L;

            for (TaskEventEntity event : sortedEvents) {
                long eventTimestamp = parseTimestamp(event.getTimestamp());
                if (eventTimestamp > lastTimestamp && currentStatus != null && Objects.equals(currentAssignee, userId)) {
                    String type = resolveStatusType(currentStatus, knownStatusTypes);
                    if ("custom".equalsIgnoreCase(type)) {
                        long duration = eventTimestamp - lastTimestamp;
                        userIntervalsForThisTask.add(new UserGanttAnalyticsResponse.WorkIntervalDto(
                                currentStatus, lastTimestamp, eventTimestamp, formatDuration(duration), duration
                        ));
                        allUserActiveIntervals.add(new long[]{lastTimestamp, eventTimestamp});
                        userTaskTotalMillis += duration;
                        rawTotalMillis += duration;
                    }
                }

                if (event.getToStatus() != null) currentStatus = event.getToStatus();
                if (event.getToAssignee() != null) currentAssignee = event.getToAssignee();
                lastTimestamp = eventTimestamp;
            }

            // Son (veya devam eden) adımdaki süreyi kontrol et
            long endTimestamp = (realEndDate != null) ? parseTimestamp(realEndDate) : System.currentTimeMillis();
            if (endTimestamp > lastTimestamp && currentStatus != null && Objects.equals(currentAssignee, userId)) {
                String type = resolveStatusType(currentStatus, knownStatusTypes);
                if ("custom".equalsIgnoreCase(type)) {
                    long duration = endTimestamp - lastTimestamp;
                    userIntervalsForThisTask.add(new UserGanttAnalyticsResponse.WorkIntervalDto(
                            currentStatus, lastTimestamp, endTimestamp, formatDuration(duration), duration
                    ));
                    allUserActiveIntervals.add(new long[]{lastTimestamp, endTimestamp});
                    userTaskTotalMillis += duration;
                    rawTotalMillis += duration;
                }
            }

            // Eğer kullanıcının bu task üzerinde aktif bir çalışması varsa Gantt listesine ekle
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

        // Gantt satırlarını ilk başlama tarihine göre sırala
        taskTimelines.sort(Comparator.comparingLong(UserGanttAnalyticsResponse.UserTaskGanttDto::getFirstStartedAt));

        // Çakışmaları eleyerek GERÇEK (Net) çalışma süresini hesapla!
        long netMergedMillis = calculateMergedDurationMillis(allUserActiveIntervals);

        response.setNetTotalActiveMillis(netMergedMillis);
        response.setNetTotalActiveWorkingTime(formatDuration(netMergedMillis));
        response.setRawTotalTaskWorkingTime(formatDuration(rawTotalMillis));
        response.setTotalAssignedTasksCount(taskTimelines.size());
        response.setTaskTimelines(taskTimelines);

        return response;
    }
}