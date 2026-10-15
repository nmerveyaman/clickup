package com.example.demo.repository;

import com.example.demo.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {

    List<TaskEntity> findByListId(String listId);

    /**
     * Sprint başlangıç tarihini dinamik olarak hesaplar:
     * 1. ÖNCELİK: Event tablosundaki en erken geçiş zamanı (timestamp)
     * 2. ÖNCELİK (FALLBACK): Event yoksa, tasks tablosundaki en erken oluşturulma tarihi (date_created)
     */
    @Query(value = """
            SELECT COALESCE(
                (
                    SELECT MIN(CAST(te.timestamp AS BIGINT))
                    FROM tasks t
                    JOIN task_events te ON t.id = te.task_id
                    WHERE t.list_id = :listId
                ),
                (
                    SELECT MIN(CAST(t.date_created AS BIGINT))
                    FROM tasks t
                    WHERE t.list_id = :listId
                )
            )
            """, nativeQuery = true)
    Optional<Long> findEffectiveSprintStartDate(@Param("listId") String listId);
}