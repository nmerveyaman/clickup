package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "spaces")
@Getter
@Setter
public class SpaceEntity {

    @Id
    @Column(name = "id")
    private String id; //uuıd

    @Column(name = "name")
    private String name;

    @Column(name = "color")
    private String color;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "is_archived")
    private Boolean isArchived;
}