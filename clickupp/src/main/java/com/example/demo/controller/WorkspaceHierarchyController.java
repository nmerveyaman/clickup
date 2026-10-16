package com.example.demo.controller;

import com.example.demo.dto.ClickUpListResponseDto;
import com.example.demo.dto.WorkspaceOptionDto;
import com.example.demo.service.WorkspaceHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceHierarchyController {

    private final WorkspaceHierarchyService hierarchyService;

    @GetMapping("/available")
    public ResponseEntity<List<WorkspaceOptionDto>> getAvailableWorkspaces(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(hierarchyService.getWorkspacesForUser(userId));
    }

    @GetMapping("/lists")
    public ResponseEntity<ClickUpListResponseDto> getLists(@RequestParam("workspaceId") String workspaceId) {
        ClickUpListResponseDto response = hierarchyService.getListsByWorkspaceId(workspaceId);
        return ResponseEntity.ok(response);
    }
}