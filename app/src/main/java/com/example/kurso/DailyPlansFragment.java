package com.example.kurso;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.firestore.*;
import java.util.*;

public class DailyPlansFragment extends Fragment {
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<TaskItem> taskList = new ArrayList<>();
    private FirebaseFirestore db;

    public DailyPlansFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_plans, container, false);
        recyclerView = view.findViewById(R.id.recyclerDailyTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        taskAdapter = new TaskAdapter(getContext(), taskList);
        recyclerView.setAdapter(taskAdapter);

        loadLatestPlan();

        return view;
    }

    private void loadLatestPlan() {
        db.collection("daily_plans")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    taskList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) doc.get("tasks");
                        if (rawTasks != null) {
                            for (Map<String, Object> taskMap : rawTasks) {
                                String text = (String) taskMap.get("text");
                                String time = (String) taskMap.get("time");
                                Boolean done = (Boolean) taskMap.get("completed");
                                taskList.add(new TaskItem(text, time, done != null ? done : false));
                            }
                        }
                    }
                    taskAdapter.notifyDataSetChanged();
                });
    }
}
