package com.example.kurso;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_NOTE = 0;
    private final int TYPE_PLAN = 1;

    private final Context context;
    private final List<Object> originalList = new ArrayList<>();
    private final List<Object> filteredList = new ArrayList<>();

    public NotesAdapter(Context context, List<Object> itemList) {
        this.context = context;
        updateData(itemList);
    }

    public void filter(String query) {
        filteredList.clear();

        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(originalList); // показать всё
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());

            for (Object item : originalList) {
                if (item instanceof Note) {
                    Note note = (Note) item;

                    boolean matchesTitle = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery);
                    boolean matchesContent = note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery);
                    boolean matchesMood = note.getMood() != null && note.getMood().toLowerCase().contains(lowerQuery);
                    boolean matchesTags = note.getTags() != null && note.getTags().toString().toLowerCase().contains(lowerQuery);

                    if (matchesTitle || matchesContent || matchesMood || matchesTags) {
                        filteredList.add(note);
                    }
                }
                // 🔴 НЕ добавляем PlanWrapper при фильтрации
            }
        }

        notifyDataSetChanged();
    }


    public void updateData(List<Object> newData) {
        originalList.clear();
        originalList.addAll(newData);
        filteredList.clear();
        filteredList.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filteredList.get(position) instanceof Note ? TYPE_NOTE : TYPE_PLAN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_NOTE) {
            return new NoteViewHolder(inflater.inflate(R.layout.item_note, parent, false));
        } else {
            return new PlanViewHolder(inflater.inflate(R.layout.item_plan, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = filteredList.get(position);
        if (holder instanceof NoteViewHolder) {
            ((NoteViewHolder) holder).bind((Note) item);
        } else if (holder instanceof PlanViewHolder) {
            ((PlanViewHolder) holder).bind((PlanWrapper) item);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class PlanWrapper {
        public String id;
        public List<String> tasks;

        public PlanWrapper(String id, List<String> tasks) {
            this.id = id;
            this.tasks = tasks;
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, dateTime, mood, tags;
        ImageButton btnEdit, btnDelete;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            content = itemView.findViewById(R.id.textContent);
            dateTime = itemView.findViewById(R.id.textDateTime);
            mood = itemView.findViewById(R.id.textMood);
            tags = itemView.findViewById(R.id.textTags);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Note note) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            dateTime.setText(note.getDateTime());
            mood.setText("Настроение: " + (note.getMood() != null ? note.getMood() : "—"));
            tags.setText(note.getTags() != null && !note.getTags().isEmpty()
                    ? "Теги: " + String.join(", ", note.getTags())
                    : "Теги: —");

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, CreateNoteActivity.class);
                intent.putExtra("noteId", note.getId());
                context.startActivity(intent);
            });

            btnDelete.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("notes").document(note.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Заметка удалена", Toast.LENGTH_SHORT).show();
                            originalList.remove(note);
                            filter(""); // обновим список
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            });
        }
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView textPlan;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlan = itemView.findViewById(R.id.textPlan);
        }

        void bind(PlanWrapper plan) {
            StringBuilder builder = new StringBuilder("План на день:\n");
            for (String task : plan.tasks) {
                builder.append("• ").append(task).append("\n");
            }
            textPlan.setText(builder.toString().trim());
        }
    }
}
