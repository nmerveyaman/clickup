package com.example.demo.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name="workspaces")
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor


public class WorkspaceEntity {
    @Id
    @Column(name="id", nullable = false,updatable=false)
    private String id;


    @Column(name = "name", nullable = false)
    private String name;




}
