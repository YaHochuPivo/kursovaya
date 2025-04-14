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
import com.example.kurso.TaskItem;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;

public class DailyPlanActivity extends AppCompatActivity implements TaskAdapter.OnTaskStatusChangeListener {

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
        if (getIntent().hasExtra("planId")) {
            planId = getIntent().getStringExtra("planId");
            ArrayList<TaskItem> tasksFromIntent = getIntent().getParcelableArrayListExtra("tasks");
            if (tasksFromIntent != null) {
                taskList.clear();
                taskList.addAll(tasksFromIntent);
                adapter.notifyDataSetChanged();
            }
        }
        if (getIntent().hasExtra("tasks")) {
            ArrayList<TaskItem> tasksFromIntent = getIntent().getParcelableArrayListExtra("tasks");
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

        List<Map<String, Object>> formattedTasks = new ArrayList<>();
        for (TaskItem task : taskList) {
            if (task.getText() != null && !task.getText().trim().isEmpty()) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("text", task.getText());
                taskMap.put("time", task.getTime());
                taskMap.put("done", task.isDone());
                formattedTasks.add(taskMap);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("tasks", formattedTasks);
        data.put("timestamp", new Date());

        db.collection("daily_plans")
            .document(planId)
            .update(data)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "План обновлен", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void savePlan() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("DailyPlanActivity", "Пользователь не авторизован при сохранении плана");
            return;
        }

        // Фильтруем пустые задачи
        List<TaskItem> validTasks = new ArrayList<>();
        for (TaskItem task : taskList) {
            if (task.getText() != null && !task.getText().trim().isEmpty()) {
                validTasks.add(task);
            }
        }

        if (validTasks.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну задачу", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем текущую локальную дату
        Calendar calendar = Calendar.getInstance();
        
        // Устанавливаем время на начало текущего дня
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long timestamp = calendar.getTimeInMillis();

        // Создаем новый план
        Plan plan = new Plan(validTasks);
        plan.setUserId(currentUser.getUid());
        plan.setTimestamp(timestamp);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d("DailyPlanActivity", String.format(
            "Сохранение плана: userId=%s, timestamp=%d (%s), tasks=%d",
            plan.getUserId(), plan.getTimestamp(), sdf.format(new Date(timestamp)), plan.getTasks().size()));

        // Если это редактирование существующего плана
        if (planId != null) {
            db.collection("daily_plans")
                .document(planId)
                .set(plan)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DailyPlanActivity", "План успешно обновлен: " + planId);
                    Toast.makeText(this, "План обновлен", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("DailyPlanActivity", "Ошибка при обновлении плана", e);
                    Toast.makeText(this, 
                        "Ошибка обновления плана: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        } else {
            // Сохраняем новый план
            db.collection("daily_plans")
                .add(plan)
                .addOnSuccessListener(documentReference -> {
                    Log.d("DailyPlanActivity", "План успешно сохранен: " + documentReference.getId());
                    Toast.makeText(this, "План сохранен", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("DailyPlanActivity", "Ошибка при сохранении плана", e);
                    Toast.makeText(this, 
                        "Ошибка сохранения плана: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }


}
