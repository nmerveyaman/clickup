package com.example.demo.repository;

import com.example.demo.entity.TaskEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskEventRepository extends JpaRepository<TaskEventEntity, String> {


    List<TaskEventEntity> findAllByTaskIdOrderByTimestampDesc(String taskId);

    List<TaskEventEntity> findAllByTaskIdAndEventType(String taskId, String eventType);
}