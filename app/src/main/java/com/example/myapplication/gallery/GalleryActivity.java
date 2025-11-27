package com.example.myapplication.gallery;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView   counterText;
    private Button     btnAddPhoto;

    private long eventId;
    private final List<String> photoUrls = new ArrayList<>();
    private PhotoPagerAdapter adapter;

    private final ApiService api = RetrofitClient
            .getInstance(this)
            .create(ApiService.class);

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    this::handleImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        viewPager   = findViewById(R.id.viewPager);
        counterText = findViewById(R.id.counterText);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);

        eventId = getIntent().getLongExtra("EVENT_ID", -1);
        if (eventId == -1) {
            Toast.makeText(this, "Brak ID wydarzenia", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new PhotoPagerAdapter(photoUrls);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position);
            }
        });

        loadPhotos();

        btnAddPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
    }


    private void loadPhotos() {
        api.getEventPhotos(eventId).enqueue(new Callback<List<String>>() {
            @Override public void onResponse(@NonNull Call<List<String>> call,
                                             @NonNull Response<List<String>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    photoUrls.clear();
                    photoUrls.addAll(resp.body());
                    adapter.notifyDataSetChanged();
                    updateCounter(0);
                } else showToast("Błąd ładowania galerii");
            }

            @Override public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                showToast("Błąd sieci: " + t.getMessage());
            }
        });
    }


    private void handleImagePicked(Uri uri) {
        if (uri == null) return;
        File tempFile = copyUriToTempFile(uri);
        if (tempFile == null) {
            showToast("Nie udało się odczytać pliku");
            return;
        }

        RequestBody reqFile = RequestBody.create(tempFile,
                MediaType.parse(getContentResolver().getType(uri)));
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", tempFile.getName(), reqFile);

        api.uploadEventPhoto(eventId, body).enqueue(new Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> resp) {
                if (resp.isSuccessful()) {
                    showToast("Zdjęcie wysłane!");
                    loadPhotos();
                } else showToast("Błąd uploadu");
            }

            @Override public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showToast("Błąd sieci: " + t.getMessage());
            }
        });
    }


    private File copyUriToTempFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            File temp = File.createTempFile("upload", ".tmp", getCacheDir());
            try (FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return temp;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateCounter(int position) {
        if (photoUrls.isEmpty()) counterText.setText("Brak zdjęć");
        else counterText.setText("Zdjęcie " + (position + 1) + "/" + photoUrls.size());
    }

    private void showToast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }


    private static class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoViewHolder> {
        private final List<String> data;
        PhotoPagerAdapter(List<String> d) { data = d; }

        @NonNull @Override public PhotoViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup p, int v) {
            android.view.View view = android.view.LayoutInflater
                    .from(p.getContext())
                    .inflate(R.layout.item_photo_pager, p, false);
            return new PhotoViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull PhotoViewHolder h, int pos) {
            Glide.with(h.image.getContext())
                    .load("http://10.0.2.2:8080" + data.get(pos))
                    .into(h.image);
        }

        @Override public int getItemCount() { return data.size(); }
    }

    private static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final android.widget.ImageView image;
        PhotoViewHolder(@NonNull View v) { super(v); image = v.findViewById(R.id.photo); }
    }
}
