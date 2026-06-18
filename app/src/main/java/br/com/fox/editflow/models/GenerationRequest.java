package br.com.fox.editflow.models;

public class GenerationRequest {
    private String imageId;
    private String mode;
    private String customPrompt;

    public GenerationRequest(String imageId, String mode, String customPrompt) {
        this.imageId = imageId;
        this.mode = mode;
        this.customPrompt = customPrompt;
    }
}
