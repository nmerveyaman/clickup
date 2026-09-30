package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "workspace_members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_key")
    private String roleKey;

    @Column(name = "invited_by_user_id")
    private Long invitedByUserId;

    @Column(name = "date_joined")
    private String dateJoined;

    @Column(name = "date_invited")
    private String dateInvited;
}