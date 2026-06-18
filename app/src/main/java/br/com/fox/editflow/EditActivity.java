package br.com.fox.editflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import br.com.fox.editflow.api.ApiService;
import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.databinding.ActivityEditBinding;
import br.com.fox.editflow.models.GenerationRequest;
import br.com.fox.editflow.models.GenerationResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditActivity extends AppCompatActivity {

    private ActivityEditBinding binding;
    private ApiService apiService;
    private String imageId;
    private String originalImageUri;

    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int POLLING_INTERVAL = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        imageId = getIntent().getStringExtra("IMAGE_ID");
        originalImageUri = getIntent().getStringExtra("IMAGE_URI");

        if (originalImageUri != null) {
            Glide.with(this).load(Uri.parse(originalImageUri)).into(binding.ivPreview);
        }

        binding.btnGenerate.setOnClickListener(v -> {
            if (imageId == null) {
                Toast.makeText(this, "Erro: ID da imagem não encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            generateImage();
        });
    }

    private void generateImage() {
        String prompt = binding.etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Digite um prompt personalizado", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnGenerate.setEnabled(false);
        binding.btnGenerate.setText(R.string.edit_generating);
        binding.progressGenerate.setVisibility(View.VISIBLE);

        GenerationRequest request = new GenerationRequest(imageId, "PROMPT", prompt);
        apiService.createGeneration(request).enqueue(new Callback<GenerationResponse>() {
            @Override
            public void onResponse(Call<GenerationResponse> call, Response<GenerationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String generationId = response.body().getId();
                    pollGenerationStatus(generationId);
                } else {
                    resetButtonState();
                    Toast.makeText(EditActivity.this, "Erro ao iniciar geração: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenerationResponse> call, Throwable t) {
                resetButtonState();
                Toast.makeText(EditActivity.this, "Erro de conexão: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetButtonState() {
        binding.btnGenerate.setEnabled(true);
        binding.btnGenerate.setText(R.string.edit_generate_btn);
        binding.progressGenerate.setVisibility(View.GONE);
    }

    private void pollGenerationStatus(String generationId) {
        apiService.getGenerationStatus(generationId).enqueue(new Callback<GenerationResponse>() {
            @Override
            public void onResponse(Call<GenerationResponse> call, Response<GenerationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GenerationResponse genRes = response.body();
                    
                    // Log the response for debugging (visible in Logcat)
                    android.util.Log.d("EditActivity", "Status: " + genRes.getStatus() + " | ResultID: " + genRes.getResultImageId());

                    if ("COMPLETED".equalsIgnoreCase(genRes.getStatus()) || 
                        "SUCCESS".equalsIgnoreCase(genRes.getStatus()) || 
                        genRes.getResultImageId() != null) {
                        
                        binding.progressGenerate.setVisibility(View.GONE);
                        binding.btnGenerate.setEnabled(true);
                        
                        Intent intent = new Intent(EditActivity.this, ResultActivity.class);
                        intent.putExtra("RESULT_IMAGE_ID", genRes.getResultImageId());
                        intent.putExtra("RESULT_IMAGE_URL", genRes.getImageUrl());
                        startActivity(intent);
                        finish();
                    } else if ("FAILED".equalsIgnoreCase(genRes.getStatus()) || 
                               "ERROR".equalsIgnoreCase(genRes.getStatus())) {
                        resetButtonState();
                        Toast.makeText(EditActivity.this, "Geração falhou no servidor", Toast.LENGTH_SHORT).show();
                    } else {
                        // Still processing (PENDING, PROCESSING, etc)
                        handler.postDelayed(() -> pollGenerationStatus(generationId), POLLING_INTERVAL);
                    }
                } else {
                    // API error during polling, try again anyway
                    handler.postDelayed(() -> pollGenerationStatus(generationId), POLLING_INTERVAL);
                }
            }

            @Override
            public void onFailure(Call<GenerationResponse> call, Throwable t) {
                // Connection error during polling, try again
                handler.postDelayed(() -> pollGenerationStatus(generationId), POLLING_INTERVAL);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
