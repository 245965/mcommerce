package com.example.myapplication.network;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.example.myapplication.data.UserDto;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

import java.io.IOException;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;



public class RetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            TokenManager tokenManager = new TokenManager(context);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            clientBuilder.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                String token = tokenManager.getToken();
                Log.d("AUTH_TOKEN", "Token = " + token);
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer " + token);
                }

                Request request = requestBuilder.build();
                okhttp3.Response response = chain.proceed(request);
                HttpLoggingInterceptor logger = new HttpLoggingInterceptor(msg -> Log.d("HTTP", msg));
                logger.setLevel(HttpLoggingInterceptor.Level.HEADERS);
                clientBuilder.addInterceptor(logger);

                if (response.code() == 401) {
                    Log.d("LOGOWANIE", "LOGOWANIE");
                    tokenManager.clearToken();

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        Intent intent = new Intent(context, com.example.myapplication.LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                        Toast.makeText(context, "Sesja wygasła. Zaloguj się ponownie.", Toast.LENGTH_LONG).show();
                    });
                }

                return response;
            });

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/")
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static void getCurrentUserId(Context context, Consumer<Long> callback) {
        ApiService apiService = RetrofitClient.getInstance(context).create(ApiService.class);

        Log.d("USER_ID", "Wywołanie getCurrentUserId()");

        apiService.getCurrentUser().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("USER_ID", "Sukces! userId=" + response.body().getId());
                    callback.accept(response.body().getId());
                } else {
                    Log.e("USER_ID", "Błąd serwera lub brak ciała: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                Log.e("USER_ID", "Błąd sieci: ", t);
            }
        });
    }
}

