package com.example.demo.repository;
import java.util.List;
import com.example.demo.entity.ListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListRepository extends JpaRepository<ListEntity, String> {
    List<ListEntity> findByFolderId(String folderId);
    List<ListEntity> findBySpaceIdAndFolderIdIsNull(String spaceId);
    @Query("SELECT l FROM ListEntity l WHERE l.spaceId IN (SELECT s.id FROM SpaceEntity s WHERE s.workspace.id = :workspaceId)")
    List<ListEntity> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);
}
