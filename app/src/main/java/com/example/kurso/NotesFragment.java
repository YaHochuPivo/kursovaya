package com.example.kurso;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class NotesFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<Note> noteList;
    private FirebaseFirestore db;

    public NotesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        Button btnAddNote = view.findViewById(R.id.btnAddNote);
        Button btnAddPlan = view.findViewById(R.id.btnAddPlan);

        db = FirebaseFirestore.getInstance();
        noteList = new ArrayList<>();
        adapter = new NotesAdapter(getContext(), noteList);
        recyclerView.setAdapter(adapter);

        loadNotes();

        btnAddNote.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateNoteActivity.class)));
        btnAddPlan.setOnClickListener(v -> startActivity(new Intent(getContext(), DailyPlanActivity.class)));

        return view;
    }

    private void loadNotes() {
        db.collection("notes").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                noteList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Note note = document.toObject(Note.class);
                    noteList.add(note);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}