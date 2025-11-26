package com.example.myapplication.budget;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.PaymentActivity;
import com.example.myapplication.R;
import com.example.myapplication.data.CreateExpenseDto;
import com.example.myapplication.data.ExpenseDto;
import com.example.myapplication.event.ExpenseAdapter;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.util.NavbarUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BudgetActivity extends AppCompatActivity {

    private ExpenseAdapter adapter;

    private long eventId = -1;

    private LocalDate deadline;
    private View btnAddExpense;
    private View btnShowSettlement;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        apiService = RetrofitClient.getInstance(getApplicationContext()).create(ApiService.class);

        eventId = getIntent().getLongExtra("EVENT_ID", -1);

        String deadlineStr = getIntent().getStringExtra("BUDGET_DEADLINE");
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                deadline = LocalDate.parse(deadlineStr);
            } catch (Exception e) {
                deadline = LocalDate.MAX;
            }
        } else {
            deadline = LocalDate.MAX;
        }

        Log.d("DEBUG_BUDGET", "Fallback deadline z Intentu: " + deadline);

        RecyclerView rv = findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpenseAdapter();
        rv.setAdapter(adapter);

        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnShowSettlement = findViewById(R.id.btnShowSettlement);

        btnAddExpense.setOnClickListener(v -> openAddExpenseSheet());
        btnShowSettlement.setOnClickListener(v -> showSettlementDialog());
        NavbarUtils.bindAvatar(this, R.id.budgetToolbar, "http://10.0.2.2:8080");

        if (eventId != -1) {
            apiService.getBudgetDeadline(eventId).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            deadline = LocalDate.parse(response.body());
                            Log.d("BUDGET", "Zaktualizowany deadline z serwera: " + deadline);
                            toggleButtons();
                        } catch (Exception e) {
                            Log.e("BUDGET", "Błąd parsowania daty", e);
                        }
                    } else {
                        Log.w("BUDGET", "Nie udało się pobrać deadline'u z serwera");
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("BUDGET", "Błąd sieci przy pobieraniu deadline'u: " + t.getMessage());
                }
            });
        }

        // Pobierz wydatki
        apiService.getExpensesForEvent(eventId).enqueue(new Callback<List<ExpenseDto>>() {
            @Override
            public void onResponse(Call<List<ExpenseDto>> call, Response<List<ExpenseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(BudgetActivity.this, "Brak danych lub błąd serwera", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ExpenseDto>> call, Throwable t) {
                Toast.makeText(BudgetActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        toggleButtons();
    }

    private void toggleButtons() {
        boolean afterDeadline = LocalDate.now().isAfter(deadline);
        btnAddExpense.setVisibility(afterDeadline ? View.GONE  : View.VISIBLE);
        btnShowSettlement.setVisibility(afterDeadline ? View.VISIBLE : View.GONE);
    }

    private void openAddExpenseSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_add_expense, null);

        EditText etDesc = v.findViewById(R.id.etExpenseDesc);
        EditText etAmt  = v.findViewById(R.id.etExpenseAmount);

        v.findViewById(R.id.btnSaveExpense).setOnClickListener(b -> {
            String desc   = etDesc.getText().toString().trim();
            String amtStr = etAmt.getText().toString().trim();
            if (desc.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(this,"Uzupełnij pola",Toast.LENGTH_SHORT).show(); return;
            }

            double amt = Double.parseDouble(amtStr);

            CreateExpenseDto dto = new CreateExpenseDto(desc, amt, eventId);

            Intent intent = new Intent(BudgetActivity.this, PaymentActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("EXPENSE_DESC", desc);
            intent.putExtra("EXPENSE_AMOUNT", amt);
            startActivityForResult(intent, 1001);
            sheet.dismiss();

        });

        sheet.setContentView(v);
        sheet.show();
    }

    private void showSettlementDialog() {
        if (eventId == -1) {
            Toast.makeText(this, "Brak ID wydarzenia", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getSettlement(eventId).enqueue(new Callback<Map<String, Double>>() {
            @Override
            public void onResponse(Call<Map<String, Double>> call, Response<Map<String, Double>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Double> entry : response.body().entrySet()) {
                        double balance = entry.getValue();
                        if (balance < 0)
                            sb.append(entry.getKey()).append(" ➜ do zapłaty: ").append(String.format("%.2f zł", -balance)).append("\n");
                        else if (balance > 0)
                            sb.append(entry.getKey()).append(" ➜ do otrzymania: ").append(String.format("%.2f zł", balance)).append("\n");
                    }

                    new AlertDialog.Builder(BudgetActivity.this)
                            .setTitle("Rozliczenie")
                            .setMessage(sb.toString().isEmpty() ? "Brak rozliczeń do wyświetlenia." : sb.toString())
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(BudgetActivity.this, "Błąd wczytywania rozliczenia", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable t) {
                Toast.makeText(BudgetActivity.this, "Błąd sieci: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            apiService.getExpensesForEvent(eventId).enqueue(new Callback<List<ExpenseDto>>() {
                @Override
                public void onResponse(Call<List<ExpenseDto>> call, Response<List<ExpenseDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        adapter.setData(response.body());
                    }
                }

                @Override
                public void onFailure(Call<List<ExpenseDto>> call, Throwable t) { }
            });
        }
    }


}
