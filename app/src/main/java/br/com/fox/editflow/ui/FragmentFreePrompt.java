package br.com.fox.editflow.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import br.com.fox.editflow.EditActivity;
import br.com.fox.editflow.R;
import br.com.fox.editflow.databinding.FragmentFreePromptBinding;

public class FragmentFreePrompt extends Fragment {

    private FragmentFreePromptBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFreePromptBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnGenerate.setOnClickListener(v -> {
            String prompt = binding.etPrompt.getText() != null ? binding.etPrompt.getText().toString().trim() : "";
            if (TextUtils.isEmpty(prompt)) {
                binding.tilPrompt.setError(getString(R.string.edit_prompt_empty));
                return;
            }
            binding.tilPrompt.setError(null);
            if (getActivity() instanceof EditActivity) {
                ((EditActivity) getActivity()).generateFromPrompt(prompt);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
