package com.editflowimg.backend.entity;

import com.editflowimg.backend.enums.GenerationMode;
import com.editflowimg.backend.enums.GenerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "generations")
public class GenerationEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_image_id")
    private ImageEntity sourceImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationMode mode;

    private String style;
    private String purpose;

    @Column(name = "background_action")
    private String backgroundAction;

    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    private String customPrompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status;

    @ManyToOne
    @JoinColumn(name = "result_image_id")
    private ImageEntity resultImage;

    @Column(name = "analysis_text", columnDefinition = "TEXT")
    private String analysisText;

    @Column(name = "changes_text", columnDefinition = "TEXT")
    private String changesText;

    @Column(name = "description_text", columnDefinition = "TEXT")
    private String descriptionText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private Instant startedAt;
    private Instant finishedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
