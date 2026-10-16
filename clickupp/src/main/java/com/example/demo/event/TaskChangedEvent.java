package com.example.demo.event;

import com.example.demo.dto.TaskEventNotification;

public record TaskChangedEvent(TaskEventNotification notification) {
}
