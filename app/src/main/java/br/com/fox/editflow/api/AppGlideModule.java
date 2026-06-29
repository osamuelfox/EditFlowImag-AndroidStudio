package br.com.fox.editflow.api;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;

import java.io.InputStream;

import okhttp3.OkHttpClient;

@GlideModule
public class AppGlideModule extends com.bumptech.glide.module.AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        OkHttpClient client = RetrofitClient.getOkHttpClient(context);
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
