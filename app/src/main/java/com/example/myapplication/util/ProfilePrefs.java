package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ProfilePrefs {
    private static final String PREF = "eventbuddy_prefs";
    private static final String KEY_FN = "firstName";
    private static final String KEY_LN = "lastName";
    private static final String KEY_AV = "avatarUrl";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences sp;

    public ProfilePrefs(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void save(String first, String last, String avatarUrl, String email) {
        sp.edit()
                .putString(KEY_FN, first)
                .putString(KEY_LN, last)
                .putString(KEY_AV, avatarUrl)
                .putString(KEY_EMAIL, email)
                .apply();
    }
    public String email(){ return sp.getString(KEY_EMAIL, null); }
    public String firstName() { return sp.getString(KEY_FN, ""); }
    public String lastName()  { return sp.getString(KEY_LN,  ""); }
    public String avatarUrl() { return sp.getString(KEY_AV,  null); }

    public void clear() {
        sp.edit().remove(KEY_FN).remove(KEY_LN).remove(KEY_AV).apply();
    }
}
