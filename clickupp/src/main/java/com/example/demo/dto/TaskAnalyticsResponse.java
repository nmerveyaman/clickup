package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskAnalyticsResponse {
    private String taskId;
    private String taskName;
    private String currentStatus;
    private String priority;
    private String startDate;
    private String endDate;
    private String totalTaskDuration;
    private String totalActiveWorkingTime;
    private String totalWaitingTime;
    private Map<String, ColumnDetail> columnDetails;
    private List<ColumnVisit> columnHistory;

    // Kişi bazlı toplam aktif çalışma süresi (UserId -> Formatlı Süre)
    // "custom" tipindeki kolonlarda geçen süreler toplanarak buraya yazılır.
    private Map<Long, String> userActiveWorkingTimes;
    // Atanan kullanıcı görevdeyken "unstarted" statüsünde geçen süre.
    private Map<Long, String> userWaitingTimes;

    private int rollbackCount;
    private boolean isStalled;
    private String blockedByTaskId;
    private boolean isScopeCreep;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColumnDetail {
        private String statusType;
        private String totalDurationFormatted;
        private Map<Long, String> assigneeTimes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColumnVisit {
        private String statusName;
        private int visitNumber;
        private String statusType;
        private String totalDurationFormatted;
        private Map<Long, String> assigneeTimes;
    }
}