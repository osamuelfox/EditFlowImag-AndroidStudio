package br.com.fox.editflow.models;

public class GenerationResponse {
    private String id;
    private String status; // e.g., "PENDING", "COMPLETED"
    private String resultImageId; // Assuming there's a field for the generated image id
    private String imageUrl; // Or a direct URL

    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getResultImageId() { return resultImageId; }
    public String getImageUrl() { return imageUrl; }
}
