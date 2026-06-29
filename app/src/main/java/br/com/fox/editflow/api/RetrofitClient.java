package br.com.fox.editflow.api;

import android.content.Context;
import br.com.fox.editflow.utils.TokenManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class RetrofitClient {
    public static final String BASE_URL = "https://backend.fox.api.br/";

    private static Retrofit retrofit = null;

    private static OkHttpClient okHttpClient = null;

    public static OkHttpClient getOkHttpClient(Context context) {
        if (okHttpClient == null) {
            Context appContext = context.getApplicationContext();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    TokenManager tokenManager = new TokenManager(appContext);
                    String token = tokenManager.getToken();

                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();
                    if (token != null) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(requestBuilder.build());
                }
            };

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .build();
        }
        return okHttpClient;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
