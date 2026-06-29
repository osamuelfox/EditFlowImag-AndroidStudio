package br.com.fox.editflow.api;

import br.com.fox.editflow.models.AuthResponse;
import br.com.fox.editflow.models.GenerationRequest;
import br.com.fox.editflow.models.GenerationResponse;
import br.com.fox.editflow.models.ImageResponse;
import br.com.fox.editflow.models.LoginRequest;
import br.com.fox.editflow.models.RegisterRequest;
import br.com.fox.editflow.models.UserProfile;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/user/profile")
    Call<UserProfile> getUserProfile();

    @Multipart
    @POST("api/images")
    Call<ImageResponse> uploadImage(@Part MultipartBody.Part file);

    @POST("api/generations")
    Call<GenerationResponse> createGeneration(@Body GenerationRequest request);

    @GET("api/generations/{id}")
    Call<GenerationResponse> getGenerationStatus(@Path("id") String id);
}
