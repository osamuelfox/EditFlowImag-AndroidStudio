package br.com.fox.editflow.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    @SerializedName("credits")
    private int credits;

    @SerializedName("initial_credits")
    private int initialCredits;

    @SerializedName("plan")
    private String plan;

    public RegisterRequest(String name, String email, String password, int credits) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.credits = credits;
        this.initialCredits = credits;
        this.plan = "TRIAL";
    }
}
