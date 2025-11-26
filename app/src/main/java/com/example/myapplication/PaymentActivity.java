package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.ClientSecretResponseDto;
import com.example.myapplication.data.CreateExpenseDto;
import com.example.myapplication.data.ExpenseDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardMultilineWidget;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {
    private Button payButton;
    private ProgressBar progress;
    private TextView statusText;
    private CardMultilineWidget cardWidget;

    private ApiService api;
    private Stripe stripe;

    private long eventId;

    private String expenseDesc;
    private double expenseAmount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Intent i = getIntent();
        eventId = i.getLongExtra("EVENT_ID", 0L);
        expenseDesc = i.getStringExtra("EXPENSE_DESC");
        expenseAmount = i.getDoubleExtra("EXPENSE_AMOUNT", 0.0);

        PaymentConfiguration.init(
                getApplicationContext(),
                getString(R.string.stripe_publishable_key)
        );
        stripe = new Stripe(
                getApplicationContext(),
                PaymentConfiguration.getInstance(getApplicationContext()).getPublishableKey()
        );

        payButton  = findViewById(R.id.buttonPay);
        progress   = findViewById(R.id.progress);
        statusText = findViewById(R.id.textStatus);
        cardWidget = findViewById(R.id.cardInputWidget);

        api = RetrofitClient.getInstance(getApplicationContext()).create(ApiService.class);

        payButton.setOnClickListener(v -> startPayment());
    }

    private void startPayment() {
        PaymentMethodCreateParams pmParams = cardWidget.getPaymentMethodCreateParams();
        if (pmParams == null) { toast("Uzupełnij dane karty"); return; }

        setLoading(true);
        requestClientSecret(pmParams, expenseDesc, expenseAmount);
    }


    private void requestClientSecret(PaymentMethodCreateParams pmParams, String desc, double amt) {
        api.createStripePayment(eventId, new BigDecimal(amt)).enqueue(new Callback<ClientSecretResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<ClientSecretResponseDto> call,
                                   @NonNull Response<ClientSecretResponseDto> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || response.body().getClientSecret() == null) {
                    setStatus("Błąd serwera");
                    return;
                }
                confirmPayment(pmParams, response.body().getClientSecret(), desc, amt);
            }

            @Override
            public void onFailure(@NonNull Call<ClientSecretResponseDto> call, @NonNull Throwable t) {
                setLoading(false);
                setStatus("Błąd połączenia: " + t.getMessage());
            }
        });
    }

    private void confirmPayment(PaymentMethodCreateParams pmParams, String clientSecret, String desc, double amt) {
        ConfirmPaymentIntentParams confirmParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(pmParams, clientSecret);

        stripe.confirmPayment(this, confirmParams);

        CreateExpenseDto dto = new CreateExpenseDto(desc, amt, eventId);
        api.addExpense(dto).enqueue(new Callback<ExpenseDto>() {
            @Override
            public void onResponse(Call<ExpenseDto> call, Response<ExpenseDto> response) {
                if (response.isSuccessful()) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    setStatus("Błąd zapisu wydatku po płatności");
                }
            }

            @Override
            public void onFailure(Call<ExpenseDto> call, Throwable t) {
                setStatus("Błąd sieci przy zapisie wydatku: " + t.getMessage());
            }
        });
    }

    private void setStatus(String msg) {
        statusText.setText(msg);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        payButton.setEnabled(!loading);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private static String safe(Editable e) {
        return e == null ? "" : e.toString().trim();
    }
}
