package br.com.fox.editflow.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import br.com.fox.editflow.api.ApiService;
import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.models.ImageResponse;
import br.com.fox.editflow.models.UserProfile;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {

    private static final long TIMEOUT_MS = 30_000L;

    public final MutableLiveData<UiState> uploadState = new MutableLiveData<>(new UiState.Idle());
    public final MutableLiveData<UiState> profileState = new MutableLiveData<>(new UiState.Idle());

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timeoutFuture;
    private Call<ImageResponse> pendingCall;

    public void fetchUserProfile(Context context) {
        profileState.setValue(new UiState.Loading(""));
        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        api.getUserProfile().enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    profileState.postValue(new UiState.Success<>(response.body()));
                } else {
                    profileState.postValue(new UiState.Error(String.valueOf(response.code()), true));
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                profileState.postValue(new UiState.Error("connection", true));
            }
        });
    }

    public void uploadImage(Context context, Uri imageUri, String loadingMsg) {
        uploadState.setValue(new UiState.Loading(loadingMsg));
        scheduleTimeout();

        // Preparar arquivo em background thread (I/O)
        ioExecutor.execute(() -> {
            File file = getFileFromUri(context, imageUri);
            if (file == null) {
                cancelTimeout();
                uploadState.postValue(new UiState.Error("image_processing", false));
                return;
            }

            String mimeType = context.getContentResolver().getType(imageUri);
            if (mimeType == null) mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
            pendingCall = api.uploadImage(body);
            pendingCall.enqueue(new Callback<ImageResponse>() {
                @Override
                public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                    cancelTimeout();
                    if (!(uploadState.getValue() instanceof UiState.Loading)) return;

                    if (response.isSuccessful() && response.body() != null) {
                        uploadState.postValue(new UiState.Success<>(response.body()));
                    } else if (response.code() == 413) {
                        uploadState.postValue(new UiState.Error("too_large", false));
                    } else {
                        uploadState.postValue(new UiState.Error(String.valueOf(response.code()), true));
                    }
                }

                @Override
                public void onFailure(Call<ImageResponse> call, Throwable t) {
                    cancelTimeout();
                    if (!(uploadState.getValue() instanceof UiState.Loading)) return;
                    if (!call.isCanceled()) {
                        uploadState.postValue(new UiState.Error("connection", true));
                    }
                }
            });
        });
    }

    public void cancelUpload() {
        cancelTimeout();
        if (pendingCall != null) pendingCall.cancel();
        uploadState.setValue(new UiState.Idle());
    }

    private void scheduleTimeout() {
        timeoutFuture = scheduler.schedule(() ->
                mainHandler.post(() -> {
                    if (pendingCall != null) pendingCall.cancel();
                    uploadState.postValue(new UiState.Error("timeout", true));
                }), TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    private File getFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String mimeType = context.getContentResolver().getType(uri);
            String extension = (mimeType != null && mimeType.contains("png")) ? ".png" : ".jpg";
            String fileName = "upload_" + System.currentTimeMillis() + extension;
            File tempFile = new File(context.getCacheDir(), fileName);

            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelUpload();
        ioExecutor.shutdownNow();
        scheduler.shutdownNow();
    }
}
