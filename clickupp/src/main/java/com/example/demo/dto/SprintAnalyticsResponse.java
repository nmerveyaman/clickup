package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class SprintAnalyticsResponse {
    private String listId;
    private String listName;
    private Long effectiveSprintStartDate;
    private Long sprintDueDate;

    // 1. Genel Sprint Özeti ve Oran
    private int totalTasks;
    private int completedTasks;   // statusType = closed / done
    private int inProgressTasks;  // statusType = custom (Aktif üzerinde çalışılanlar)
    private int todoTasks;        // statusType = open / unstarted (Henüz başlanmamışlar)
    private String completionRatio; // Örn: "10 / 30"
    private int totalActiveAssignees; // Bu sprintte görev almış/çalışmış toplam kişi sayısı

    // 2. Risk ve Kalite Metrikleri (Varsa gösterilecekler)
    private int scopeCreepTaskCount;
    private List<TaskSummaryDto> scopeCreepTasks; // Hangi task'lar scope creep?

    private int stalledTaskCount;
    private List<TaskSummaryDto> stalledTasks;    // Hangi task'lar stalled (hareketsiz)?

    private int blockedTaskCount;
    private List<TaskSummaryDto> blockedTasks;    // Hangi task'lar bloklu?

    private int totalRollbackCount;

    // 3. Efor ve Darboğaz (Bottleneck)
    private String totalSprintActiveWorkingTime;
    private String bottleneckColumn; // Aktif ("custom") statüler arasında en çok vakit harcanan kolon
    private Map<Long, String> userTotalActiveWorkingTimes; // Kullanıcı bazlı toplam aktif çalışma süreleri

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