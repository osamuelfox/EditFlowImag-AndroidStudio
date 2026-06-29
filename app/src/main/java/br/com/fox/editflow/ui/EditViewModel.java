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
import br.com.fox.editflow.models.GenerationRequest;
import br.com.fox.editflow.models.GenerationResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel da tela de edição.
 * Encapsula a criação da geração e o polling de status,
 * com timeout global de 30s e UiState via LiveData.
 */
public class EditViewModel extends ViewModel {

    private static final long TIMEOUT_MS   = 30_000L;
    private static final int  POLL_DELAY_MS = 3_000;

    public final MutableLiveData<UiState> generationState = new MutableLiveData<>(new UiState.Idle());

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    private ScheduledFuture<?> timeoutFuture;
    private Call<GenerationResponse> pendingCall;
    private boolean cancelled = false;

    // ── API ─────────────────────────────────────────────────────────────────

    public void startGeneration(Context context, String imageId, String prompt, String loadingMsg) {
        cancelled = false;
        generationState.setValue(new UiState.Loading(loadingMsg));
        scheduleTimeout(context);

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        GenerationRequest request = new GenerationRequest(imageId, "PROMPT", prompt);
        pendingCall = api.createGeneration(request);
        pendingCall.enqueue(new Callback<GenerationResponse>() {
            @Override
            public void onResponse(Call<GenerationResponse> call, Response<GenerationResponse> response) {
                if (cancelled) return;
                if (response.isSuccessful() && response.body() != null) {
                    String genId = response.body().getId();
                    pollStatus(context, genId);
                } else {
                    cancelTimeout();
                    generationState.postValue(new UiState.Error(String.valueOf(response.code()), true));
                }
            }

            @Override
            public void onFailure(Call<GenerationResponse> call, Throwable t) {
                if (cancelled) return;
                cancelTimeout();
                generationState.postValue(new UiState.Error("connection", true));
            }
        });
    }

    private void pollStatus(Context context, String generationId) {
        if (cancelled) return;

        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        pendingCall = api.getGenerationStatus(generationId);
        pendingCall.enqueue(new Callback<GenerationResponse>() {
            @Override
            public void onResponse(Call<GenerationResponse> call, Response<GenerationResponse> response) {
                if (cancelled) return;

                if (!response.isSuccessful() || response.body() == null) {
                    // Resposta inválida — tenta novamente no próximo ciclo
                    scheduleNextPoll(context, generationId);
                    return;
                }

                GenerationResponse gen = response.body();
                if (gen.isSucceeded() && gen.getResultImageId() != null) {
                    cancelTimeout();
                    generationState.postValue(new UiState.Success<>(gen));
                } else if (gen.isFailed()) {
                    cancelTimeout();
                    String msg = gen.getErrorMessage() != null ? gen.getErrorMessage() : "server_error";
                    generationState.postValue(new UiState.Error(msg, true));
                } else {
                    // PROCESSING — continua polling
                    scheduleNextPoll(context, generationId);
                }
            }

            @Override
            public void onFailure(Call<GenerationResponse> call, Throwable t) {
                if (cancelled) return;
                // Falha de rede temporária — continua tentando
                scheduleNextPoll(context, generationId);
            }
        });
    }

    private void scheduleNextPoll(Context context, String generationId) {
        mainHandler.postDelayed(() -> pollStatus(context, generationId), POLL_DELAY_MS);
    }

    // ── Cancelamento ────────────────────────────────────────────────────────

    public void cancelGeneration() {
        cancelled = true;
        cancelTimeout();
        mainHandler.removeCallbacksAndMessages(null);
        if (pendingCall != null) pendingCall.cancel();
        generationState.setValue(new UiState.Idle());
    }

    // ── Timeout ─────────────────────────────────────────────────────────────

    private void scheduleTimeout(Context context) {
        cancelTimeout();
        timeoutFuture = scheduler.schedule(() ->
                mainHandler.post(() -> {
                    if (!cancelled) {
                        if (pendingCall != null) pendingCall.cancel();
                        mainHandler.removeCallbacksAndMessages(null);
                        generationState.postValue(new UiState.Error("timeout", true));
                    }
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
        cancelGeneration();
        scheduler.shutdownNow();
    }
}
