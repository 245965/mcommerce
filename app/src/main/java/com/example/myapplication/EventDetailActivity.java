
package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.data.EventParticipantDto;
import com.example.myapplication.data.ReminderRequest;
import com.example.myapplication.model.PollOption;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.budget.BudgetActivity;
import com.example.myapplication.chat.ChatActivity;
import com.example.myapplication.util.NavbarUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    TextView titleText, dateText, descText, locationText;
    ImageView eventImage;

    LinearLayout dateVotingOptionsContainer, locationVotingOptionsContainer;
    RadioGroup dateVotingRadioGroup, locationVotingRadioGroup;
    Button btnVoteDate, btnVoteLocation;

    ApiService apiService;

    List<PollOption> datePollOptions = new ArrayList<>();
    List<PollOption> locationPollOptions = new ArrayList<>();

    private Button btnOpenBudget;
    private ImageButton btnOpenChat;
    private ImageButton btnOpenTasks;
    private ImageButton btnOpenGallery;

    private boolean isParticipant = false;

    private String dateVotingEnd;
    private String locationVotingEnd;

    private TextView dateVotingEndInfo;
    private TextView locationVotingEndInfo;

    private Button btnJoinEvent;
    private TextView textAlreadyJoined;
    private LinearLayout layoutParticipants;

    private String budgetDeadline;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        apiService = RetrofitClient.getInstance(this).create(ApiService.class);

        titleText = findViewById(R.id.eventDetailTitle);
        dateText = findViewById(R.id.eventDetailDate);
        descText = findViewById(R.id.eventDetailDescription);
        eventImage = findViewById(R.id.eventDetailImage);
        locationText = findViewById(R.id.eventDetailLocation);
        dateVotingOptionsContainer = findViewById(R.id.dateVotingOptionsContainer);
        locationVotingOptionsContainer = findViewById(R.id.locationVotingOptionsContainer);
        dateVotingRadioGroup = findViewById(R.id.dateVotingRadioGroup);
        locationVotingRadioGroup = findViewById(R.id.locationVotingRadioGroup);
        btnVoteDate = findViewById(R.id.btnVoteDate);
        btnVoteLocation = findViewById(R.id.btnVoteLocation);
        btnOpenBudget = findViewById(R.id.btnOpenBudget);
        btnOpenTasks = findViewById(R.id.btnOpenTasks);
        btnOpenGallery = findViewById(R.id.btnOpenGallery);
        LinearLayout remindMeLayout = findViewById(R.id.remindMeLayout);

        btnJoinEvent = findViewById(R.id.btnJoinEvent);
        textAlreadyJoined = findViewById(R.id.textAlreadyJoined);
        layoutParticipants = findViewById(R.id.layoutParticipants);

        btnOpenTasks.setOnClickListener(v -> {
            Intent i = new Intent(EventDetailActivity.this,
                    com.example.myapplication.task.TaskActivity.class);
            i.putExtra("EVENT_ID",
                    getIntent().getLongExtra("eventId", -1));

            startActivity(i);
        });
        NavbarUtils.bindAvatar(this, R.id.detailToolbar, "http://10.0.2.2:8080");

        btnOpenChat = findViewById(R.id.btnOpenChat);
        dateVotingEndInfo = findViewById(R.id.dateVotingEndInfo);
        locationVotingEndInfo = findViewById(R.id.locationVotingEndInfo);
        LinearLayout shareContainer = findViewById(R.id.shareContainer);
        shareContainer.setOnClickListener(v -> shareEvent());

        Intent dataIntent = getIntent();
        if (Intent.ACTION_VIEW.equals(dataIntent.getAction())
                && dataIntent.getData() != null) {

            String last = dataIntent.getData().getLastPathSegment();
            try {
                long deepEventId = Long.parseLong(last);
                dataIntent.putExtra("eventId", deepEventId);
            } catch (NumberFormatException ignored) {
            }
        }

        btnOpenChat.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("EVENT_ID", getIntent().getLongExtra("eventId", -1));
            startActivity(i);
        });

        btnOpenGallery.setOnClickListener(v -> {
            Intent i = new Intent(EventDetailActivity.this, com.example.myapplication.gallery.GalleryActivity.class);
            i.putExtra("EVENT_ID", getIntent().getLongExtra("eventId", -1));
            startActivity(i);
        });

        btnOpenBudget.setOnClickListener(v -> {
            Long eventId = getIntent().getLongExtra("eventId", -1);
            Intent i = new Intent(EventDetailActivity.this, BudgetActivity.class);
            i.putExtra("EVENT_ID", eventId);
            i.putExtra("BUDGET_DEADLINE", budgetDeadline);
            startActivity(i);
        });

        btnJoinEvent.setOnClickListener(v -> {
            showRegulaminDialog(() -> {
                Log.d("EVENT_JOIN", "Zaakceptowano regulamin");
                Long eventId = getIntent().getLongExtra("eventId", -1);
                if (eventId == -1) {
                    Log.d("EVENT_JOIN", "Brakuje eventId lub userId");
                    return;
                }

                RetrofitClient.getCurrentUserId(this, userId -> {
                    apiService.joinEvent(eventId, userId).enqueue(new Callback<EventParticipantDto>() {
                        @Override
                        public void onResponse(Call<EventParticipantDto> call, Response<EventParticipantDto> response) {
                            if (response.isSuccessful()) {
                                Log.d("EVENT_JOIN", "Dołączono do wydarzenia: " + response.body());
                                Toast.makeText(EventDetailActivity.this, "Dołączono do wydarzenia!", Toast.LENGTH_SHORT).show();
                                isParticipant = true;
                                updateParticipantViews();
                            } else {
                                Log.e("EVENT_JOIN", "Błąd serwera: " + response.code());
                                Toast.makeText(EventDetailActivity.this, "Nie udało się dołączyć do wydarzenia", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<EventParticipantDto> call, Throwable t) {
                            Log.e("EVENT_JOIN", "Błąd sieci", t);
                            Toast.makeText(EventDetailActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        });

        remindMeLayout.setOnClickListener(v -> {
            final String[] options = {"1 dzień przed", "2 dni przed", "Tydzień przed"};
            final int[] daysBefore = {1, 2, 7};
            AlertDialog.Builder builder = new AlertDialog.Builder(EventDetailActivity.this);
            builder.setTitle("Kiedy przypomnieć?");
            builder.setItems(options, (dialog, which) -> {
                sendPushReminderRequest(daysBefore[which]);
            });
            builder.show();
        });

        Long eventId = getIntent().getLongExtra("eventId", -1);

        if (eventId != -1) {
            apiService.getEvent(eventId).enqueue(new Callback<com.example.myapplication.model.Event>() {
                @Override
                public void onResponse(Call<com.example.myapplication.model.Event> call, Response<com.example.myapplication.model.Event> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.example.myapplication.model.Event event = response.body();

                        dateVotingEnd = event.getDatePollDeadline();
                        locationVotingEnd = event.getLocationPollDeadline();
                        budgetDeadline = event.getBudgetDeadline();

                        isParticipant = event.isParticipant();
                        updateParticipantViews();

                        btnOpenBudget.setVisibility(isParticipant ? View.VISIBLE : View.GONE);

                        titleText.setText(event.getTitle());
                        dateText.setText(event.getDate());
                        descText.setText(event.getDescription());
                        locationText.setText(event.getLocation());

                        updateMapWithLocation(event.getLocation());

                        Glide.with(EventDetailActivity.this)
                                .load("http://10.0.2.2:8080" + event.getImageUrl())
                                .into(eventImage);

                        fetchUpdatedPollOptions(true);
                        fetchUpdatedPollOptions(false);

                        setupVotingOptions();
                        setupVoteButtons();

                        if (isParticipant) {
                            fetchAndShowParticipants();
                        }
                    }
                }

                @Override
                public void onFailure(Call<com.example.myapplication.model.Event> call, Throwable t) {
                    Toast.makeText(EventDetailActivity.this, "Błąd ładowania wydarzenia", Toast.LENGTH_SHORT).show();
                }
            });
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void shareEvent() {
        long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) {
            return;
        }

        String backendBase = "http://10.0.2.2:8080";
        String link = backendBase + "/go/event/" + eventId;

        String title = titleText.getText().toString();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_TEXT, "Zobacz wydarzenie: " + title + "\n" + link);
        send.setType("text/plain");
        startActivity(Intent.createChooser(send, "Udostępnij przez"));
    }

    private void showRegulaminDialog(Runnable onAccept) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_regulamin, null);
        CheckBox checkbox = dialogView.findViewById(R.id.checkboxAkceptuje);
        TextView tvRegulamin = dialogView.findViewById(R.id.tvRegulamin);
        tvRegulamin.setText("Przystępując do wydarzenia, zobowiązujesz się do przestrzegania zasad ustalonych przez organizatora.\n" +
                "Nie możesz się później wypisać z wydarzenia. Organizator może kontaktować się z Tobą w sprawie wydarzenia.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Regulamin wydarzenia")
                .setView(dialogView)
                .setPositiveButton("Potwierdź", null)
                .setNegativeButton("Anuluj", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button pozytywny = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            pozytywny.setEnabled(false);
            checkbox.setOnCheckedChangeListener((btn, checked) -> pozytywny.setEnabled(checked));
            pozytywny.setOnClickListener(v -> {
                dialog.dismiss();
                onAccept.run();
            });
        });

        dialog.show();
    }

    private void updateParticipantViews() {
        if (isParticipant) {
            btnJoinEvent.setVisibility(View.GONE);
            textAlreadyJoined.setVisibility(View.VISIBLE);
            layoutParticipants.setVisibility(View.VISIBLE);

            btnOpenBudget.setVisibility(View.VISIBLE);
            btnOpenChat.setVisibility(View.VISIBLE);
            btnOpenTasks.setVisibility(View.VISIBLE);
            btnOpenGallery.setVisibility(View.VISIBLE);

            setupVotingOptions();

            fetchAndShowParticipants();
        } else {
            btnJoinEvent.setVisibility(View.VISIBLE);
            textAlreadyJoined.setVisibility(View.GONE);
            layoutParticipants.setVisibility(View.GONE);

            btnOpenBudget.setVisibility(View.GONE);
            btnOpenChat.setVisibility(View.GONE);
            btnOpenTasks.setVisibility(View.GONE);
            btnOpenGallery.setVisibility(View.GONE);
        }
    }

    private void fetchAndShowParticipants() {
        Long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) return;
        apiService.getEventParticipants(eventId).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> participants = response.body();
                    layoutParticipants.removeAllViews();
                    TextView header = new TextView(EventDetailActivity.this);
                    header.setText("Uczestnicy wydarzenia (" + participants.size() + "):");
                    header.setTextColor(Color.WHITE);
                    header.setTextSize(16);
                    layoutParticipants.addView(header);

                    for (String name : participants) {
                        TextView tv = new TextView(EventDetailActivity.this);
                        tv.setText(name);
                        tv.setTextColor(Color.WHITE);
                        layoutParticipants.addView(tv);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
            }
        });
    }

    private void sendPushReminderRequest(int daysBefore) {
        Long eventId = getIntent().getLongExtra("eventId", -1);
        String fcmToken = getSharedPreferences("eventbuddy_prefs", MODE_PRIVATE).getString("fcmToken", null);

        if (fcmToken == null) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(t -> {
                        if (t.isSuccessful()) {
                            String newTok = t.getResult();
                            getSharedPreferences("eventbuddy_prefs", MODE_PRIVATE)
                                    .edit().putString("fcmToken", newTok).apply();

                            sendPushReminderRequest(daysBefore);
                        } else {
                            Toast.makeText(this, "Token FCM niegotowy – spróbuj za chwilę", Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        }

        ReminderRequest req = new ReminderRequest();
        req.eventId = eventId;
        req.daysBefore = daysBefore;
        req.fcmToken = fcmToken;

        ApiService api = RetrofitClient.getInstance(this).create(ApiService.class);
        api.registerReminder(req).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EventDetailActivity.this, "Przypomnienie ustawione!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EventDetailActivity.this, "Błąd ustawiania przypomnienia", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Toast.makeText(EventDetailActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isVotingActive(String endDateString) {
        if (endDateString == null || endDateString.isEmpty()) {
            return true;
        }
        try {
            java.util.Date endDate;
            if (endDateString.length() == 10) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                endDate = sdf.parse(endDateString);
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                endDate = sdf.parse(endDateString);
            }

            java.util.Date now = new java.util.Date();
            return now.before(endDate);
        } catch (Exception e) {
            return true;
        }
    }

    private void setupVotingOptions() {

        if (!isParticipant) {
            dateVotingOptionsContainer.setVisibility(View.GONE);
            btnVoteDate.setVisibility(View.GONE);
            locationVotingOptionsContainer.setVisibility(View.GONE);
            btnVoteLocation.setVisibility(View.GONE);
            dateVotingEndInfo.setVisibility(View.GONE);
            locationVotingEndInfo.setVisibility(View.GONE);
            return;
        }

        if (dateVotingEnd != null && !dateVotingEnd.isEmpty()) {
            dateVotingEndInfo.setVisibility(View.VISIBLE);
            if (isVotingActive(dateVotingEnd)) {
                dateVotingEndInfo.setText("Głosowanie na datę trwa do: " + dateVotingEnd);
            } else {
                dateVotingEndInfo.setText("Głosowanie na datę zakończone.");
            }
        } else {
            dateVotingEndInfo.setVisibility(View.GONE);
        }

        if (locationVotingEnd != null && !locationVotingEnd.isEmpty()) {
            locationVotingEndInfo.setVisibility(View.VISIBLE);
            if (isVotingActive(locationVotingEnd)) {
                locationVotingEndInfo.setText("Głosowanie na lokalizację trwa do: " + locationVotingEnd);
            } else {
                locationVotingEndInfo.setText("Głosowanie na lokalizację zakończone.");
            }
        } else {
            locationVotingEndInfo.setVisibility(View.GONE);
        }

        if (datePollOptions != null && !datePollOptions.isEmpty() && isVotingActive(dateVotingEnd)) {
            dateVotingOptionsContainer.setVisibility(View.VISIBLE);
            btnVoteDate.setVisibility(View.VISIBLE);
            dateVotingRadioGroup.removeAllViews();
            for (PollOption option : datePollOptions) {
                RadioButton rb = new RadioButton(this);
                rb.setText(option.getValue() + " (" + option.getVoteCount() + " głosów)");
                Log.d("TAG", "pollId = " + option.getPollId() + ", optionId = " + option.getId());
                rb.setTag(new Pair<>(option.getPollId(), option.getId()));
                rb.setTextColor(Color.WHITE);
                dateVotingRadioGroup.addView(rb);
            }

        } else {
            dateVotingOptionsContainer.setVisibility(View.GONE);
            btnVoteDate.setVisibility(View.GONE);
        }
        if (locationPollOptions != null && !locationPollOptions.isEmpty() && isVotingActive(locationVotingEnd)) {
            locationVotingOptionsContainer.setVisibility(View.VISIBLE);
            btnVoteLocation.setVisibility(View.VISIBLE);
            locationVotingRadioGroup.removeAllViews();
            for (PollOption option : locationPollOptions) {
                RadioButton rb = new RadioButton(this);
                rb.setText(option.getValue() + " (" + option.getVoteCount() + " głosów)");
                rb.setTag(new Pair<>(option.getPollId(), option.getId()));
                rb.setTextColor(Color.WHITE);
                locationVotingRadioGroup.addView(rb);
            }

        } else {
            locationVotingOptionsContainer.setVisibility(View.GONE);
            btnVoteLocation.setVisibility(View.GONE);
        }
    }

    private void setupVoteButtons() {
        btnVoteDate.setOnClickListener(v -> {
            int selectedId = dateVotingRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Wybierz opcję do głosowania na datę", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selectedRadio = findViewById(selectedId);
            Pair<Long, Long> ids = (Pair<Long, Long>) selectedRadio.getTag();
            sendVote(ids.first, ids.second, true);
        });

        btnVoteLocation.setOnClickListener(v -> {
            int selectedId = locationVotingRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Wybierz opcję do głosowania na lokalizację", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selectedRadio = findViewById(selectedId);
            Pair<Long, Long> ids = (Pair<Long, Long>) selectedRadio.getTag();
            sendVote(ids.first, ids.second, false);
        });
    }

    private void sendVote(Long pollId, Long optionId, boolean isDateVote) {
        apiService.vote(pollId, optionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EventDetailActivity.this, "Głos oddany!", Toast.LENGTH_SHORT).show();
                    fetchUpdatedPollOptions(isDateVote);
                } else {
                    Toast.makeText(EventDetailActivity.this, "Błąd oddawania głosu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EventDetailActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUpdatedPollOptions(boolean isDateVote) {
        Long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) {
            Toast.makeText(this, "Błąd: brak ID wydarzenia", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<PollOption>> call;

        if (isDateVote) {
            call = apiService.getDatePollOptions(eventId);
        } else {
            call = apiService.getLocationPollOptions(eventId);
        }

        call.enqueue(new Callback<List<PollOption>>() {
            @Override
            public void onResponse(Call<List<PollOption>> call, Response<List<PollOption>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PollOption> updatedOptions = response.body();

                    Log.d("PollOptions", "Odebrano " + updatedOptions.size() + " opcji");

                    if (isDateVote) {
                        datePollOptions = updatedOptions;
                    } else {
                        locationPollOptions = updatedOptions;
                    }

                    runOnUiThread(() -> setupVotingOptions());
                } else {
                    Toast.makeText(EventDetailActivity.this, "Błąd pobierania wyników", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PollOption>> call, Throwable t) {
                Toast.makeText(EventDetailActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSettlementDialog() {
        Long eventId = getIntent().getLongExtra("eventId", -1);
        if (eventId == -1) {
            Toast.makeText(this, "Brak ID wydarzenia", Toast.LENGTH_SHORT).show();
            return;
        }
        apiService.getSettlement(eventId).enqueue(new Callback<Map<String, Double>>() {
            @Override
            public void onResponse(Call<Map<String, Double>> call, Response<Map<String, Double>> response) {
                if (response.isSuccessful()) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Double> entry : response.body().entrySet()) {
                        double balance = entry.getValue();
                        if (balance < 0)
                            sb.append(entry.getKey()).append(" ➜ do zapłaty: ").append(-balance).append(" zł\n");
                        else if (balance > 0)
                            sb.append(entry.getKey()).append(" ➜ do otrzymania: ").append(balance).append(" zł\n");
                    }
                    new AlertDialog.Builder(EventDetailActivity.this)
                            .setTitle("Rozliczenie")
                            .setMessage(sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable t) {
            }
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (locationText != null && locationText.getText() != null) {
            String currentLoc = locationText.getText().toString();
            if (!currentLoc.isEmpty()) {
                updateMapWithLocation(currentLoc);
            }
        }
    }

    private void updateMapWithLocation(String locationName) {
        if (mMap == null || locationName == null || locationName.isEmpty()) return;

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(EventDetailActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    runOnUiThread(() -> {
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                    });
                }
            } catch (IOException e) {
                Log.e("MAP_ERROR", "Błąd geokodowania: " + e.getMessage());
            }
        }).start();
    }
}
