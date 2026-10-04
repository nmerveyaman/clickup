package com.example.demo.repository;
import com.example.demo.entity.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, String> {
    @Modifying
    @Query(value = "INSERT INTO workspaces (id, name) VALUES (:id, :name) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name", nativeQuery = true)
    void upsertWorkspace(@Param("id") String id, @Param("name") String name);
}
