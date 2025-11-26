package com.example.myapplication.util;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;

public final class NavbarUtils {
    private NavbarUtils() {}

    public static void bindAvatar(AppCompatActivity activity, int includedRootId, @Nullable String baseUrl) {
        View root = activity.findViewById(includedRootId);
        if (root == null) return;

        ImageView img = root.findViewById(R.id.avatarImage);
        TextView  tv  = root.findViewById(R.id.avatarInitials);

        // klik = otwarcie profilu
        View.OnClickListener openProfile = v ->
                activity.startActivity(new Intent(activity, ProfileActivity.class));

        if (img != null) { img.setOnClickListener(openProfile); img.setClickable(true); }
        if (tv  != null) { tv.setOnClickListener(openProfile);  tv.setClickable(true); }

        SharedPreferences prefs = activity.getSharedPreferences("eventbuddy_prefs", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", "");
        String lastName  = prefs.getString("lastName",  "");
        String avatarUrl = prefs.getString("avatarUrl", null);

        String fullBase = (baseUrl == null || baseUrl.isEmpty()) ? "http://10.0.2.2:8080" : baseUrl;

        if (avatarUrl == null || avatarUrl.isEmpty()) {
            if (img != null) img.setVisibility(View.GONE);
            if (tv  != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(makeInitials(firstName, lastName));
            }
            return;
        }

        if (img == null || tv == null) return;
        String fullUrl = fullBase + avatarUrl;

        Glide.with(img.getContext())
                .load(fullUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e,
                                                Object model,
                                                Target<Drawable> target,
                                                boolean isFirstResource) {
                        // fallback → inicjały
                        img.setVisibility(View.GONE);
                        tv.setVisibility(View.VISIBLE);
                        tv.setText(makeInitials(firstName, lastName));
                        return true; // sami ogarniamy fallback, nie wstawiaj placeholdera
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource,
                                                   Object model,
                                                   Target<Drawable> target,
                                                   DataSource dataSource,
                                                   boolean isFirstResource) {

                        tv.setVisibility(View.GONE);
                        img.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(img);
    }

    public static String makeInitials(String first, String last) {
        char f = (first != null && !first.isEmpty()) ? Character.toUpperCase(first.charAt(0)) : '•';
        char l = (last  != null && !last.isEmpty())  ? Character.toUpperCase(last.charAt(0))  : '•';
        return "" + f + l;
    }
}
