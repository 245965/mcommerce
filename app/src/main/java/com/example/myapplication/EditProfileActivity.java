package com.example.myapplication;

import static com.example.myapplication.util.NavbarUtils.makeInitials;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.util.NavbarUtils;
import com.example.myapplication.util.ProfilePrefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFirst, etLast;
    private TextView tvEmail;
    private ImageView ivAvatarPreview;
    private Uri pickedAvatarUri = null;
    private ApiService api;
    private TextView tvAvatarInitials;

    private final java.util.concurrent.Executor io = java.util.concurrent.Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pickedAvatarUri = uri;
                    ivAvatarPreview.setVisibility(View.VISIBLE);
                    tvAvatarInitials.setVisibility(View.GONE);

                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .thumbnail(0.25f)
                            .into(ivAvatarPreview);
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        NavbarUtils.bindAvatar(this, R.id.profileToolbar, "http://10.0.2.2:8080");

        etFirst = findViewById(R.id.etFirstName);
        etLast  = findViewById(R.id.etLastName);
        tvEmail = findViewById(R.id.tvEmail);
        ivAvatarPreview = findViewById(R.id.ivAvatarPreview);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnPick = findViewById(R.id.btnPickPhoto);

        api = RetrofitClient.getInstance(this).create(ApiService.class);


        ProfilePrefs pp = new ProfilePrefs(this);
        etFirst.setText(pp.firstName());
        etLast.setText(pp.lastName());
        if (pp.email() != null) tvEmail.setText(pp.email());

        String url = pp.avatarUrl();
        if (url != null && !url.isEmpty()) {
            tvAvatarInitials.setVisibility(View.GONE);
            ivAvatarPreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load("http://10.0.2.2:8080" + url)
                    .into(ivAvatarPreview);
        } else {
            ivAvatarPreview.setVisibility(View.GONE);
            tvAvatarInitials.setVisibility(View.VISIBLE);
            tvAvatarInitials.setText(makeInitials(pp.firstName(), pp.lastName()));
        }

        btnPick.setOnClickListener(v -> pickImage.launch("image/*"));
        btnSave.setOnClickListener(v -> saveChanges()


        );





    }

    private void saveChanges() {
        String first = etFirst.getText().toString().trim();
        String last  = etLast.getText().toString().trim();
        boolean isValid = true;

        //walidacja imie
        if (first.isEmpty()) {
            etFirst.setError("Imię jest wymagane");
            etFirst.requestFocus();
            isValid = false;
        } else if (first.length() < 2) {
            etFirst.setError("Imię jest za krótkie (min. 2 znaki)");
            etFirst.requestFocus();
            isValid = false;
        } else if (!first.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+")) {
            etFirst.setError("Imię może zawierać tylko litery");
            isValid = false;
        }


        if (last.isEmpty()) {
            etLast.setError("Nazwisko jest wymagane");
            if (isValid) etLast.requestFocus();
            isValid = false;
        } else if (last.length() < 2) {
            etLast.setError("Nazwisko jest za krótkie (min. 2 znaki)");
            isValid = false;
        } else if (!last.matches("[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\-]+")) {
            etLast.setError("Nazwisko może zawierać tylko litery i myślnik");
            isValid = false;
        }

        if (!isValid) {
            return;
        }


        if (pickedAvatarUri != null) {
            uploadAvatarThenSaveNames(first, last);
        } else {
            updateNames(first, last);
        }
    }

    private void uploadAvatarThenSaveNames(String first, String last) {
        io.execute(() -> {
            try {
                File tmp = copyUriToTempFile(pickedAvatarUri);   // I/O poza UI
                String mime = getContentResolver().getType(pickedAvatarUri);
                if (mime == null) mime = "image/jpeg";

                RequestBody fileBody = RequestBody.create(tmp, MediaType.parse(mime));
                MultipartBody.Part part = MultipartBody.Part
                        .createFormData("avatar", ensureExt(tmp.getName(), mime), fileBody);

                runOnUiThread(() -> {
                    api.uploadAvatar(part).enqueue(new Callback<Map<String, String>>() {
                        @Override public void onResponse(Call<Map<String, String>> c, Response<Map<String, String>> r) {
                            if (!r.isSuccessful() || r.body() == null) {
                                String body = null;
                                try { body = r.errorBody() != null ? r.errorBody().string() : null; } catch (Exception ignore) {}
                                android.util.Log.e("UPLOAD", "HTTP " + r.code() + " body=" + body);
                                Toast.makeText(EditProfileActivity.this, "Błąd uploadu (" + r.code() + ")", Toast.LENGTH_LONG).show();
                                return;
                            }
                            String newUrl = r.body().get("avatarUrl");
                            ProfilePrefs pp = new ProfilePrefs(EditProfileActivity.this);
                            pp.save(pp.firstName(), pp.lastName(), newUrl, pp.email());
                            updateNames(first, last);
                        }
                        @Override public void onFailure(Call<Map<String, String>> c, Throwable t) {
                            android.util.Log.e("UPLOAD", "onFailure", t);
                            Toast.makeText(EditProfileActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(EditProfileActivity.this, "Nie udało się przygotować pliku", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateNames(String first, String last) {
        api.updateProfile(first, last).enqueue(new Callback<Map<String,String>>() {
            @Override public void onResponse(Call<Map<String,String>> c, Response<Map<String,String>> r) {
                if (r.isSuccessful()) {
                    ProfilePrefs pp = new ProfilePrefs(EditProfileActivity.this);
                    pp.save(first, last, pp.avatarUrl(), pp.email());

                    // odśwież navbar i wyjdź
                    NavbarUtils.bindAvatar(EditProfileActivity.this, R.id.profileToolbar, "http://10.0.2.2:8080");
                    Toast.makeText(EditProfileActivity.this, "Zapisano", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Błąd zapisu (" + r.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String,String>> c, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private String ensureExt(String name, String mime) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
            return name;
        }
        if ("image/png".equals(mime)) return name + ".png";
        if ("image/webp".equals(mime)) return name + ".webp";
        return name + ".jpg";
    }
}
