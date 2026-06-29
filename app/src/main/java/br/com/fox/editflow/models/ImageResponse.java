package br.com.fox.editflow.models;

import com.google.gson.annotations.SerializedName;

/**
 * Mapeamento real da resposta do backend para /api/images (POST upload).
 *
 * JSON real retornado pelo backend:
 * {
 *   "id": "26520895-b17c-4386-ac3d-d8405a946636",
 *   "kind": "ORIGINAL",
 *   "originalName": "foto.jpg",
 *   "contentType": "image/jpeg",
 *   "sizeBytes": 8625,
 *   "createdAt": "2026-06-29T00:28:39.203283Z",
 *   "user": { "id": "...", "name": "...", "email": "..." }
 * }
 */
public class ImageResponse {

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
