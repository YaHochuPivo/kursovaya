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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class EditProfileDialog extends Dialog {
    private EditText displayNameInput;
    private EditText bioInput;
    private Button saveButton;
    private Button cancelButton;
    private FirebaseFirestore db;

    public EditProfileDialog(@NonNull Context context) {
        super(context);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_edit_profile);

        // Инициализация компонентов
        displayNameInput = findViewById(R.id.displayNameInput);
        bioInput = findViewById(R.id.bioInput);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Загрузка текущих данных пользователя
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null) {
                displayNameInput.setText(user.getDisplayName());
            }
            
            // Загружаем bio из Firestore
            db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && document.getString("bio") != null) {
                            bioInput.setText(document.getString("bio"));
                        }
                    });
        }

        // Обработчики кнопок
        saveButton.setOnClickListener(v -> updateProfile());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void updateProfile() {
        String newDisplayName = displayNameInput.getText().toString().trim();
        String newBio = bioInput.getText().toString().trim();
        
        if (newDisplayName.isEmpty()) {
            displayNameInput.setError("Введите имя");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Обновляем displayName в Firebase Auth
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Обновляем информацию в Firestore
                            Map<String, Object> userUpdates = new HashMap<>();
                            userUpdates.put("displayName", newDisplayName);
                            userUpdates.put("bio", newBio);
                            userUpdates.put("lastUpdated", Timestamp.now());

                            db.collection("users")
                                    .document(user.getUid())
                                    .update(userUpdates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), 
                                            "Ошибка при сохранении данных: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(getContext(), 
                                "Ошибка при обновлении профиля: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
} 