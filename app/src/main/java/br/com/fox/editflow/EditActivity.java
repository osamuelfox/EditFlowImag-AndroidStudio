package br.com.fox.editflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import br.com.fox.editflow.api.RetrofitClient;
import br.com.fox.editflow.databinding.ActivityEditBinding;
import br.com.fox.editflow.models.GenerationResponse;
import br.com.fox.editflow.models.UserProfile;
import br.com.fox.editflow.ui.EditViewModel;
import br.com.fox.editflow.ui.FragmentFreePrompt;
import br.com.fox.editflow.ui.FragmentGuidedPrompt;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;
import br.com.fox.editflow.utils.TokenManager;

public class EditActivity extends AppCompatActivity {

    private ActivityEditBinding binding;
    private EditViewModel viewModel;
    private String imageId;
    private String originalImageUri;
    private boolean isLoading = false;
    private int currentCredits = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageId         = getIntent().getStringExtra("IMAGE_ID");
        originalImageUri = getIntent().getStringExtra("IMAGE_URI");

        viewModel = new ViewModelProvider(this).get(EditViewModel.class);

        // Inicializa créditos localmente (o backend não gerencia créditos)
        TokenManager tm = new TokenManager(this);
        currentCredits = tm.getAvailableCredits();

        setupImagePreview();
        setupViewPager();
        setupBackPressHandler();
        observeViewModel();
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new FragmentFreePrompt() : new FragmentGuidedPrompt();
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.tab_free_prompt : R.string.tab_guided_prompt);
        }).attach();
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

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isLoading) {
                    showCancelGenerationDialog();
                } else if (binding.viewPager.getCurrentItem() > 0) {
                   binding.viewPager.setCurrentItem(0);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── Geração ──────────────────────────────────────────────────────────────

    public void generateFromPrompt(String prompt) {
        if (imageId == null) {
            Snackbar.make(binding.getRoot(), R.string.error_image_id_missing, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (currentCredits <= 0) {
            showNoCreditsDialog();
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
                binding.viewPager.setUserInputEnabled(false);

            } else if (state instanceof UiState.Success) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                binding.viewPager.setUserInputEnabled(true);

                // Consome crédito no front-end após sucesso
                br.com.fox.editflow.utils.TokenManager tm = new br.com.fox.editflow.utils.TokenManager(this);
                tm.consumeCredit();
                currentCredits = tm.getAvailableCredits();

                @SuppressWarnings("unchecked")
                UiState.Success<GenerationResponse> success = (UiState.Success<GenerationResponse>) state;
                navigateToResult(success.data);

            } else if (state instanceof UiState.Error) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                binding.viewPager.setUserInputEnabled(true);
                handleError((UiState.Error) state);
            }
        });
    }

    private void handleError(UiState.Error error) {
        String msg = error.message;
        String displayMsg;

        if ("402".equals(msg)) {
            showNoCreditsDialog();
            return;
        }

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
            // No longer using attemptGenerate() since it was replaced by fragment-specific logic
            // For retry, we'd ideally need to know which prompt was used. 
            // Simple approach: just show the message.
        }
        snackbar.show();
    }

    private void showNoCreditsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_credits_title)
                .setMessage(R.string.dialog_no_credits_message)
                .setPositiveButton(R.string.action_view_plans, (d, w) -> {
                    Intent intent = new Intent(this, PlansActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
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
