package com.example.demo.service;

import com.example.demo.dto.ClickUpTaskDto;
import com.example.demo.dto.TaskEventNotification;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.TaskEventEntity;
import com.example.demo.enums.TaskEventType;
import com.example.demo.event.TaskChangedEvent;
import com.example.demo.repository.TaskEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskEventDetectorServiceTest {

    private final TaskEventRepository taskEventRepository = mock(TaskEventRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final TaskEventDetectorService detector =
            new TaskEventDetectorService(taskEventRepository, eventPublisher);

    @Test
    void recordsAndPublishesPriorityChanges() {
        TaskEntity dbTask = new TaskEntity();
        dbTask.setId("task-1");
        dbTask.setWorkspaceId("workspace-1");
        dbTask.setListId("list-1");
        dbTask.setName("Test task");
        dbTask.setStatusName("In Progress");
        dbTask.setPriority("low");
        dbTask.setOrderIndex("1");

        ClickUpTaskDto apiTask = new ClickUpTaskDto(
                "task-1",
                "Test task",
                "1",
                null,
                "123456",
                null,
                null,
                new ClickUpTaskDto.StatusDto("In Progress", "custom", 1),
                null,
                new ClickUpTaskDto.ListObjDto("list-1"),
                null,
                new ClickUpTaskDto.PriorityDto("high"),
                null
        );
        when(taskEventRepository.save(any(TaskEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        detector.detectAndRecordEvents(dbTask, apiTask);

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(TaskEventEntity.class);
        verify(taskEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(TaskEventType.PRIORITY_CHANGED);
        assertThat(eventCaptor.getValue().getFromPriority()).isEqualTo("low");
        assertThat(eventCaptor.getValue().getToPriority()).isEqualTo("high");

        var publishedEventCaptor = org.mockito.ArgumentCaptor.forClass(TaskChangedEvent.class);
        verify(eventPublisher).publishEvent(publishedEventCaptor.capture());
        TaskEventNotification notification = publishedEventCaptor.getValue().notification();
        assertThat(notification.eventType()).isEqualTo("PRIORITY_CHANGED");
        assertThat(notification.fromPriority()).isEqualTo("low");
        assertThat(notification.toPriority()).isEqualTo("high");
        assertThat(notification.fromStatus()).isNull();
        assertThat(notification.toStatus()).isNull();
    }
}
