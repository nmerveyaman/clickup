package com.example.demo.repository;
import com.example.demo.entity.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, String> {
    List<SpaceEntity> findByWorkspaceId(String workspaceId);
} // dsl daha iyi sorgu