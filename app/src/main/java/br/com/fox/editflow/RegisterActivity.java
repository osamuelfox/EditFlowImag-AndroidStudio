package br.com.fox.editflow;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import br.com.fox.editflow.databinding.ActivityRegisterBinding;
import br.com.fox.editflow.ui.RegisterViewModel;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        setupBackPressHandler();
        setupClickListeners();
        observeViewModel();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnTabLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
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

    // ── Validação e Cadastro ─────────────────────────────────────────────────

    private void attemptRegister() {
        clearErrors();

        String name     = getText(binding.etName);
        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (name.length() < 2) {
            binding.tilName.setError(getString(R.string.error_name_short));
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }

        if (!valid) return;

        viewModel.register(this, name, email, password, getString(R.string.loading_registering));
    }

    private void clearErrors() {
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
    }

    // ── Observação do ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.registerState.observe(this, state -> {
            if (state instanceof UiState.Loading) {
                isLoading = true;
                LoadingOverlayHelper.show(binding.loadingOverlay.getRoot(),
                        ((UiState.Loading) state).message);
                setFormEnabled(false);

            } else if (state instanceof UiState.Success) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                setFormEnabled(true);
                navigateToMain();

            } else if (state instanceof UiState.Error) {
                isLoading = false;
                LoadingOverlayHelper.hide(binding.loadingOverlay.getRoot());
                setFormEnabled(true);
                handleError((UiState.Error) state);
            }
        });
    }

    private void handleError(UiState.Error error) {
        String msg = error.message;

        if ("email_conflict".equals(msg)) {
            binding.tilEmail.setError(getString(R.string.error_register));
        } else if ("connection".equals(msg)) {
            showRetrySnackbar(getString(R.string.error_connection));
        } else if ("timeout".equals(msg)) {
            showRetrySnackbar(getString(R.string.error_timeout));
        } else {
            showRetrySnackbar(getString(R.string.error_register_server, safeInt(msg)));
        }
    }

    private void showRetrySnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry, v -> attemptRegister())
                .show();
    }

    // ── Diálogo de cancelamento ──────────────────────────────────────────────

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(R.string.dialog_cancel_loading_message)
                .setPositiveButton(R.string.dialog_cancel_confirm, (d, w) -> {
                    viewModel.cancel();
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel_dismiss, null)
                .show();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setFormEnabled(boolean enabled) {
        LoadingOverlayHelper.setFormEnabled(enabled,
                binding.etName, binding.etEmail, binding.etPassword, binding.btnRegister,
                binding.btnTabLogin);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
