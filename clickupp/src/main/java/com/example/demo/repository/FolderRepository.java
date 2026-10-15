package com.example.demo.repository;
import com.example.demo.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, String> {
    List<FolderEntity> findBySpaceId(String spaceId);
}
