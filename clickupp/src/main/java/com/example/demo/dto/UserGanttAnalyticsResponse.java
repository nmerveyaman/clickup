package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class UserGanttAnalyticsResponse {
    private Long userId;
    private String listId;

    // Çakışan (paralel) saatlerin elendiği GERÇEK toplam çalışma süresi (Örn: 1 Gün)
    private String netTotalActiveWorkingTime;
    private long netTotalActiveMillis;

    // Task'ların tekil sürelerinin düz toplamı (İstersen "Paralel İş Yükü" olarak göstermek için)
    private String rawTotalTaskWorkingTime;

    // Kullanıcının çalıştığı toplam task sayısı
    private int totalAssignedTasksCount;

    // Gantt Chart satırları (Her bir task ve altındaki çalışma aralıkları)
    private List<UserTaskGanttDto> taskTimelines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTaskGanttDto {
        private String taskId;
        private String taskName;
        private String currentStatus;
        private String totalUserDurationOnTask; // Bu task'ta harcadığı toplam süre
        private long firstStartedAt;            // Gantt satırı başlangıcı
        private long lastEndedAt;               // Gantt satırı bitişi
        private List<WorkIntervalDto> intervals; // Gantt üzerindeki kesikli/kesintisiz bar'lar
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkIntervalDto {
        private String statusName;     // Hangi aktif kolonda çalıştı? (örn: "in progress", "review")
        private long startTimestamp;   // Bar başlangıç tarihi (ms)
        private long endTimestamp;     // Bar bitiş tarihi (ms)
        private String durationFormatted;
        private long durationMillis;
    }
}