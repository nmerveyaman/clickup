package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "task_events")
@Getter
@Setter
@AllArgsConstructor
public class TaskEventEntity {


    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;


    @Column(name = "task_id", nullable = false)
    private String taskId;


    @Column(name = "event_type", nullable = false)
    private String eventType;


    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;


    @Column(name = "assignee_id")
    private Long assigneeId;


    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "timestamp")
    private String timestamp;

    public TaskEventEntity() {
        this.id = UUID.randomUUID().toString();
    }
}