package com.example.myapplication.task;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.EventParticipantDto;
import com.example.myapplication.data.TaskDto;
import com.example.myapplication.data.UserDto;
import com.example.myapplication.model.PaginatedResponse;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.util.NavbarUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private Button btnAddTask;
    private TaskAdapter adapter;
    private long eventId;
    private ArrayList<String> participants;
    private Map<String, Long> participantsIds;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        api = RetrofitClient.getInstance(this).create(ApiService.class);

        eventId      = getIntent().getLongExtra("EVENT_ID", -1);
        participants = getIntent().getStringArrayListExtra("PARTICIPANTS");
        if (participants == null) participants = new ArrayList<>();
        loadParticipants();

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter(api, this);                         // <-- NEW
        rvTasks.setAdapter(adapter);

        btnAddTask = findViewById(R.id.btnAddTask);
        btnAddTask.setOnClickListener(v -> openAddTaskSheet());
        NavbarUtils.bindAvatar(this, R.id.taskToolbar, "http://10.0.2.2:8080");

        loadTasks();
    }

    // Pobieranie uczestników
    private void loadParticipants(){
        api.getEventParticipants(eventId, 0, 50, "ACTIVE").enqueue(new Callback<PaginatedResponse<EventParticipantDto>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<EventParticipantDto>> call, Response<PaginatedResponse<EventParticipantDto>> response) {
                if (response.isSuccessful() && response.body() != null){
                    Stream<UserDto> participantUserDtosStream = response.body().getContent().stream().map(EventParticipantDto::getUser);
                    participantsIds = participantUserDtosStream.collect(Collectors.toMap(
                            u -> u.getFirstName() + " " + u.getLastName() + " ("+u.getEmail()+")",
                            UserDto::getId
                    ));
                    participants = new ArrayList<>(participantsIds.keySet());
                }
                else{
                    Toast.makeText(TaskActivity.this, "Błąd ładowania uczestników! Musisz być aktywym członkiem wydarzenia.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<EventParticipantDto>> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Błąd ładowania uczestników! Musisz być aktywym członkiem wydarzenia.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
    private void loadTasks() {
        api.getEventsTasks(0, 40, eventId).enqueue(new Callback<PaginatedResponse<TaskDto>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<TaskDto>> call, Response<PaginatedResponse<TaskDto>> response) {
                if (response.isSuccessful() && response.body() != null){
                    PaginatedResponse<TaskDto> page = response.body();
                    List<TaskDto> tasks = page.getContent();
                    adapter.setData(tasks);
                }
                else{
                    Toast.makeText(TaskActivity.this, "Błąd ładowania zadań! Musisz być aktywym członkiem wydarzenia.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<TaskDto>> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Błąd ładowania zadań! Musisz być aktywym członkiem wydarzenia.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void openAddTaskSheet() {
        BottomSheetDialog d = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_add_task, null);

        EditText            etTitle    = v.findViewById(R.id.etTaskTitle);
        AutoCompleteTextView acAssignee = v.findViewById(R.id.acAssignee);
        Button              btnSave    = v.findViewById(R.id.btnSaveTask);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item_white,
                participants
        );
        acAssignee.setAdapter(spinnerAdapter);
        acAssignee.setThreshold(1);

        acAssignee.setOnClickListener(v1 -> acAssignee.showDropDown());
        acAssignee.setOnFocusChangeListener((v1, hasFocus) -> {
            if (hasFocus) acAssignee.showDropDown();
        });

        btnSave.setOnClickListener(b -> {
            String title = etTitle.getText().toString().trim();
            String who   = acAssignee.getText().toString().trim();

            if (title.isEmpty() || who.isEmpty()) {
                Toast.makeText(this,
                        "Uzupełnij tytuł i wybierz osobę",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task();
            task.setTitle(title);
            task.setDone(false);
            api.addTask(eventId, task, participantsIds.get(who)).enqueue(
                    new Callback<TaskDto>() {
                        @Override
                        public void onResponse(Call<TaskDto> call, Response<TaskDto> response) {
                            if (response.isSuccessful() && response.body() != null){
                                TaskDto t = response.body();
                                adapter.add(t);
                            }
                            else{
                                Toast.makeText(TaskActivity.this, "Nie udało się dodać zadania!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<TaskDto> call, Throwable t) {
                            Toast.makeText(TaskActivity.this, "Nie udało się dodać zadania!", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            d.dismiss();
        });

        d.setContentView(v);
        d.show();
    }
}
