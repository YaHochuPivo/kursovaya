package com.example.kurso;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class TrashFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notesListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trash, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTrash);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        adapter = new NotesAdapter(getContext(), new ArrayList<>());
        adapter.setOnRestoreClickListener((note) -> restoreNote(note));
        adapter.setOnDeleteForeverClickListener((note) -> deleteNoteForever(note));
        recyclerView.setAdapter(adapter);

        setupRealtimeUpdates();

        return view;
    }

    private void setupRealtimeUpdates() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        notesListener = db.collection("deleted_notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка при получении данных", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (snapshots != null) {
                    List<Object> deletedNotes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        Note note = document.toObject(Note.class);
                        note.setId(document.getId());
                        deletedNotes.add(note);
                    }
                    adapter.updateData(deletedNotes);
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notesListener != null) {
            notesListener.remove();
        }
    }

    private void restoreNote(Note note) {
        if (note == null || note.getId() == null) return;

        // Получаем данные заметки из корзины
        db.collection("deleted_notes")
            .document(note.getId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Сохраняем заметку в основную коллекцию
                    db.collection("notes")
                        .add(documentSnapshot.getData())
                        .addOnSuccessListener(reference -> {
                            // После успешного восстановления удаляем из корзины
                            db.collection("deleted_notes")
                                .document(note.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Заметка восстановлена", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Ошибка удаления из корзины", Toast.LENGTH_SHORT).show();
                                    }
                                });
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Ошибка восстановления заметки", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            });
    }

    private void deleteNoteForever(Note note) {
        if (note == null || note.getId() == null) return;

        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Удаление навсегда")
            .setMessage("Вы уверены, что хотите удалить заметку навсегда? Это действие нельзя отменить.")
            .setPositiveButton("Удалить", (dialog, which) -> {
                db.collection("deleted_notes")
                    .document(note.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Заметка удалена навсегда", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Ошибка удаления заметки", Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
} 