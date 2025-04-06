package com.example.kurso;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DailyPlansFragment extends Fragment {
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<String> taskList = new ArrayList<>();
    private FirebaseFirestore db;

    public DailyPlansFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_plans, container, false);
        recyclerView = view.findViewById(R.id.recyclerDailyTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(taskList);
        recyclerView.setAdapter(taskAdapter);

        db = FirebaseFirestore.getInstance();

        loadLatestPlan();

        return view;
    }

    private void loadLatestPlan() {
        db.collection("daily_plans")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> planData = doc.getData();
                        for (String key : planData.keySet()) {
                            if (!key.equals("timestamp")) {
                                String task = planData.get(key).toString();
                                if (!task.isEmpty()) {
                                    taskList.add(task);
                                }
                            }
                        }
                    }
                    taskAdapter.notifyDataSetChanged();
                });
    }
}
