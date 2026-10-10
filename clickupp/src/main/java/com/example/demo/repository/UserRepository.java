package com.example.demo.repository;

import com.example.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Veritabanında OAuth token'ı bulunan tüm kullanıcıları getirir
    List<UserEntity> findByAccessTokenIsNotNull();
}