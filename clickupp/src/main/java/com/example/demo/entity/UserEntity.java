package com.example.demo.entity;

import com.example.demo.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "profile_picture")
    private String profilePicture;

    // --- YENİ EKLENEN OAUTH VE ROL ALANLARI ---

    @Column(name = "access_token")
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_role")
    private Role appRole;
}