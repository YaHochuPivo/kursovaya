package com.example.kurso;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private TextView loginText;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginText = findViewById(R.id.loginText);

        registerButton.setOnClickListener(v -> registerUser());
        loginText.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Валидация полей
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Введите email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Введите корректный email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Введите пароль");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Пароль должен быть не менее 6 символов");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Пароли не совпадают");
            return;
        }

        // Сначала проверяем, существует ли пользователь
        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null && !signInMethods.isEmpty()) {
                            // Email уже используется
                            emailInput.setError("Email уже зарегистрирован");
                            Toast.makeText(RegisterActivity.this,
                                    "Этот email уже зарегистрирован. Попробуйте войти или восстановить пароль.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Email свободен, регистрируем пользователя
                            createNewUser(email, password);
                        }
                    } else {
                        // Ошибка при проверке email
                        Toast.makeText(RegisterActivity.this,
                                "Ошибка проверки email: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createNewUser(String email, String password) {
        // Регистрация пользователя
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Сохраняем дополнительную информацию о пользователе в Firestore
                        saveUserToFirestore(email);
                    } else {
                        Exception exception = task.getException();
                        String errorMessage = "Ошибка регистрации";
                        
                        if (exception != null) {
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Этот email уже зарегистрирован. Попробуйте войти или восстановить пароль.";
                                emailInput.setError("Email уже зарегистрирован");
                            } else {
                                errorMessage = "Ошибка регистрации: " + exception.getMessage();
                            }
                        }
                        
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String email) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("displayName", email.split("@")[0]); // Временное имя из email
        user.put("bio", "");
        user.put("createdAt", Timestamp.now());
        user.put("lastUpdated", Timestamp.now());
        user.put("friendIds", new ArrayList<>()); // Пустой список друзей
        user.put("profileImagePath", ""); // Путь к фото профиля
        user.put("mood", ""); // Текущее настроение
        user.put("moodHistory", new ArrayList<>()); // История настроений

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this,
                            "Регистрация успешно завершена",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this,
                            "Ошибка сохранения данных: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
} 