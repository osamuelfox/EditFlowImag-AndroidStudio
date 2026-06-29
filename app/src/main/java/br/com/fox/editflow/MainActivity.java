package br.com.fox.editflow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.provider.MediaStore;

import com.google.android.material.snackbar.Snackbar;

import br.com.fox.editflow.databinding.ActivityMainBinding;
import br.com.fox.editflow.models.ImageResponse;
import br.com.fox.editflow.models.UserProfile;
import br.com.fox.editflow.ui.MainViewModel;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;
import br.com.fox.editflow.utils.TokenManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_PERMISSION  = 101;

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupBackPressHandler();
        setupClickListeners();

        // Inicializa a UI com dados locais imediatamente
        updateUI();

        observeViewModel();

        viewModel.fetchUserProfile(this);
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.cardUpload.setOnClickListener(v -> checkPermissionAndPickImage());
        binding.btnUpgrade.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlansActivity.class);
            startActivity(intent);
        });
        binding.btnLogout.setOnClickListener(v -> {
            new TokenManager(this).clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isLoading) {
                    showCancelDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // ── Permissão e seleção de imagem ────────────────────────────────────────

    private void checkPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            Snackbar.make(binding.getRoot(),
                    R.string.error_permission_denied, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            viewModel.uploadImage(this, data.getData(), getString(R.string.loading_uploading));
        }
    }

    // ── Observação do ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.profileState.observe(this, state -> {
            if (state instanceof UiState.Loading) {
                binding.pbCredits.setVisibility(android.view.View.VISIBLE);
            } else if (state instanceof UiState.Success) {
                UserProfile profile = ((UiState.Success<UserProfile>) state).data;
                TokenManager tm = new TokenManager(this);
                
                // Sincroniza apenas o nome, o backend não gerencia créditos
                tm.saveAuth(tm.getToken(), tm.getCurrentEmail(), profile.getName());
                
                updateUI();
            } else if (state instanceof UiState.Error) {
                // Em caso de erro, apenas garante que os créditos locais continuem aparecendo
                updateUI();
            }
        });

        viewModel.uploadState.observe(this, state -> {
            if (state instanceof UiState.Loading) {
                isLoading = true;
                LoadingOverlayHelper.show(binding.loadingOverlay.getRoot(),
                        ((UiState.Loading) state).message);
                LoadingOverlayHelper.setFormEnabled(false, binding.cardUpload);

            } else if (state instanceof UiState.Success) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.cardUpload);

                @SuppressWarnings("unchecked")
                UiState.Success<ImageResponse> success = (UiState.Success<ImageResponse>) state;
                navigateToEdit(success.data);

            } else if (state instanceof UiState.Error) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                LoadingOverlayHelper.setFormEnabled(true, binding.cardUpload);
                handleError((UiState.Error) state);
            }
        });
    }

    private void handleError(UiState.Error error) {
        String msg = error.message;
        String displayMsg;

        if ("too_large".equals(msg)) {
            displayMsg = getString(R.string.error_upload_too_large);
        } else if ("image_processing".equals(msg)) {
            displayMsg = getString(R.string.error_image_processing);
        } else if ("connection".equals(msg)) {
            displayMsg = getString(R.string.error_connection);
        } else if ("timeout".equals(msg)) {
            displayMsg = getString(R.string.error_timeout);
        } else {
            displayMsg = getString(R.string.error_upload_server, safeInt(msg));
        }

        Snackbar snackbar = Snackbar.make(binding.getRoot(), displayMsg, Snackbar.LENGTH_LONG);
        if (error.retryable) {
            snackbar.setAction(R.string.action_retry, v -> checkPermissionAndPickImage());
        }
        snackbar.show();
    }

    // ── Diálogo de cancelamento ──────────────────────────────────────────────

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(R.string.dialog_cancel_loading_message)
                .setPositiveButton(R.string.dialog_cancel_confirm, (d, w) -> viewModel.cancelUpload())
                .setNegativeButton(R.string.dialog_cancel_dismiss, null)
                .show();
    }

    // ── Navegação ────────────────────────────────────────────────────────────

    private void navigateToEdit(ImageResponse imageResponse) {
        if (imageResponse == null) return;
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("IMAGE_ID", imageResponse.getId());
        startActivity(intent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateUI() {
        TokenManager tm = new TokenManager(this);
        int displayCredits = tm.getAvailableCredits();
        binding.tvCredits.setText(getString(R.string.credits_label, displayCredits));
        binding.tvUserName.setText(tm.getUserName().toLowerCase());
        
        binding.pbCredits.setVisibility(android.view.View.GONE);
        binding.tvCredits.setVisibility(android.view.View.VISIBLE);
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
