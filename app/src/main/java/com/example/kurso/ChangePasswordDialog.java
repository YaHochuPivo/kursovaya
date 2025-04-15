package com.example.kurso;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

public class ChangePasswordDialog extends Dialog {
    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;
    private Button saveButton;
    private Button cancelButton;

    public ChangePasswordDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_change_password);

        // Инициализация компонентов
        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Обработчики кнопок
        saveButton.setOnClickListener(v -> validateAndChangePassword());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void validateAndChangePassword() {
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // Проверка пустых полей
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка длины пароля
        if (newPassword.length() < 6) {
            newPasswordInput.setError("Пароль должен быть не менее 6 символов");
            return;
        }

        // Проверка совпадения паролей
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordInput.setError("Пароли не совпадают");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Повторная аутентификация пользователя
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> updatePassword(newPassword))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), 
                            "Неверный текущий пароль", Toast.LENGTH_SHORT).show());
        }
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            Toast.makeText(getContext(), "Ошибка при изменении пароля", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
} 