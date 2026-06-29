package br.com.fox.editflow.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import br.com.fox.editflow.api.ApiService;
import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.models.AuthResponse;
import br.com.fox.editflow.models.LoginRequest;
import br.com.fox.editflow.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends ViewModel {

    private static final long TIMEOUT_MS = 30_000L;

    public final MutableLiveData<UiState> loginState = new MutableLiveData<>(new UiState.Idle());

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timeoutFuture;
    private Call<AuthResponse> pendingCall;

    public void login(Context context, String email, String password, String loadingMessage) {
        loginState.setValue(new UiState.Loading(loadingMessage));

        // Timeout de 30s
        scheduleTimeout(context);

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        pendingCall = api.login(new LoginRequest(email, password));
        pendingCall.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                cancelTimeout();
                if (!(loginState.getValue() instanceof UiState.Loading)) return;

                if (response.isSuccessful() && response.body() != null) {
                    TokenManager tm = new TokenManager(context);
                    String email = response.body().getEmail();
                    String name = response.body().getName();
                    tm.saveAuth(response.body().getToken(), email, name);
                    tm.addCredits(email, 2); // Adiciona 2 créditos a cada login
                    loginState.postValue(new UiState.Success<>(response.body()));
                } else if (response.code() == 401 || response.code() == 403) {
                    loginState.postValue(new UiState.Error(null, false)); // erro inline
                } else {
                    loginState.postValue(new UiState.Error(String.valueOf(response.code()), true));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                cancelTimeout();
                if (!(loginState.getValue() instanceof UiState.Loading)) return;
                if (!call.isCanceled()) {
                    loginState.postValue(new UiState.Error("connection", true));
                }
            }
        });
    }

    public void cancelLogin() {
        cancelTimeout();
        if (pendingCall != null) pendingCall.cancel();
        loginState.setValue(new UiState.Idle());
    }

    private void scheduleTimeout(Context context) {
        timeoutFuture = scheduler.schedule(() ->
                mainHandler.post(() -> {
                    if (pendingCall != null) pendingCall.cancel();
                    loginState.postValue(new UiState.Error("timeout", true));
                }), TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelLogin();
        scheduler.shutdownNow();
    }
}
