package com.example.demo.controller;

import com.example.demo.dto.SprintAnalyticsResponse;
import com.example.demo.dto.TaskAnalyticsResponse;
import com.example.demo.dto.UserGanttAnalyticsResponse;
import com.example.demo.entity.ListEntity;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.TaskEventEntity;
import com.example.demo.repository.ListRepository;
import com.example.demo.repository.TaskEventRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TaskRepository taskRepository;
    private final TaskEventRepository taskEventRepository;
    private final ListRepository listRepository;

    /**
     * 1. Tekil Görev (Task) Kapsamlı Analiz Endpoint'i
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(@PathVariable String taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task bulunamadı: " + taskId));

        List<TaskEventEntity> events = taskEventRepository.findByTaskIdOrderByTimestampAsc(taskId);

        ListEntity listEntity = null;
        if (task.getListId() != null) {
            listEntity = listRepository.findById(task.getListId()).orElse(null);
        }

        TaskAnalyticsResponse response = analyticsService.getComprehensiveAnalytics(task, events, listEntity);
        return ResponseEntity.ok(response);
    }

    public TaskAnalyticsResponse getComprehensiveAnalytics(TaskEntity task, List<TaskEventEntity> events, ListEntity listEntity) {
        var knownStatusTypes = new java.util.HashMap<String, String>();
        if (listEntity != null && listEntity.getStatusesJson() != null && !listEntity.getStatusesJson().isBlank()) {
            try {
                var statuses = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(listEntity.getStatusesJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.example.demo.dto.ClickUpStatusDto>>() {});
                for (var status : statuses) {
                    if (status.status() != null && status.type() != null) {
                        knownStatusTypes.put(status.status().toLowerCase(java.util.Locale.ROOT).trim(), status.type().toLowerCase(java.util.Locale.ROOT));
                    }
                }
            } catch (Exception ignored) {
                // ignore malformed status payloads and fall back to task metadata
            }
        }
        if (task.getStatusName() != null && task.getStatusType() != null) {
            knownStatusTypes.put(task.getStatusName().toLowerCase(java.util.Locale.ROOT).trim(), task.getStatusType().toLowerCase(java.util.Locale.ROOT));
        }
        Long effectiveSprintStart = listEntity == null ? null : analyticsService.resolveEffectiveSprintStartDate(listEntity);
        return analyticsService.getComprehensiveAnalytics(task, events, knownStatusTypes, effectiveSprintStart);
    }

    /**
     * 2. Sprint (List) Bazlı Toplu Özet Analiz Endpoint'i
     */
    @GetMapping("/sprint/{listId}")
    public ResponseEntity<SprintAnalyticsResponse> getSprintAnalytics(@PathVariable String listId) {
        // 1. Olası gizli boşlukları ve kaçış karakterlerini temizle
        String cleanListId = (listId != null) ? listId.trim() : "";

        // 2. Konsola giren verinin uzunluğunu basarak görünmeyen karakter var mı kontrol et
        System.out.println(">>> Aranan Liste ID: '" + cleanListId + "' | Karakter Uzunluğu: " + cleanListId.length());

        // 3. Veritabanından aramayı temizlenmiş ID ile yap
        ListEntity listEntity = listRepository.findById(cleanListId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sprint / Liste veritabanında bulunamadı: '" + cleanListId + "' (Uzunluk: " + cleanListId.length() + ")"
                ));

        List<TaskEntity> tasks = taskRepository.findByListId(cleanListId);

        SprintAnalyticsResponse response = analyticsService.getSprintComprehensiveAnalytics(listEntity, tasks);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Kullanıcı (Assignee) Bazlı Gantt Chart ve Net Çalışma Süresi Endpoint'i
     * (Belirli bir sprint için ?listId=... gönderilebilir; gönderilmezse kullanıcının tüm görevlerini tarar)
     */
    @GetMapping("/user/{userId}/gantt")
    public ResponseEntity<UserGanttAnalyticsResponse> getUserGanttAnalytics(
            @PathVariable Long userId,
            @RequestParam(required = false) String listId) {

        List<TaskEntity> tasks = (listId != null && !listId.isBlank())
                ? taskRepository.findByListId(listId)
                : taskRepository.findAll();

        UserGanttAnalyticsResponse response = analyticsService.getUserGanttAnalytics(userId, listId, tasks);
        return ResponseEntity.ok(response);
    }
}