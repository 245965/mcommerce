package com.example.myapplication.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.MyApp;                 // globalny kontekst
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class TokenRepository {

    private static final String PREFS_NAME = "eventbuddy_prefs";
    private static final String KEY_TOKEN  = "fcm_token";

    private static final String KEY_FCM_LOCAL  = "fcm_token_local";
    private static final String KEY_FCM_SYNCED = "fcm_token_synced";

    private TokenRepository() { }

    public static void register(String token) {
        Context ctx = MyApp.getContext();
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOKEN, token).apply();
        Log.d("FCM", "Token zapisany lokalnie: " + token);

        ApiService api = RetrofitClient.getInstance(ctx).create(ApiService.class);
        api.sendFcmToken(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                Log.d("FCM", "Token FCM wysłany do backendu: " + token);
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                Log.e("FCM", "Błąd wysyłania tokenu FCM do backendu", t);
            }
        });
    }

    public static void syncIfPending(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token  = sp.getString(KEY_FCM_LOCAL, null);
        boolean done  = sp.getBoolean(KEY_FCM_SYNCED, false);
        if (token == null || done) return;
        sendNow(ctx, token, sp);
    }

    public static void clearSyncState(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_FCM_SYNCED).apply();
    }

    private static void sendNow(Context ctx, String token, SharedPreferences sp) {
        ApiService api = RetrofitClient.getInstance(ctx).create(ApiService.class);
        api.sendFcmToken(token).enqueue(new retrofit2.Callback<Void>() {
            @Override public void onResponse(retrofit2.Call<Void> c, retrofit2.Response<Void> r) {
                if (r.isSuccessful()) {
                    sp.edit().putBoolean(KEY_FCM_SYNCED, true).apply();
                    android.util.Log.d("FCM", "Token FCM wysłany do backendu");
                } else {
                    android.util.Log.w("FCM", "Błąd HTTP przy wysyłce FCM: " + r.code());
                }
            }
            @Override public void onFailure(retrofit2.Call<Void> c, Throwable t) {
                android.util.Log.e("FCM", "Błąd sieci przy wysyłce FCM", t);
            }
        });
    }
}
