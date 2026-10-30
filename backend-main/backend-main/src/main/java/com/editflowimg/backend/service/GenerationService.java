package com.editflowimg.backend.service;

import com.editflowimg.backend.dto.generation.GenerationRequest;
import com.editflowimg.backend.entity.GenerationEntity;
import com.editflowimg.backend.entity.ImageEntity;
import com.editflowimg.backend.entity.UserEntity;
import com.editflowimg.backend.enums.GenerationMode;
import com.editflowimg.backend.enums.GenerationStatus;
import com.editflowimg.backend.exception.AppException;
import com.editflowimg.backend.repository.GenerationRepository;
import com.editflowimg.backend.repository.ImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GenerationService {

    private final GenerationRepository generationRepository;
    private final ImageRepository imageRepository;
    private final GenerationProcessor generationProcessor;

    public GenerationService(GenerationRepository generationRepository,
                             ImageRepository imageRepository,
                             GenerationProcessor generationProcessor) {
        this.generationRepository = generationRepository;
        this.imageRepository = imageRepository;
        this.generationProcessor = generationProcessor;
    }

    @Transactional
    public GenerationEntity createGeneration(GenerationRequest request, UserEntity user) {
        ImageEntity image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new AppException("Imagem não encontrada", HttpStatus.NOT_FOUND));

        if (!image.getUser().getId().equals(user.getId())) {
            throw new AppException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        validateRequest(request);

        GenerationEntity generation = new GenerationEntity();
        generation.setUser(user);
        generation.setSourceImage(image);
        generation.setMode(request.getMode());
        generation.setStyle(request.getStyle());
        generation.setPurpose(request.getPurpose());
        generation.setBackgroundAction(request.getBackgroundAction());
        generation.setCustomPrompt(request.getCustomPrompt());
        generation.setStatus(GenerationStatus.PROCESSING);
        generation.setStartedAt(Instant.now());

        GenerationEntity saved = generationRepository.save(generation);
        UUID generationId = saved.getId();

        // Dispara o processamento somente após a transação ser commitada,
        // garantindo que a entidade já está no banco quando o processor buscar.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generationProcessor.process(generationId);
            }
        });

        return saved;
    }

    public GenerationEntity findById(UUID id, UserEntity user) {
        GenerationEntity generation = generationRepository.findById(id)
                .orElseThrow(() -> new AppException("Geração não encontrada", HttpStatus.NOT_FOUND));

        if (!generation.getUser().getId().equals(user.getId())) {
            throw new AppException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        return generation;
    }

    public List<GenerationEntity> listByUser(UserEntity user) {
        return generationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    private void validateRequest(GenerationRequest request) {
        if (request.getMode() == null) {
            throw new AppException("O modo de geração é obrigatório", HttpStatus.BAD_REQUEST);
        }

        if (request.getImageId() == null) {
            throw new AppException("A imagem é obrigatória", HttpStatus.BAD_REQUEST);
        }

        if (request.getMode() == GenerationMode.GUIDED) {
            if (isBlank(request.getStyle())
                    || isBlank(request.getPurpose())
                    || isBlank(request.getBackgroundAction())) {
                throw new AppException(
                        "No modo GUIDED, estilo, finalidade e ação do fundo são obrigatórios",
                        HttpStatus.BAD_REQUEST);
            }
        }

        if (request.getMode() == GenerationMode.PROMPT) {
            if (isBlank(request.getCustomPrompt())) {
                throw new AppException(
                        "No modo PROMPT, o prompt personalizado é obrigatório",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
