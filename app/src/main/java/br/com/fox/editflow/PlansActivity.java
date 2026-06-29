package br.com.fox.editflow;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import br.com.fox.editflow.databinding.ActivityPlansBinding;

public class PlansActivity extends AppCompatActivity {

    private ActivityPlansBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlansBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnClose.setOnClickListener(v -> finish());
    }
}
