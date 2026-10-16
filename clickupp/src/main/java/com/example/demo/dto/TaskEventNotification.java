package com.example.demo.dto;

public record TaskEventNotification(
        Long eventId,
        String workspaceId,
        String listId,
        String taskId,
        String taskName,
        String eventType,
        String fromStatus,
        String toStatus,
        String timestamp,
        String fromPriority,
        String toPriority
) {
}
