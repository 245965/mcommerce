package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Event;

import java.util.ArrayList;
import java.util.List;

import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import com.example.myapplication.util.NavbarUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.Toast;

import com.example.myapplication.model.PaginatedResponse;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;


    private ApiService apiService;

    private ActivityResultLauncher<Intent> addEventLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        recyclerView = findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        NavbarUtils.bindAvatar(this, R.id.homeToolbar, "http://10.0.2.2:8080");
        ImageButton moreButton = findViewById(R.id.moreButton);
        moreButton.setOnClickListener(v -> showPopupMenu(v));
        TextView addButton = findViewById(R.id.addEventButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddEventActivity.class);
            addEventLauncher.launch(intent);
        });

        eventList = new ArrayList<>();
        apiService = RetrofitClient.getInstance(getApplicationContext()).create(ApiService.class);
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);
        addEventLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadEvents(0, 40);
                    }
                }
        );

        loadEvents(0, 40);
    }
    @Override
    protected void onResume() {
        super.onResume();
        NavbarUtils.bindAvatar(this, R.id.homeToolbar, "http://10.0.2.2:8080");
        loadEvents(0, 40);
    }
    private void logoutUser() {
        getSharedPreferences("eventbuddy_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show();
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
             if (id == R.id.menu_logout) {
                logoutUser();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void loadEvents(int page, int size) {
        Call<PaginatedResponse<Event>> call = apiService.getEvents(page, size);
        call.enqueue(new Callback<PaginatedResponse<Event>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Event>> call, Response<PaginatedResponse<Event>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Event> eventsFromApi = response.body().getContent();
                    eventList.clear();
                    eventList.addAll(eventsFromApi);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(HomeActivity.this, "Nie udało się pobrać wydarzeń", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Event>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}
