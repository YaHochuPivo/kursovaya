package com.example.kurso;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.example.kurso.TaskItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public class DailyPlanActivity extends AppCompatActivity implements TaskAdapter.OnTaskStatusChangeListener {
    private static final String TAG = "DailyPlanActivity";

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<TaskItem> taskList;
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
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // ⛔️ Убираем заголовок
        }

        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnSavePlan = findViewById(R.id.btnSavePlan);
        recyclerView = findViewById(R.id.recyclerViewTasks);
        db = FirebaseFirestore.getInstance();

        taskList = new ArrayList<>();
        taskList.add(new TaskItem("", null, false));

        adapter = new TaskAdapter(this, taskList);
        adapter.setOnTaskStatusChangeListener(this); // Устанавливаем слушатель

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 🔹 Добавление новой строки
        btnAddTask.setOnClickListener(v -> {
            taskList.add(new TaskItem("", "", false));

            adapter.notifyItemInserted(taskList.size() - 1);
        });

        // 🔹 Если это редактирование
        ArrayList<TaskItem> tasksFromIntent = getIntent().getParcelableArrayListExtra("tasks");
        if (getIntent().hasExtra("planId")) {
            planId = getIntent().getStringExtra("planId");
        }
        
        if (tasksFromIntent != null && !tasksFromIntent.isEmpty()) {
            taskList.clear();
            taskList.addAll(tasksFromIntent);
        } else {
            // Если нет задач, добавляем одну пустую
            taskList.clear();
            taskList.add(new TaskItem("", null, false));
        }
        adapter.notifyDataSetChanged();

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onTaskStatusChanged(int position, boolean isChecked) {
        if (planId != null) {
            TaskItem task = taskList.get(position);
            task.setDone(isChecked);
            updatePlanInFirestore();
        }
    }

    private void updatePlanInFirestore() {
        if (planId == null) return;
        savePlan(); // Redirect to the main save method for consistency
    }

    private void savePlan() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Пользователь не авторизован при сохранении плана");
            Toast.makeText(this, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate and filter tasks
        List<TaskItem> validTasks = taskList.stream()
            .filter(task -> task.getText() != null && !task.getText().trim().isEmpty())
            .collect(Collectors.toList());

        if (validTasks.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну задачу", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get start of current day timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long dayStartTimestamp = calendar.getTimeInMillis();

        // Create or update plan
        Plan plan = new Plan(validTasks);
        plan.setUserId(currentUser.getUid());
        plan.setTimestamp(dayStartTimestamp);

        // Log plan details
        logPlanDetails(plan);

        // Save to Firestore
        DocumentReference planRef = planId != null 
            ? db.collection("daily_plans").document(planId)
            : db.collection("daily_plans").document();

        planRef.set(plan)
            .addOnSuccessListener(aVoid -> {
                String message = planId != null ? "План обновлен" : "План сохранен";
                Log.d(TAG, message + ": " + planRef.getId());
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                String operation = planId != null ? "обновлении" : "сохранении";
                Log.e(TAG, "Ошибка при " + operation + " плана", e);
                Toast.makeText(this, 
                    "Ошибка при " + operation + " плана: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void logPlanDetails(Plan plan) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d(TAG, String.format(
            "Сохранение плана: userId=%s, timestamp=%d (%s), tasks=%d",
            plan.getUserId(),
            plan.getTimestamp(),
            sdf.format(new Date(plan.getTimestamp())),
            plan.getTasks().size()
        ));
    }
}
