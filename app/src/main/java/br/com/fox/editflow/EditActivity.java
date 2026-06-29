package br.com.fox.editflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.databinding.ActivityEditBinding;
import br.com.fox.editflow.models.GenerationResponse;
import br.com.fox.editflow.ui.EditViewModel;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;

public class EditActivity extends AppCompatActivity {

    private ActivityEditBinding binding;
    private EditViewModel viewModel;
    private String imageId;
    private String originalImageUri;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageId         = getIntent().getStringExtra("IMAGE_ID");
        originalImageUri = getIntent().getStringExtra("IMAGE_URI");

        viewModel = new ViewModelProvider(this).get(EditViewModel.class);

        setupImagePreview();
        setupBackPressHandler();
        setupClickListeners();
        observeViewModel();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupImagePreview() {
        if (originalImageUri != null && !originalImageUri.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(originalImageUri))
                    .into(binding.ivPreview);
        } else if (imageId != null) {
            // Carrega prévia do servidor se não há URI local
            String previewUrl = RetrofitClient.BASE_URL + "api/images/" + imageId + "/download";
            Glide.with(this)
                    .load(previewUrl)
                    .into(binding.ivPreview);
        }
    }

    private void setupClickListeners() {
        binding.btnGenerate.setOnClickListener(v -> {
            if (imageId == null) {
                binding.tilPrompt.setError(getString(R.string.error_image_id_missing));
                return;
            }
            attemptGenerate();
        });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isLoading) {
                    showCancelGenerationDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── Geração ──────────────────────────────────────────────────────────────

    private void attemptGenerate() {
        binding.tilPrompt.setError(null);

        String prompt = binding.etPrompt.getText() != null
                ? binding.etPrompt.getText().toString().trim() : "";

        if (TextUtils.isEmpty(prompt)) {
            binding.tilPrompt.setError(getString(R.string.edit_prompt_empty));
            return;
        }

        viewModel.startGeneration(this, imageId, prompt, getString(R.string.loading_generating));
    }

    // ── Observação do ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.generationState.observe(this, state -> {
            if (state instanceof UiState.Loading) {
                isLoading = true;
                LoadingOverlayHelper.show(binding.loadingOverlay.getRoot(),
                        ((UiState.Loading) state).message);
                LoadingOverlayHelper.setFormEnabled(false, binding.etPrompt, binding.btnGenerate);

            } else if (state instanceof UiState.Success) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.etPrompt, binding.btnGenerate);

                @SuppressWarnings("unchecked")
                UiState.Success<GenerationResponse> success = (UiState.Success<GenerationResponse>) state;
                navigateToResult(success.data);

            } else if (state instanceof UiState.Error) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.etPrompt, binding.btnGenerate);
                handleError((UiState.Error) state);
            }
        });
    }

    private void handleError(UiState.Error error) {
        String msg = error.message;
        String displayMsg;

        if ("connection".equals(msg)) {
            displayMsg = getString(R.string.error_connection);
        } else if ("timeout".equals(msg)) {
            displayMsg = getString(R.string.error_timeout);
        } else if ("server_error".equals(msg) || msg == null) {
            displayMsg = getString(R.string.error_generation_failed);
        } else {
            // Mensagem de erro do servidor (geração falhou)
            try {
                Integer.parseInt(msg);
                displayMsg = getString(R.string.error_generate_server, Integer.parseInt(msg));
            } catch (NumberFormatException e) {
                displayMsg = getString(R.string.error_generation_failed_detail, msg);
            }
        }

        Snackbar snackbar = Snackbar.make(binding.getRoot(), displayMsg, Snackbar.LENGTH_LONG);
        if (error.retryable) {
            snackbar.setAction(R.string.action_retry, v -> attemptGenerate());
        }
        snackbar.show();
    }

    // ── Diálogo de cancelamento ──────────────────────────────────────────────

    private void showCancelGenerationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(R.string.dialog_cancel_generation_message)
                .setPositiveButton(R.string.dialog_cancel_confirm, (d, w) -> {
                    viewModel.cancelGeneration();
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel_dismiss, null)
                .show();
    }

    // ── Navegação ────────────────────────────────────────────────────────────

    private void navigateToResult(GenerationResponse gen) {
        if (gen == null) return;
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("RESULT_IMAGE_ID",   gen.getResultImageId());
        intent.putExtra("ANALYSIS_TEXT",     gen.getAnalysisText());
        intent.putExtra("CHANGES_TEXT",      gen.getChangesText());
        intent.putExtra("DESCRIPTION_TEXT",  gen.getDescriptionText());
        startActivity(intent);
        finish();
    }
}
