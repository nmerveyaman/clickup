package com.example.demo.repository;
import com.example.demo.entity.WorkspaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMemberEntity, Long> {
    Optional<WorkspaceMemberEntity> findByWorkspaceIdAndUserId(String workspaceId, Long userId);}