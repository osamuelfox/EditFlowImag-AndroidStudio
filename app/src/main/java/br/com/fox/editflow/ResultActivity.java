package br.com.fox.editflow;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.databinding.ActivityResultBinding;
import br.com.fox.editflow.ui.ResultViewModel;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;
    private ResultViewModel viewModel;
    private String resultImageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resultImageId = getIntent().getStringExtra("RESULT_IMAGE_ID");

        viewModel = new ViewModelProvider(this).get(ResultViewModel.class);

        loadResultImage();
        setupClickListeners();
        observeViewModel();
    }

    // ── Carregamento da imagem ────────────────────────────────────────────────

    private void loadResultImage() {
        if (resultImageId != null) {
            String downloadUrl = RetrofitClient.BASE_URL + "api/images/" + resultImageId + "/download";
            Glide.with(this)
                    .load(downloadUrl)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.stat_notify_error)
                    .into(binding.ivResult);
        }
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnEditOther.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        binding.btnDownload.setOnClickListener(v -> {
            if (resultImageId == null) {
                Snackbar.make(binding.getRoot(),
                        R.string.error_download_unavailable, Snackbar.LENGTH_SHORT).show();
                return;
            }
            viewModel.downloadImage(this, resultImageId, getString(R.string.loading_downloading));
        });
    }

    // ── Observação do ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.downloadState.observe(this, state -> {
            if (state instanceof UiState.Loading) {
                LoadingOverlayHelper.show(binding.loadingOverlay.getRoot(),
                        ((UiState.Loading) state).message);
                LoadingOverlayHelper.setFormEnabled(false, binding.btnDownload, binding.btnEditOther);

            } else if (state instanceof UiState.Success) {
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.btnDownload, binding.btnEditOther);

                // Mensagem de sucesso antes de qualquer ação
                Snackbar.make(binding.getRoot(),
                        R.string.success_image_saved, Snackbar.LENGTH_LONG).show();

                // Reset do estado para evitar re-trigger em rotação de tela
                viewModel.resetDownloadState();

            } else if (state instanceof UiState.Error) {
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.btnDownload, binding.btnEditOther);

                UiState.Error error = (UiState.Error) state;
                String msg = "timeout".equals(error.message)
                        ? getString(R.string.error_timeout)
                        : getString(R.string.error_download);

                Snackbar snackbar = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG);
                if (error.retryable) {
                    snackbar.setAction(R.string.action_retry, v ->
                            viewModel.downloadImage(this, resultImageId,
                                    getString(R.string.loading_downloading)));
                }
                snackbar.show();
                viewModel.resetDownloadState();
            }
        });
    }
}
