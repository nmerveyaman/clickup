package com.example.demo.service;

import com.example.demo.dto.TaskEventNotification;
import com.example.demo.event.TaskChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TaskEventStreamService {

    private final Map<String, Map<String, SseEmitter>> emittersByWorkspace = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String workspaceId) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        Map<String, SseEmitter> workspaceEmitters = emittersByWorkspace.computeIfAbsent(
                workspaceId,
                ignored -> new ConcurrentHashMap<>()
        );
        workspaceEmitters.put(connectionId, emitter);

        Runnable removeEmitter = () -> removeEmitter(workspaceId, connectionId);
        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(error -> removeEmitter.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("workspaceId", workspaceId)));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskChanged(TaskChangedEvent event) {
        TaskEventNotification notification = event.notification();
        if (notification.workspaceId() == null) {
            return;
        }
        Map<String, SseEmitter> workspaceEmitters = emittersByWorkspace.get(notification.workspaceId());
        if (workspaceEmitters == null) {
            return;
        }

        workspaceEmitters.forEach((connectionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("task-event").data(notification));
            } catch (IOException | IllegalStateException exception) {
                log.debug("Removing disconnected task event stream {}", connectionId, exception);
                removeEmitter(notification.workspaceId(), connectionId);
            }
        });
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeats() {
        emittersByWorkspace.forEach((workspaceId, workspaceEmitters) ->
                workspaceEmitters.forEach((connectionId, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event().comment("keepalive"));
                    } catch (IOException | IllegalStateException exception) {
                        log.debug("Removing disconnected task event stream {}", connectionId, exception);
                        removeEmitter(workspaceId, connectionId);
                    }
                })
        );
    }

    private void removeEmitter(String workspaceId, String connectionId) {
        Map<String, SseEmitter> workspaceEmitters = emittersByWorkspace.get(workspaceId);
        if (workspaceEmitters == null) {
            return;
        }
        workspaceEmitters.remove(connectionId);
        if (workspaceEmitters.isEmpty()) {
            emittersByWorkspace.remove(workspaceId, workspaceEmitters);
        }
    }
}
