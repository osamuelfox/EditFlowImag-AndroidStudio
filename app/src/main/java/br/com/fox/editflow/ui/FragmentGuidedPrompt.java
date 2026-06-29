package br.com.fox.editflow.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;

import br.com.fox.editflow.EditActivity;
import br.com.fox.editflow.R;
import br.com.fox.editflow.databinding.FragmentGuidedPromptBinding;
import br.com.fox.editflow.databinding.LayoutGuidedChipBinding;

public class FragmentGuidedPrompt extends Fragment {

    private FragmentGuidedPromptBinding binding;
    private GuidedPromptViewModel viewModel;
    private int currentStep = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGuidedPromptBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(GuidedPromptViewModel.class);

        setupListeners();
        updateUI();
    }

    private void setupListeners() {
        binding.btnNext.setOnClickListener(v -> {
            if (currentStep < 4) {
                currentStep++;
                updateUI();
            } else {
                if (getActivity() instanceof EditActivity) {
                    ((EditActivity) getActivity()).generateFromPrompt(viewModel.buildPrompt());
                }
            }
        });

        binding.btnBack.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateUI();
            }
        });

        binding.etProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.product.setValue(s.toString());
                validateStep();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateUI() {
        binding.tvStepTitle.setText(getString(R.string.step_format, currentStep));
        binding.btnBack.setVisibility(currentStep > 1 ? View.VISIBLE : View.INVISIBLE);
        binding.btnNext.setText(currentStep == 4 ? R.string.action_generate_wizard : R.string.action_next);

        binding.cgOptions.removeAllViews();
        binding.cgOptions.setVisibility(View.GONE);
        binding.tilProduct.setVisibility(View.GONE);

        switch (currentStep) {
            case 1:
                binding.tvQuestion.setText(R.string.question_style);
                setupOptions(new String[]{getString(R.string.style_modern), getString(R.string.style_minimalist), getString(R.string.style_corporate), getString(R.string.style_creative)}, viewModel.style);
                break;
            case 2:
                binding.tvQuestion.setText(R.string.question_platform);
                setupOptions(new String[]{getString(R.string.platform_instagram), getString(R.string.platform_linkedin), getString(R.string.platform_whatsapp), getString(R.string.platform_ecommerce)}, viewModel.platform);
                break;
            case 3:
                binding.tvQuestion.setText(R.string.question_product);
                binding.tilProduct.setVisibility(View.VISIBLE);
                binding.etProduct.setText(viewModel.product.getValue());
                break;
            case 4:
                binding.tvQuestion.setText(R.string.question_tone);
                setupOptions(new String[]{getString(R.string.tone_professional), getString(R.string.tone_casual), getString(R.string.tone_urgent), getString(R.string.tone_inspiring)}, viewModel.tone);
                break;
        }
        validateStep();
    }

    private void setupOptions(String[] options, androidx.lifecycle.MutableLiveData<String> liveData) {
        binding.cgOptions.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String option : options) {
            Chip chip = (Chip) inflater.inflate(R.layout.layout_guided_chip, binding.cgOptions, false);
            chip.setText(option);
            if (option.equals(liveData.getValue())) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    liveData.setValue(option);
                    validateStep();
                }
            });
            binding.cgOptions.addView(chip);
        }
    }

    private void validateStep() {
        boolean isValid = false;
        switch (currentStep) {
            case 1: isValid = viewModel.style.getValue() != null && !viewModel.style.getValue().isEmpty(); break;
            case 2: isValid = viewModel.platform.getValue() != null && !viewModel.platform.getValue().isEmpty(); break;
            case 3: isValid = viewModel.product.getValue() != null && !viewModel.product.getValue().isEmpty(); break;
            case 4: isValid = viewModel.tone.getValue() != null && !viewModel.tone.getValue().isEmpty(); break;
        }
        binding.btnNext.setEnabled(isValid);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
