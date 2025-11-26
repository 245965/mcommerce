package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable; // <— ważne, bo używamy 'Editable'
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.network.dto.ClientSecretResponse;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardMultilineWidget;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    private EditText amountEdit;
    private Button payButton;
    private ProgressBar progress;
    private TextView statusText;
    private CardMultilineWidget cardWidget;

    private ApiService api;
    private Stripe stripe;

    private long eventId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // 1) EventId z Intentu
        Intent i = getIntent();
        eventId = (i != null) ? i.getLongExtra("eventId", 0L) : 0L;

        // 2) Stripe init (publishable key z strings.xml)
//        PaymentConfiguration.init(
//                getApplicationContext(),
//                getString(R.string.stripe_publishable_key)
//        );
        stripe = new Stripe(
                getApplicationContext(),
                PaymentConfiguration.getInstance(getApplicationContext()).getPublishableKey()
        );

        // 3) Widoki
        amountEdit = findViewById(R.id.editTextAmount);
        payButton  = findViewById(R.id.buttonPay);
        progress   = findViewById(R.id.progress);
        statusText = findViewById(R.id.textStatus);
        cardWidget = findViewById(R.id.cardInputWidget);

        // 4) API
        api = RetrofitClient.getInstance(getApplicationContext()).create(ApiService.class);

        payButton.setOnClickListener(v -> startPayment());
    }

    private void startPayment() {
        String amount = safe(amountEdit.getText());
        if (amount.isEmpty()) {
            toast("Podaj kwotę (np. 49.99)");
            return;
        }
        if (!amount.matches("\\d+(\\.\\d{1,2})?")) {
            toast("Zły format kwoty (np. 49.99)");
            return;
        }

        PaymentMethodCreateParams pmParams = cardWidget.getPaymentMethodCreateParams();
        if (pmParams == null) {
            toast("Uzupełnij dane karty");
            return;
        }

        setLoading(true);
        requestClientSecret(amount, pmParams);
    }

    private void requestClientSecret(String amount, PaymentMethodCreateParams pmParams) {
        api.createStripePayment(eventId, amount).enqueue(new Callback<ClientSecretResponse>() {
            @Override
            public void onResponse(@NonNull Call<ClientSecretResponse> call,
                                   @NonNull Response<ClientSecretResponse> response) {
                if (!response.isSuccessful()) {
                    setLoading(false);
                    setStatus("Błąd serwera (" + response.code() + ")");
                    return;
                }
                ClientSecretResponse body = response.body();
                if (body == null) {
                    setLoading(false);
                    setStatus("Pusta odpowiedź");
                    return;
                }
                if (body.error != null) {
                    setLoading(false);
                    setStatus("Błąd: " + body.error);
                    return;
                }
                if (body.clientSecret == null || body.clientSecret.isEmpty()) {
                    setLoading(false);
                    setStatus("Brak clientSecret");
                    return;
                }
                confirmPayment(pmParams, body.clientSecret);
            }

            @Override
            public void onFailure(@NonNull Call<ClientSecretResponse> call, @NonNull Throwable t) {
                setLoading(false);
                setStatus("Brak połączenia: " + t.getLocalizedMessage());
            }
        });
    }

    private void confirmPayment(PaymentMethodCreateParams pmParams, String clientSecret) {
        ConfirmPaymentIntentParams confirmParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(pmParams, clientSecret);

        stripe.confirmPayment(this, confirmParams);
        setLoading(false);
        setStatus("Płatność przetwarzana…");
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        payButton.setEnabled(!loading);
    }

    private void setStatus(String msg) {
        statusText.setText(msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private static String safe(Editable e) {
        return e == null ? "" : e.toString().trim();
    }
}
