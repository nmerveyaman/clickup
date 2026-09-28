package com.example.demo.repository;

import com.example.demo.entity.SpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceRepository extends JpaRepository<SpaceEntity, String> {
} //query dsl daha iyi sorgu