package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.model.Event;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.util.NavbarUtils;
import com.example.myapplication.util.ProfilePrefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView avatarImage;
    private TextView initialsView, nameView, emailView;
    private ApiService api;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPicked);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        NavbarUtils.bindAvatar(this, R.id.profileToolbar, "http://10.0.2.2:8080");

        avatarImage  = findViewById(R.id.profileAvatarImage);
        initialsView = findViewById(R.id.profileAvatarInitials);
        nameView     = findViewById(R.id.profileName);
        emailView    = findViewById(R.id.profileEmail);

        api = RetrofitClient.getInstance(this).create(ApiService.class);

        findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class))
        );
        avatarImage.setOnClickListener(v -> pickImage.launch("image/*"));
        initialsView.setOnClickListener(v -> pickImage.launch("image/*"));

        bindProfile();
    }

    @Override protected void onResume() {
        super.onResume();
        NavbarUtils.bindAvatar(this, R.id.profileToolbar, "http://10.0.2.2:8080");
        bindProfile();
    }

    private void bindProfile() {
        ProfilePrefs pp = new ProfilePrefs(this);
        String fn = pp.firstName();
        String ln = pp.lastName();
        String url = pp.avatarUrl();
        String email = pp.email();

        nameView.setText((fn + " " + ln).trim());
        if (email != null) emailView.setText(email);

        if (url != null && !url.isEmpty()) {
            initialsView.setVisibility(View.GONE);
            avatarImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load("http://10.0.2.2:8080" + url)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(avatarImage);
        } else {
            avatarImage.setVisibility(View.GONE);
            initialsView.setVisibility(View.VISIBLE);
            initialsView.setText(makeInitials(fn, ln));
        }
    }

    private String makeInitials(String f, String l) {
        char c1 = (f != null && !f.isEmpty()) ? Character.toUpperCase(f.charAt(0)) : '•';
        char c2 = (l != null && !l.isEmpty()) ? Character.toUpperCase(l.charAt(0)) : '•';
        return "" + c1 + c2;
    }

    private void onPicked(Uri uri) {
        if (uri == null) return;
        try {
            File tmp = copyUriToTempFile(uri);
            String mime = getContentResolver().getType(uri);
            RequestBody fileBody = RequestBody.create(tmp, MediaType.parse(mime != null ? mime : "image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", tmp.getName(), fileBody);

            api.uploadAvatar(part).enqueue(new Callback<Map<String,String>>() {
                @Override public void onResponse(Call<Map<String,String>> c, Response<Map<String,String>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        String newUrl = r.body().get("avatarUrl");
                        ProfilePrefs pp = new ProfilePrefs(ProfileActivity.this);
                        pp.save(pp.firstName(), pp.lastName(), newUrl, pp.email());
                        bindProfile();
                        NavbarUtils.bindAvatar(ProfileActivity.this, R.id.profileToolbar, "http://10.0.2.2:8080");
                    } else {
                        Toast.makeText(ProfileActivity.this, "Błąd uploadu", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Map<String,String>> c, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {}
    }

    private File copyUriToTempFile(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            File temp = File.createTempFile("upload", ".tmp", getCacheDir());
            try (FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[8192]; int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return temp;
        }
    }
}
