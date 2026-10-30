package com.editflowimg.backend.controller;

import com.editflowimg.backend.dto.generation.GenerationRequest;
import com.editflowimg.backend.entity.GenerationEntity;
import com.editflowimg.backend.entity.UserEntity;
import com.editflowimg.backend.service.GenerationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    public GenerationEntity create(@RequestBody GenerationRequest request,
                                   @AuthenticationPrincipal UserEntity user) {
        return generationService.createGeneration(request, user);
    }

    @GetMapping("/{id}")
    public GenerationEntity findById(@PathVariable UUID id,
                                     @AuthenticationPrincipal UserEntity user) {
        return generationService.findById(id, user);
    }

    @GetMapping
    public List<GenerationEntity> listByUser(@AuthenticationPrincipal UserEntity user) {
        return generationService.listByUser(user);
    }
}
