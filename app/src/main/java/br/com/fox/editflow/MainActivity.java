package br.com.fox.editflow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import br.com.fox.editflow.api.ApiService;
import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.databinding.ActivityMainBinding;
import br.com.fox.editflow.models.ImageResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_PERMISSION = 101;

    private ActivityMainBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        binding.cardUpload.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_PERMISSION);
        } else {
            pickImage();
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Toast.makeText(this, "Permissão necessária para selecionar foto", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {
        File file = getFileFromUri(imageUri);
        if (file == null) {
            Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tenta obter o tipo exato (ex: image/jpeg, image/png)
        String mimeType = getContentResolver().getType(imageUri);
        if (mimeType == null) mimeType = "image/jpeg";

        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
        // Alguns backends esperam "image" em vez de "file"
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        binding.progressUpload.setVisibility(View.VISIBLE);
        binding.cardUpload.setEnabled(false);

        apiService.uploadImage(body).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.cardUpload.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(MainActivity.this, EditActivity.class);
                    intent.putExtra("IMAGE_ID", response.body().getId());
                    intent.putExtra("IMAGE_URI", imageUri.toString());
                    startActivity(intent);
                } else if (response.code() == 500) {
                    // Tentativa alternativa: mudar o nome do campo para "image" se der 500 com "file"
                    retryUploadWithDifferentName(imageUri, file, requestFile);
                } else {
                    Toast.makeText(MainActivity.this, "Erro " + response.code() + " no upload", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.cardUpload.setEnabled(true);
                Toast.makeText(MainActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void retryUploadWithDifferentName(Uri imageUri, File file, RequestBody requestFile) {
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        
        apiService.uploadImage(body).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.cardUpload.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(MainActivity.this, EditActivity.class);
                    intent.putExtra("IMAGE_ID", response.body().getId());
                    intent.putExtra("IMAGE_URI", imageUri.toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Erro persistente (500) no servidor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.cardUpload.setEnabled(true);
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Tenta manter a extensão original
            String extension = ".jpg";
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.contains("png")) extension = ".png";

            File tempFile = new File(getCacheDir(), "upload_image" + extension);
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
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
}

