package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.model.RegisterResponse;
import com.example.myapplication.util.ProfilePrefs;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText;
    private Button registerButton;
    private ApiService apiService;

    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isConfirmPasswordValid = false;
    private boolean isFirstNameValid = false;
    private boolean isLastNameValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        emailEditText = findViewById(R.id.exitTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        firstNameEditText = findViewById(R.id.exitTextName);
        lastNameEditText = findViewById(R.id.exitTextSurname);

        registerButton = findViewById(R.id.buttonRegisterSubmit);

        apiService = RetrofitClient.getInstance(getApplicationContext()).create(ApiService.class);


        registerButton.setOnClickListener(v -> registerUser());

        setupRealtimeValidation();
    }

    private void setupRealtimeValidation() {
        emailEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (input.length() > 60 || !android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                    emailEditText.setError("Nieprawidłowy email (max 60 znaków)");
                    isEmailValid = false;
                } else {
                    emailEditText.setError(null);
                    isEmailValid = true;
                }
            }
        });

        passwordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (input.length() < 8 ||
                        !input.matches(".*[A-Z].*") ||
                        !input.matches(".*[a-z].*") ||
                        !input.matches(".*\\d.*") ||
                        !input.matches(".*[!@#$%^&*()_+=\\[\\]{};:<>|./?,-].*")) {
                    passwordEditText.setError("Min. 8 znaków, duża, mała litera, cyfra, znak specjalny");
                    isPasswordValid = false;
                } else {
                    passwordEditText.setError(null);
                    isPasswordValid = true;
                }
            }
        });

        EditText confirmPasswordEditText = findViewById(R.id.editTextRepeatPassword);
        confirmPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String confirmPass = s.toString();
                String originalPass = passwordEditText.getText().toString();
                if (!confirmPass.equals(originalPass)) {
                    confirmPasswordEditText.setError("Hasła nie są zgodne");
                    isConfirmPasswordValid = false;
                } else {
                    confirmPasswordEditText.setError(null);
                    isConfirmPasswordValid = true;
                }
            }
        });

        firstNameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (!input.matches("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻ]{1,39}(-[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻ]{1,39})?$")) {
                    firstNameEditText.setError("Imię: Z dużej litery, max 40 znaków, może mieć jeden myślnik");
                    isFirstNameValid = false;
                } else {
                    firstNameEditText.setError(null);
                    isFirstNameValid = true;
                }
            }
        });

        lastNameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString().trim();
                if (!input.matches("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻ]{1,79}(-[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻ]{1,79})?$")) {
                    lastNameEditText.setError("Nazwisko: Z dużej litery, max 80 znaków, może mieć jeden myślnik");
                    isLastNameValid = false;
                } else {
                    lastNameEditText.setError(null);
                    isLastNameValid = true;
                }
            }
        });
    }

    private abstract class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }


    private void registerUser() {
        if (!isEmailValid || !isPasswordValid || !isConfirmPasswordValid || !isFirstNameValid || !isLastNameValid) {
            Toast.makeText(this, "Nie wszystkie pola zostały prawidłowo wypełnione", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<RegisterResponse> call = apiService.registerUser(username, password, firstName, lastName);

        call.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new ProfilePrefs(RegisterActivity.this).save(firstName, lastName, null,email);
                    Toast.makeText(RegisterActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Błąd rejestracji", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Błąd połączenia: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }
}