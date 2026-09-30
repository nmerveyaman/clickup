package com.example.demo.entity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "status_type")
    private String statusType;

    @Column(name="order_index")
    private String orderIndex;

    @Column(name = "date_created")
    private String dateCreated;

    @Column(name = "date_updated")
    private String dateUpdated;

    @Column(name = "date_closed")
    private String dateClosed;

    @Column (name = "date_done")
    private String dateDone;

    @Column(name="creator_id")
    private String creatorId;

    @Column(name="list_id")
    private String listId;

    @ManyToMany
    @JoinTable(
            name= "task_assignees",
            joinColumns = @JoinColumn(name="task_id"),
            inverseJoinColumns = @JoinColumn(name="user_id"))
    private List<UserEntity> assignees;



}
