package br.com.fox.editflow.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    private String id;
    private String name;
    private String email;
    private String token;

    @SerializedName("credits")
    private Integer credits;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public Integer getCredits() { return credits; }
}
