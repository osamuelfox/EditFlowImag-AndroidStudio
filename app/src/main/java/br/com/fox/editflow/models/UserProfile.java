package br.com.fox.editflow.models;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    private String name;

    @SerializedName("credits")
    private int credits;

    @SerializedName("credit_balance")
    private Integer creditBalance;

    @SerializedName("balance")
    private Integer balance;

    @SerializedName("available_credits")
    private Integer availableCredits;

    @SerializedName("tokens")
    private Integer tokens;

    @SerializedName("user_credits")
    private Integer userCredits;

    @SerializedName("remaining_credits")
    private Integer remainingCredits;

    public UserProfile(String name, int credits) {
        this.name = name;
        this.credits = credits;
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        if (creditBalance != null) return creditBalance;
        if (balance != null) return balance;
        if (availableCredits != null) return availableCredits;
        if (tokens != null) return tokens;
        if (userCredits != null) return userCredits;
        if (remainingCredits != null) return remainingCredits;
        return credits;
    }
}
