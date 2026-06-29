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

import br.com.fox.editflow.databinding.ActivityLoginBinding;
import br.com.fox.editflow.ui.LoginViewModel;
import br.com.fox.editflow.ui.UiState;
import br.com.fox.editflow.utils.LoadingOverlayHelper;
import br.com.fox.editflow.utils.TokenManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Redireciona automaticamente se já autenticado
        if (new TokenManager(this).getToken() != null) {
            navigateToMain();
            return;
        }

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupBackPressHandler();
        setupClickListeners();
        observeViewModel();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        binding.btnTabRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
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

    // ── Validação e Login ────────────────────────────────────────────────────

    private void attemptLogin() {
        clearErrors();

        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);

        boolean valid = true;

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

        viewModel.login(this, email, password, getString(R.string.loading_logging_in));
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
    }

    // ── Observação do ViewModel ──────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.loginState.observe(this, state -> {
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

                UiState.Error error = (UiState.Error) state;
                handleError(error);
            }
        });
    }

    private void handleError(UiState.Error error) {
        String msg = error.message;

        if (msg == null) {
            // Credenciais inválidas — erro inline nos campos
            binding.tilEmail.setError(" ");
            binding.tilPassword.setError(getString(R.string.error_login));
        } else if ("connection".equals(msg)) {
            showRetrySnackbar(getString(R.string.error_connection));
        } else if ("timeout".equals(msg)) {
            showRetrySnackbar(getString(R.string.error_timeout));
        } else {
            // Erro de servidor genérico
            showRetrySnackbar(getString(R.string.error_login_server, safeInt(msg)));
        }
    }

    private void showRetrySnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry, v -> attemptLogin())
                .show();
    }

    // ── Diálogo de cancelamento ──────────────────────────────────────────────

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_cancel_title)
                .setMessage(R.string.dialog_cancel_loading_message)
                .setPositiveButton(R.string.dialog_cancel_confirm, (d, w) -> {
                    viewModel.cancelLogin();
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel_dismiss, null)
                .show();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setFormEnabled(boolean enabled) {
        LoadingOverlayHelper.setFormEnabled(enabled,
                binding.etEmail, binding.etPassword, binding.btnLogin,
                binding.btnTabRegister);
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
