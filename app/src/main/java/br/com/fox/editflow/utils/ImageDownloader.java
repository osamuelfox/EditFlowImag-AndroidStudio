package br.com.fox.editflow.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;
import java.io.OutputStream;

public class ImageDownloader {

    public interface DownloadCallback {
        void onFinished(boolean success);
    }

    public static void saveImageToGallery(Context context, String url, String fileName, DownloadCallback callback) {
        Glide.with(context)
                .asBitmap()
                .load(url)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        boolean success = saveBitmapToGallery(context, resource, fileName);
                        if (callback != null) callback.onFinished(success);
                    }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        if (callback != null) callback.onFinished(false);
                    }
                });
    }

    private static boolean saveBitmapToGallery(Context context, Bitmap bitmap, String fileName) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EditFlow");

                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null) return false;
                fos = context.getContentResolver().openOutputStream(imageUri);
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/EditFlow";
                java.io.File file = new java.io.File(imagesDir);
                if (!file.exists()) file.mkdir();
                java.io.File image = new java.io.File(imagesDir, fileName + ".png");
                fos = new java.io.FileOutputStream(image);
            }

            boolean saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            return saved;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
