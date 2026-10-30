package com.editflowimg.backend.service;

import com.editflowimg.backend.dto.generation.AiResult;
import com.editflowimg.backend.entity.GenerationEntity;
import com.editflowimg.backend.entity.ImageEntity;
import com.editflowimg.backend.enums.GenerationMode;
import com.editflowimg.backend.enums.GenerationStatus;
import com.editflowimg.backend.enums.ImageKind;
import com.editflowimg.backend.repository.GenerationRepository;
import com.editflowimg.backend.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Component
public class GenerationProcessor {

    private static final Logger log = LoggerFactory.getLogger(GenerationProcessor.class);

    private final GenerationRepository generationRepository;
    private final ImageRepository imageRepository;
    private final AiService aiService;
    private final Path uploadDir = Paths.get("uploads");

    public GenerationProcessor(GenerationRepository generationRepository,
                               ImageRepository imageRepository,
                               AiService aiService) {
        this.generationRepository = generationRepository;
        this.imageRepository = imageRepository;
        this.aiService = aiService;
    }

    @Async("generationExecutor")
    @Transactional
    public void process(UUID generationId) {
        log.info("Iniciando processamento da geração {}", generationId);
        GenerationEntity generation = null;
        try {
            generation = generationRepository.findById(generationId).orElse(null);

            if (generation == null) {
                log.warn("Geração {} não encontrada no banco", generationId);
                return;
            }

            File originalFile = new File(generation.getSourceImage().storageKeyValue());

            if (!originalFile.exists()) {
                markAsFailed(generation, "Arquivo original da imagem não encontrado: " + originalFile.getAbsolutePath());
                return;
            }

            String prompt = buildPrompt(generation);
            log.info("Chamando Hugging Face para geração {} com prompt: {}", generationId, prompt);

            byte[] editedImageBytes = aiService.editImage(originalFile, prompt);
            log.info("Resposta do Hugging Face recebida: {} bytes", editedImageBytes.length);

            String resultFileName = "edited-" + System.currentTimeMillis() + ".png";
            Path resultPath = uploadDir.resolve(resultFileName);
            Files.write(resultPath, editedImageBytes);

            ImageEntity resultImage = new ImageEntity();
            resultImage.setUser(generation.getUser());
            resultImage.setKind(ImageKind.RESULT);
            resultImage.setOriginalName(resultFileName);
            resultImage.setContentType("image/png");
            resultImage.setSizeBytes((long) editedImageBytes.length);
            resultImage.setStorageKey(resultPath.toString());
            resultImage = imageRepository.save(resultImage);

            AiResult analysis = aiService.generateAnalysis(prompt);

            generation.setResultImage(resultImage);
            generation.setAnalysisText(String.join("\n", analysis.getAnalysisItems()));
            generation.setChangesText(String.join("\n", analysis.getChangeItems()));
            generation.setDescriptionText(analysis.getDescriptionText());
            generation.setStatus(GenerationStatus.SUCCEEDED);
            generation.setFinishedAt(Instant.now());
            generationRepository.save(generation);
            log.info("Geração {} concluída com sucesso", generationId);

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName() + ": (sem mensagem)";

            log.error("Erro ao processar geração {}: {}", generationId, errorMsg, e);
            if (generation != null) {
                markAsFailed(generation, errorMsg);
            }
        }
    }

    private void markAsFailed(GenerationEntity generation, String errorMessage) {
        generation.setStatus(GenerationStatus.FAILED);
        generation.setErrorMessage(errorMessage);
        generation.setFinishedAt(Instant.now());
        generationRepository.save(generation);
    }

    private String buildPrompt(GenerationEntity generation) {
        if (generation.getMode() == GenerationMode.GUIDED) {
            return "Edite esta imagem mantendo a foto original como base. "
                    + "Aplique um estilo " + generation.getStyle() + ". "
                    + "A finalidade da imagem é " + generation.getPurpose() + ". "
                    + "Altere o fundo para " + generation.getBackgroundAction() + ". "
                    + "Melhore iluminação, nitidez, enquadramento e aparência profissional.";
        }

        return generation.getCustomPrompt();
    }
}
