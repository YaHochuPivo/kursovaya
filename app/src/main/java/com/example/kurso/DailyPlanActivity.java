package com.example.kurso;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class DailyPlanActivity extends AppCompatActivity {
    private EditText editTask1, editTask2, editTask3, editTask4, editTask5;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_plan);

        editTask1 = findViewById(R.id.editTask1);
        editTask2 = findViewById(R.id.editTask2);
        editTask3 = findViewById(R.id.editTask3);
        editTask4 = findViewById(R.id.editTask4);
        editTask5 = findViewById(R.id.editTask5);
        Button btnSavePlan = findViewById(R.id.btnSavePlan);
        db = FirebaseFirestore.getInstance();

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    private void savePlan() {
        Map<String, Object> plan = new HashMap<>();
        plan.put("task1", editTask1.getText().toString().trim());
        plan.put("task2", editTask2.getText().toString().trim());
        plan.put("task3", editTask3.getText().toString().trim());
        plan.put("task4", editTask4.getText().toString().trim());
        plan.put("task5", editTask5.getText().toString().trim());

        db.collection("daily_plans").add(plan).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "План на день сохранён!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
