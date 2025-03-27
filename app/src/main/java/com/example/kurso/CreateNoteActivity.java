package com.example.kurso;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class CreateNoteActivity extends AppCompatActivity {
    private EditText editTitle, editContent;
    private TextView textDateTime, textTags, selectedMood;
    private String selectedDateTime;
    private List<String> selectedTags = new ArrayList<>();
    private String selectedMoodStr = "";
    private FirebaseFirestore db;
    private String noteId;

    private final String[] availableTags = {"дома", "на улице", "с друзьями", "один"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        textDateTime = findViewById(R.id.textDateTime);
        textTags = findViewById(R.id.textTags);
        selectedMood = findViewById(R.id.selectedMood);
        Button btnSelectDateTime = findViewById(R.id.btnSelectDateTime);
        Button btnAddTag = findViewById(R.id.btnAddTag);
        Button btnSave = findViewById(R.id.btnSave);
        db = FirebaseFirestore.getInstance();

        // Настроение
        setupMoodSelection();

        // Дата по умолчанию
        selectedDateTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        textDateTime.setText("Дата и время: " + selectedDateTime);

        btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        btnAddTag.setOnClickListener(v -> showTagSelector());

        // Если редактируем
        Intent intent = getIntent();
        if (intent.hasExtra("noteId")) {
            noteId = intent.getStringExtra("noteId");
            editTitle.setText(intent.getStringExtra("noteTitle"));
            editContent.setText(intent.getStringExtra("noteContent"));
            // Здесь можно будет загрузить и mood/tags
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void setupMoodSelection() {
        int[] moodIds = {R.id.mood_sad, R.id.mood_angry, R.id.mood_neutral, R.id.mood_happy, R.id.mood_excited};
        String[] moodValues = {"sad", "angry", "neutral", "happy", "excited"};

        for (int i = 0; i < moodIds.length; i++) {
            int index = i;
            findViewById(moodIds[i]).setOnClickListener(v -> {
                selectedMoodStr = moodValues[index];
                selectedMood.setText("Настроение выбрано: " + ((TextView) v).getText());
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

    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> note = new HashMap<>();
        note.put("title", title);
        note.put("content", content);
        note.put("dateTime", selectedDateTime);
        note.put("tags", selectedTags);
        note.put("mood", selectedMoodStr);

        if (noteId == null) {
            db.collection("notes").add(note).addOnSuccessListener(docRef -> {
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
