package com.editflowimg.backend.dto.generation;

import com.editflowimg.backend.enums.GenerationMode;
import java.util.UUID;

public class GenerationRequest {
    private UUID imageId;
    private GenerationMode mode;
    private String style;
    private String purpose;
    private String backgroundAction;
    private String customPrompt;

    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }

    public GenerationMode getMode() { return mode; }
    public void setMode(GenerationMode mode) { this.mode = mode; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getBackgroundAction() { return backgroundAction; }
    public void setBackgroundAction(String backgroundAction) { this.backgroundAction = backgroundAction; }

    public String getCustomPrompt() { return customPrompt; }
    public void setCustomPrompt(String customPrompt) { this.customPrompt = customPrompt; }
}