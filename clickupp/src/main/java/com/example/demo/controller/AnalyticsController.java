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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 2. Sprint (List) Bazlı Toplu Özet Analiz Endpoint'i
     */
    @GetMapping("/sprint/{listId}")
    public ResponseEntity<SprintAnalyticsResponse> getSprintAnalytics(@PathVariable String listId) {
        ListEntity listEntity = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Sprint / Liste bulunamadı: " + listId));

        List<TaskEntity> tasks = taskRepository.findByListId(listId);

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