package com.example.kurso;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;
import com.google.android.material.appbar.MaterialToolbar;


public class CreateNoteActivity extends AppCompatActivity {
    private EditText editTitle, editContent;
    private TextView textDateTime, textTags, selectedMood;
    private String selectedDateTime;
    private List<String> selectedTags = new ArrayList<>();
    private String selectedMoodStr = "";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String noteId;

    private final String[] availableTags = {"дома", "на улице", "с друзьями", "один"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // ⛔️ Убираем заголовок
        }


        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        textDateTime = findViewById(R.id.textDateTime);
        textTags = findViewById(R.id.textTags);
        selectedMood = findViewById(R.id.selectedMood);
        Button btnSelectDateTime = findViewById(R.id.btnSelectDateTime);
        Button btnAddTag = findViewById(R.id.btnAddTag);
        Button btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        selectedDateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        textDateTime.setText("Дата и время: " + selectedDateTime);

        btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        btnAddTag.setOnClickListener(v -> showTagSelector());

        setupMoodSelection();

        if (getIntent().hasExtra("noteId")) {
            noteId = getIntent().getStringExtra("noteId");
            loadNoteFromFirestore(noteId);
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();  // Закрыть текущую активити и вернуться назад
        return true;
    }


    private void setupMoodSelection() {
        int[] moodIds = {R.id.mood_sad, R.id.mood_angry, R.id.mood_neutral, R.id.mood_happy, R.id.mood_excited};
        String[] moodValues = {"Грустный", "Злой", "Нейтральный", "Счастливый", "Возбужденный"};

        for (int i = 0; i < moodIds.length; i++) {
            int index = i;
            findViewById(moodIds[i]).setOnClickListener(v -> {
                selectedMoodStr = moodValues[index];
                selectedMood.setText("Настроение: " + selectedMoodStr);
            });
        }
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                selectedDateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());
                textDateTime.setText("Дата и время: " + selectedDateTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTagSelector() {
        boolean[] checkedItems = new boolean[availableTags.length];
        new android.app.AlertDialog.Builder(this)
                .setTitle("Выберите теги")
                .setMultiChoiceItems(availableTags, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) selectedTags.add(availableTags[which]);
                    else selectedTags.remove(availableTags[which]);
                })
                .setPositiveButton("ОК", (dialog, which) -> {
                    textTags.setText("Теги: " + String.join(", ", selectedTags));
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadNoteFromFirestore(String noteId) {
        db.collection("notes").document(noteId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                editTitle.setText(documentSnapshot.getString("title"));
                editContent.setText(documentSnapshot.getString("content"));
                selectedDateTime = documentSnapshot.getString("dateTime");
                textDateTime.setText("Дата и время: " + selectedDateTime);

                selectedMoodStr = documentSnapshot.getString("mood");
                if (selectedMoodStr != null) {
                    selectedMood.setText("Настроение: " + selectedMoodStr);
                }

                selectedTags = (List<String>) documentSnapshot.get("tags");
                if (selectedTags != null && !selectedTags.isEmpty()) {
                    textTags.setText("Теги: " + String.join(", ", selectedTags));
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки заметки", Toast.LENGTH_SHORT).show());
    }

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        note.put("dateTime", selectedDateTime);
        note.put("mood", selectedMoodStr != null ? selectedMoodStr : "Не указано");
        note.put("tags", selectedTags != null ? selectedTags : new ArrayList<String>());
        note.put("userId", userId);

        if (noteId == null) {
            db.collection("notes").add(note).addOnSuccessListener(documentReference -> {
                String newNoteId = documentReference.getId();
                db.collection("notes").document(newNoteId).update("id", newNoteId);
                Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                finish();
            });
        } else {
            db.collection("notes").document(noteId).update(note).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Заметка обновлена", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
