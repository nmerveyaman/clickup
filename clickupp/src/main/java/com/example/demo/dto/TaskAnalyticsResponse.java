package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskAnalyticsResponse {
    private String taskId;
    private String taskName;
    private String startDate;
    private String endDate;
    private String totalActiveWorkingTime;
    private Map<String, ColumnDetail> columnDetails;

    // Kişi bazlı toplam aktif çalışma süresi (UserId -> Formatlı Süre)
    // Sadece "custom" tipindeki kolonlarda harcanan süreler toplanarak buraya yazılır.
    private Map<Long, String> userActiveWorkingTimes;

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
}