package com.example.demo.service;

import com.example.demo.dto.ClickUpListDto;
import com.example.demo.dto.ClickUpListResponseDto;
import com.example.demo.dto.WorkspaceOptionDto;
import com.example.demo.entity.ListEntity;
import com.example.demo.entity.WorkspaceEntity;
import com.example.demo.repository.ListRepository;
import com.example.demo.repository.WorkspaceMemberRepository;
import com.example.demo.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceHierarchyService {

    private final ListRepository listRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceOptionDto> getWorkspacesForUser(Long userId) {
        List<String> workspaceIds = workspaceMemberRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(member -> member.getWorkspaceId())
                .distinct()
                .toList();
        Map<String, WorkspaceEntity> workspacesById = workspaceRepository.findAllById(workspaceIds).stream()
                .collect(Collectors.toMap(WorkspaceEntity::getId, Function.identity()));

        return workspaceIds.stream()
                .map(workspacesById::get)
                .filter(workspace -> workspace != null)
                .map(workspace -> new WorkspaceOptionDto(workspace.getId(), workspace.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClickUpListResponseDto getListsByWorkspaceId(String workspaceId) {
        // Veritabanından listeleri çekiyoruz
        List<ListEntity> listEntities = listRepository.findAllByWorkspaceId(workspaceId);

        // Veritabanında henüz kayıt yoksa NullPointerException / 500 fırlatmasını önlemek için boş DTO dönüyoruz
        if (listEntities == null || listEntities.isEmpty()) {
            return new ClickUpListResponseDto(Collections.emptyList());
        }

        List<ClickUpListDto> dtoList = listEntities.stream()
                .map(entity -> new ClickUpListDto(
                        entity.getId(),          // String (List ID)
                        entity.getName(),        // String (List/Sprint Adı)
                        entity.getTaskCount(),
                        entity.getStartDate(),   // 4. Long (Start Date)
                        entity.getDueDate(),// Integer (Görev Sayısı)
                        null

                ))
                .toList();

        return new ClickUpListResponseDto(dtoList);
    }
}