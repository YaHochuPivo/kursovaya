package com.example.kurso;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerText;
    private TextView forgotPasswordText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        loginButton.setOnClickListener(v -> loginUser());
        registerText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        forgotPasswordText.setOnClickListener(v -> showResetPasswordDialog());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Введите email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Введите пароль");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка авторизации: " + 
                            task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showResetPasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        EditText emailEditText = view.findViewById(R.id.resetEmailInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("Восстановление пароля")
            .setView(view)
            .setPositiveButton("Отправить", (dialog, which) -> {
                String email = emailEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(email)) {
                    resetPassword(email);
                } else {
                    Toast.makeText(LoginActivity.this, "Введите email", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Отмена", null);

        builder.create().show();
    }

    private void resetPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Инструкции по восстановлению пароля отправлены на email",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Ошибка отправки: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}