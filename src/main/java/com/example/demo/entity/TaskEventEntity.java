package com.example.demo.entity;
import com.example.demo.enums.TaskEventType;

import jakarta.persistence.*;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Entity
@Table(name = "task_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;


    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private TaskEventType eventType;


    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;


    @Column(name = "from_assignee")
    private Long  fromAssignee;


    @Column(name = "to_assignee")
    private Long toAssignee;


    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "timestamp")
    private String timestamp;



}