package com.example.kurso;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurso.NotesAdapter.PlanWrapper;
import com.google.firebase.firestore.*;

import java.util.*;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<Object> allItems = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotesAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        Button btnAddNote = view.findViewById(R.id.btnAddNote);
        Button btnAddPlan = view.findViewById(R.id.btnAddPlan);
        Button btnExport = view.findViewById(R.id.btnExport);
        Button btnImport = view.findViewById(R.id.btnImport);

        btnAddNote.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateNoteActivity.class)));
        btnAddPlan.setOnClickListener(v -> startActivity(new Intent(getContext(), DailyPlanActivity.class)));

        btnExport.setOnClickListener(v -> {
            adapter.setSelectionMode(true); // включаем режим выделения
            Toast.makeText(getContext(), "Выделите элементы и нажмите экспорт ещё раз", Toast.LENGTH_SHORT).show();
        });

        btnExport.setOnLongClickListener(v -> {
            List<Object> selectedItems = adapter.getSelectedItems();

            if (selectedItems.isEmpty()) {
                Toast.makeText(getContext(), "Выберите элементы для экспорта", Toast.LENGTH_SHORT).show();
                return true;
            }

            String[] formats = {"JSON", "CSV", "PDF"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("Выберите формат экспорта")
                    .setItems(formats, (dialog, which) -> {
                        String selectedFormat = formats[which].toLowerCase();
                        FileUtils.exportData(requireContext(), selectedItems, selectedFormat);
                        adapter.setSelectionMode(false);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

            return true;
        });


        btnImport.setOnClickListener(v -> {
            FileUtils.importData(requireContext(), "json", importedItems -> {
                allItems.addAll(importedItems);
                adapter.updateData(allItems);
                Toast.makeText(getContext(), "Импортировано: " + importedItems.size(), Toast.LENGTH_SHORT).show();
            });
        });

        loadData();
        return view;
    }


    private void loadData() {
        allItems.clear();

        db.collection("notes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Note note = doc.toObject(Note.class);
                        if (note != null) {
                            allItems.add(note);
                        }
                    }

                    db.collection("daily_plans")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(planSnapshot -> {
                                if (!planSnapshot.isEmpty()) {
                                    DocumentSnapshot doc = planSnapshot.getDocuments().get(0);
                                    List<String> tasks = (List<String>) doc.get("tasks");
                                    if (tasks != null) {
                                        allItems.add(new PlanWrapper(doc.getId(), tasks));
                                    }
                                }

                                adapter.updateData(allItems);
                            });
                });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Поиск заметок...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                }
                return true;
            }
        });
    }
}
