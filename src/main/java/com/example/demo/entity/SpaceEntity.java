package com.example.demo.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpaceEntity {

    @Id
    @Column(name = "id")
    private String id; //uuıd

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Column(name = "name")
    private String name;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "is_archived")
    private Boolean isArchived;
}