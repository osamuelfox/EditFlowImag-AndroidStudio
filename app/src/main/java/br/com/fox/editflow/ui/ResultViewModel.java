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

import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.utils.ImageDownloader;

public class ResultViewModel extends ViewModel {

    private static final long TIMEOUT_MS = 30_000L;

    public final MutableLiveData<UiState> downloadState = new MutableLiveData<>(new UiState.Idle());

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timeoutFuture;

    public void downloadImage(Context context, String imageId, String loadingMsg) {
        if (downloadState.getValue() instanceof UiState.Loading) return; // evita duplo clique

        downloadState.setValue(new UiState.Loading(loadingMsg));
        scheduleTimeout();

        String downloadUrl = RetrofitClient.BASE_URL + "api/images/" + imageId + "/download";
        String fileName = "EditFlow_" + imageId;

        ImageDownloader.saveImageToGallery(context, downloadUrl, fileName, success -> {
            cancelTimeout();
            if (success) {
                downloadState.postValue(new UiState.Success<>());
            } else {
                downloadState.postValue(new UiState.Error("download_failed", true));
            }
        });
    }

    public void resetDownloadState() {
        downloadState.setValue(new UiState.Idle());
    }

    private void scheduleTimeout() {
        timeoutFuture = scheduler.schedule(() ->
                mainHandler.post(() ->
                        downloadState.postValue(new UiState.Error("timeout", true))
                ), TIMEOUT_MS, TimeUnit.MILLISECONDS);
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
        cancelTimeout();
        scheduler.shutdownNow();
    }
}
