package com.example.kurso;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyPlanActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<String> taskList;
    private FirebaseFirestore db;
    private String planId = null; // 🔹 Используется для редактирования

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_plan);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // ⛔️ Убираем заголовок
        }


        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnSavePlan = findViewById(R.id.btnSavePlan);
        recyclerView = findViewById(R.id.recyclerViewTasks);
        db = FirebaseFirestore.getInstance();

        taskList = new ArrayList<>();
        taskList.add(""); // первая пустая строка

        adapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 🔹 Добавление новой строки
        btnAddTask.setOnClickListener(v -> {
            taskList.add("");
            adapter.notifyItemInserted(taskList.size() - 1);
        });

        // 🔹 Если это редактирование
        if (getIntent().hasExtra("planId")) {
            planId = getIntent().getStringExtra("planId");
            List<String> tasksFromIntent = getIntent().getStringArrayListExtra("tasks");
            if (tasksFromIntent != null) {
                taskList.clear();
                taskList.addAll(tasksFromIntent);
                adapter.notifyDataSetChanged();
            }
        }

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void savePlan() {
        List<String> nonEmptyTasks = new ArrayList<>();
        for (String task : taskList) {
            if (!task.trim().isEmpty()) {
                nonEmptyTasks.add(task.trim());
            }
        }

        if (nonEmptyTasks.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну задачу", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("tasks", nonEmptyTasks);
        data.put("timestamp", new Date());

        if (planId != null) {
            // 🔄 Обновление существующего плана
            db.collection("daily_plans").document(planId).update(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "План обновлён!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка обновления плана", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 🆕 Создание нового плана
            db.collection("daily_plans").add(data)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "План на день сохранён!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка сохранения плана", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
