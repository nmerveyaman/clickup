package com.example.demo.controller;

import com.example.demo.dto.ClickUpListResponseDto;
import com.example.demo.service.WorkspaceHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceHierarchyController {

    private final WorkspaceHierarchyService hierarchyService;

    @GetMapping("/lists")
    public ResponseEntity<ClickUpListResponseDto> getLists(@RequestParam("workspaceId") String workspaceId) {
        ClickUpListResponseDto response = hierarchyService.getListsByWorkspaceId(workspaceId);
        return ResponseEntity.ok(response);
    }
}