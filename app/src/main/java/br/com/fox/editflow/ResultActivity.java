package br.com.fox.editflow;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import br.com.fox.editflow.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String resultImageId = getIntent().getStringExtra("RESULT_IMAGE_ID");
        String resultImageUrl = getIntent().getStringExtra("RESULT_IMAGE_URL");

        if (resultImageUrl != null && !resultImageUrl.isEmpty()) {
            Glide.with(this).load(resultImageUrl).into(binding.ivResult);
        } else if (resultImageId != null) {
            // Assuming the download URL format based on your instructions
            String downloadUrl = "https://backend.fox.api.br/api/images/" + resultImageId + "/download";
            Glide.with(this).load(downloadUrl).into(binding.ivResult);
        }

        binding.btnEditOther.setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, MainActivity.class));
            finish();
        });

        binding.btnDownload.setOnClickListener(v -> {
            if (resultImageId != null) {
                String downloadUrl = "https://backend.fox.api.br/api/images/" + resultImageId + "/download";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Link de download não disponível", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
