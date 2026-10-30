package com.editflowimg.backend.repository;

import com.editflowimg.backend.entity.GenerationEntity;
import com.editflowimg.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GenerationRepository extends JpaRepository<GenerationEntity, UUID> {
    List<GenerationEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
}
