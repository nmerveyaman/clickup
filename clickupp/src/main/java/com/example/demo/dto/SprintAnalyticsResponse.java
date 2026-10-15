package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Getter


public class SprintAnalyticsResponse {
    private String listId;
    private String listName;
    private Long effectiveSprintStartDate;
    private Long sprintDueDate;
    private List<ClickUpStatusDto> availableColumns;
    private Map<Long, ClickUpTeamResponseDto.ClickUpUserDto> usersMap;

    private int totalTasks;
    private int completedTasks;
    private String completionRatio;
    private int totalActiveAssignees;
    private Map<String, Integer> taskCountPerColumn;

    private Map<String, Integer> userTaskCounts;
    private int scopeCreepTaskCount;
    private List<TaskSummaryDto> scopeCreepTasks;

    private int stalledTaskCount;
    private List<TaskSummaryDto> stalledTasks;

    private int blockedTaskCount;
    private List<TaskSummaryDto> blockedTasks;

    private int totalRollbackCount;

    // 3. Efor ve Darboğaz (Bottleneck)
    private String totalSprintTaskDuration;
    private String totalSprintActiveWorkingTime;
    private String totalSprintWaitingTime;
    private String bottleneckColumn;
    private Map<Long, String> userTotalActiveWorkingTimes; // Kullanıcı bazlı toplam aktif çalışma süreleri
    private Map<Long, String> userTotalWaitingTimes;

    // 4. Tüm Görevlerin Detaylı Analiz Listesi
    private List<TaskAnalyticsResponse> taskAnalyticsList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummaryDto {
        private String taskId;
        private String taskName;
        private String currentStatus;

    }
}