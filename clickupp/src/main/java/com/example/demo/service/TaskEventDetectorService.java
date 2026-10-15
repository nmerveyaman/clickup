package com.example.demo.service;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.dto.TaskEventNotification;
import com.example.demo.event.TaskChangedEvent;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.TaskEventEntity;
import com.example.demo.enums.TaskEventType;
import com.example.demo.repository.TaskEventRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskEventDetectorService {

    private final TaskEventRepository taskEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void detectAndRecordEvents(TaskEntity dbTask, ClickUpTaskDto apiTask) {
        String eventTime = apiTask.dateUpdated() != null
                ? apiTask.dateUpdated()
                : String.valueOf(System.currentTimeMillis());

        Long oldAssigneeId = extractDbFirstAssigneeId(dbTask);
        Long newAssigneeId = extractApiFirstAssigneeId(apiTask);

        // 1. ASSIGNEE (KİŞİ) DEĞİŞİMİ KONTROLÜ
        if (!Objects.equals(oldAssigneeId, newAssigneeId)) {
            saveEvent(
                    dbTask,
                    apiTask,
                    TaskEventType.ASSIGNEE_CHANGED,
                    oldAssigneeId,
                    newAssigneeId,
                    dbTask.getStatusName(),
                    dbTask.getStatusName(),
                    eventTime
            );
        }

        String newPriority = apiTask.priority() != null ? apiTask.priority().priority() : null;
        if (!Objects.equals(dbTask.getPriority(), newPriority)) {
            saveEvent(
                    dbTask,
                    apiTask,
                    TaskEventType.PRIORITY_CHANGED,
                    oldAssigneeId,
                    newAssigneeId,
                    null,
                    null,
                    eventTime,
                    dbTask.getPriority(),
                    newPriority
            );
        }

        // 2. STATÜ DEĞİŞİMİ KONTROLÜ (PROGRESSED / ROLLBACK / COMPLETED)
        checkStatusChange(dbTask, apiTask, newAssigneeId, eventTime);

        // 3. LİSTE DEĞİŞİMİ KONTROLÜ (MOVED_TO_LIST)
        String newListId = apiTask.list() != null ? apiTask.list().id() : null;
        if (dbTask.getListId() != null && newListId != null && !Objects.equals(dbTask.getListId(), newListId)) {
            saveEvent(
                    dbTask,
                    apiTask,
                    TaskEventType.MOVED_TO_LIST,
                    oldAssigneeId,
                    newAssigneeId,
                    dbTask.getListId(),
                    newListId,
                    eventTime
            );
        }
    }

    private void checkStatusChange(TaskEntity dbTask, ClickUpTaskDto apiTask, Long currentAssigneeId, String timestamp) {
        if (apiTask.status() == null) return;

        String oldStatus = dbTask.getStatusName();
        String newStatus = apiTask.status().status();
        String newStatusType = apiTask.status().type();

        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }

        int oldOrder = parseOrder(dbTask.getOrderIndex());
        int newOrder = apiTask.status().orderIndex() != null ? apiTask.status().orderIndex() : oldOrder;

        TaskEventType eventType;
        if ("closed".equalsIgnoreCase(newStatusType) || "done".equalsIgnoreCase(newStatusType)) {
            eventType = TaskEventType.TASK_COMPLETED;
        } else if (newOrder < oldOrder) {
            eventType = TaskEventType.STATUS_ROLLBACK;
        } else {
            eventType = TaskEventType.STATUS_PROGRESSED;
        }

        saveEvent(
                dbTask,
                apiTask,
                eventType,
                currentAssigneeId,
                currentAssigneeId,
                oldStatus,
                newStatus,
                timestamp
        );
    }

    private void saveEvent(TaskEntity dbTask, ClickUpTaskDto apiTask, TaskEventType type, Long fromAssignee, Long toAssignee,
                           String fromStatus, String toStatus, String timestamp) {
        saveEvent(dbTask, apiTask, type, fromAssignee, toAssignee, fromStatus, toStatus, timestamp, null, null);
    }

    private void saveEvent(TaskEntity dbTask, ClickUpTaskDto apiTask, TaskEventType type, Long fromAssignee, Long toAssignee,
                           String fromStatus, String toStatus, String timestamp, String fromPriority, String toPriority) {
        TaskEventEntity event = TaskEventEntity.builder()
                .taskId(dbTask.getId())
                .eventType(type)
                .fromAssignee(fromAssignee)
                .toAssignee(toAssignee)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromPriority(fromPriority)
                .toPriority(toPriority)
                .timestamp(timestamp)
                .build();

        TaskEventEntity savedEvent = taskEventRepository.save(event);
        String listId = apiTask.list() != null ? apiTask.list().id() : dbTask.getListId();
        eventPublisher.publishEvent(new TaskChangedEvent(new TaskEventNotification(
                savedEvent.getId(),
                dbTask.getWorkspaceId(),
                listId,
                dbTask.getId(),
                apiTask.name(),
                type.name(),
                fromStatus,
                toStatus,
                timestamp,
                fromPriority,
                toPriority
        )));
    }

    private Long extractDbFirstAssigneeId(TaskEntity dbTask) {
        if (dbTask.getAssignees() == null || dbTask.getAssignees().isEmpty()) {
            return null;
        }
        return dbTask.getAssignees().get(0).getId();
    }

    private Long extractApiFirstAssigneeId(ClickUpTaskDto apiTask) {
        if (apiTask.assignees() == null || apiTask.assignees().isEmpty()) {
            return null;
        }
        return apiTask.assignees().get(0).id();
    }

    private int parseOrder(String orderStr) {
        try {
            return orderStr != null ? Integer.parseInt(orderStr) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}