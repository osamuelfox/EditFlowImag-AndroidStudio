package br.com.fox.editflow.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "EditFlowPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_CREDITS_PREFIX = "credits_";

    private SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Salva o token, o email e o nome do usuário logado.
     */
    public void saveAuth(String token, String email, String name) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "usuário");
    }

    /**
     * Adiciona uma quantidade de créditos para o usuário especificado.
     */
    public void addCredits(String email, int amount) {
        if (email == null || email.isEmpty()) return;
        int current = getCreditsByEmail(email);
        prefs.edit().putInt(KEY_CREDITS_PREFIX + email, current + amount).apply();
    }

    /**
     * Retorna o saldo disponível do usuário logado atualmente.
     */
    public int getAvailableCredits() {
        String email = getCurrentEmail();
        if (email == null) return 0;
        return getCreditsByEmail(email);
    }

    /**
     * Consome um crédito do usuário logado atualmente.
     */
    public void consumeCredit() {
        String email = getCurrentEmail();
        if (email == null) return;

        int current = getCreditsByEmail(email);
        if (current > 0) {
            prefs.edit().putInt(KEY_CREDITS_PREFIX + email, current - 1).apply();
        }
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getCurrentEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    private int getCreditsByEmail(String email) {
        return prefs.getInt(KEY_CREDITS_PREFIX + email, 0);
    }

    /**
     * Limpa os dados da sessão (token e email), mas preserva os saldos de créditos.
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .apply();
    }
}
