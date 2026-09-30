package com.example.demo.repository;
import com.example.demo.entity.WorkspaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMemberEntity, Long> {
    List<WorkspaceMemberEntity> findByWorkspaceId(String workspaceId);
}