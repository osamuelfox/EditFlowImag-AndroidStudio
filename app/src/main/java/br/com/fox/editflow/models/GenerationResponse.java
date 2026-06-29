package br.com.fox.editflow.models;

import com.google.gson.annotations.SerializedName;

/**
 * Mapeamento real da resposta do backend para /api/generations (POST e GET /{id}).
 *
 * JSON real retornado pelo backend (GenerationEntity serializado):
 * {
 *   "id": "43f63138-d87f-4c4e-ad59-47447fb570c2",
 *   "status": "PROCESSING" | "SUCCEEDED" | "FAILED",
 *   "mode": "PROMPT" | "GUIDED",
 *   "customPrompt": "...",
 *   "analysisText": null | "...",
 *   "changesText": null | "...",
 *   "descriptionText": null | "...",
 *   "errorMessage": null | "...",
 *   "createdAt": "2026-06-29T00:28:52.602037Z",
 *   "startedAt": "...",
 *   "finishedAt": null | "...",
 *   "resultImage": null | {
 *     "id": "uuid-da-imagem-resultado",
 *     "kind": "RESULT",
 *     "originalName": "edited-12345.png",
 *     "contentType": "image/png",
 *     "sizeBytes": 123456,
 *     "createdAt": "..."
 *   },
 *   "sourceImage": { ... },
 *   "user": { "id": "...", "name": "...", "email": "..." }
 * }
 */
public class GenerationResponse {

    @SerializedName("id")
    private String id;

    /**
     * Status da geração. Valores possíveis: "PROCESSING", "SUCCEEDED", "FAILED"
     */
    @SerializedName("status")
    private String status;

    @SerializedName("mode")
    private String mode;

    @SerializedName("customPrompt")
    private String customPrompt;

    @SerializedName("analysisText")
    private String analysisText;

    @SerializedName("changesText")
    private String changesText;

    @SerializedName("descriptionText")
    private String descriptionText;

    @SerializedName("errorMessage")
    private String errorMessage;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("finishedAt")
    private String finishedAt;

    /**
     * Imagem resultado — objeto aninhado disponível quando status = "SUCCEEDED".
     * Use resultImage.getId() para montar a URL de download:
     *   GET /api/images/{resultImage.id}/download
     */
    @SerializedName("resultImage")
    private ResultImageRef resultImage;

    // ── Getters ────────────────────────────────────────────────────────────

    public String getId()              { return id; }
    public String getStatus()          { return status; }
    public String getMode()            { return mode; }
    public String getCustomPrompt()    { return customPrompt; }
    public String getAnalysisText()    { return analysisText; }
    public String getChangesText()     { return changesText; }
    public String getDescriptionText() { return descriptionText; }
    public String getErrorMessage()    { return errorMessage; }
    public String getCreatedAt()       { return createdAt; }
    public String getFinishedAt()      { return finishedAt; }
    public ResultImageRef getResultImage() { return resultImage; }

    /**
     * Atalho para pegar o ID da imagem resultado sem verificar null manualmente.
     * Retorna null se a geração ainda não terminou.
     */
    public String getResultImageId() {
        return resultImage != null ? resultImage.getId() : null;
    }

    /** Retorna true quando a geração foi concluída com sucesso. */
    public boolean isSucceeded() {
        return "SUCCEEDED".equalsIgnoreCase(status);
    }

    /** Retorna true quando a geração falhou no servidor. */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }

    /** Retorna true enquanto a geração ainda está em andamento. */
    public boolean isProcessing() {
        return "PROCESSING".equalsIgnoreCase(status);
    }

    // ── Classe aninhada: referência da imagem resultado ───────────────────

    /**
     * Representa o objeto "resultImage" dentro da resposta de geração.
     * Mapeado do campo resultImage do GenerationEntity no backend.
     */
    public static class ResultImageRef {

        @SerializedName("id")
        private String id;

        @SerializedName("kind")
        private String kind;

        @SerializedName("originalName")
        private String originalName;

        @SerializedName("contentType")
        private String contentType;

        @SerializedName("sizeBytes")
        private long sizeBytes;

        @SerializedName("createdAt")
        private String createdAt;

        public String getId()          { return id; }
        public String getKind()        { return kind; }
        public String getOriginalName(){ return originalName; }
        public String getContentType() { return contentType; }
        public long getSizeBytes()     { return sizeBytes; }
        public String getCreatedAt()   { return createdAt; }
    }
}
