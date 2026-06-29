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
import br.com.fox.editflow.models.RegisterRequest;
import br.com.fox.editflow.utils.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends ViewModel {

    private static final long TIMEOUT_MS = 30_000L;

    public final MutableLiveData<UiState> registerState = new MutableLiveData<>(new UiState.Idle());

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timeoutFuture;
    private Call<AuthResponse> pendingCall;

    public void register(Context context, String name, String email, String password, String loadingMsg) {
        registerState.setValue(new UiState.Loading(loadingMsg));
        scheduleTimeout(context);

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        pendingCall = api.register(new RegisterRequest(name, email, password, 2));
        pendingCall.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                cancelTimeout();
                if (!(registerState.getValue() instanceof UiState.Loading)) return;

                if (response.isSuccessful() && response.body() != null) {
                    TokenManager tm = new TokenManager(context);
                    String email = response.body().getEmail();
                    String name = response.body().getName();
                    tm.saveAuth(response.body().getToken(), email, name);
                    tm.addCredits(email, 2); // Inicializa com 2 créditos no registro
                    registerState.postValue(new UiState.Success<>(response.body()));
                } else if (response.code() == 409) {
                    // E-mail já em uso — erro inline no campo de e-mail
                    registerState.postValue(new UiState.Error("email_conflict", false));
                } else {
                    registerState.postValue(new UiState.Error(String.valueOf(response.code()), true));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                cancelTimeout();
                if (!(registerState.getValue() instanceof UiState.Loading)) return;
                if (!call.isCanceled()) {
                    registerState.postValue(new UiState.Error("connection", true));
                }
            }
        });
    }

    public void cancel() {
        cancelTimeout();
        if (pendingCall != null) pendingCall.cancel();
        registerState.setValue(new UiState.Idle());
    }

    private void scheduleTimeout(Context context) {
        timeoutFuture = scheduler.schedule(() ->
                mainHandler.post(() -> {
                    if (pendingCall != null) pendingCall.cancel();
                    registerState.postValue(new UiState.Error("timeout", true));
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
        cancel();
        scheduler.shutdownNow();
    }
}
