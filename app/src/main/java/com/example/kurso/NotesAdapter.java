package com.example.kurso;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kurso.PlanWrapper; // ✅ Используем внешний PlanWrapper

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_NOTE = 0;
    private final int TYPE_PLAN = 1;

    private final Context context;
    private final List<Object> originalList = new ArrayList<>();
    private final List<Object> filteredList = new ArrayList<>();

    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();

    public NotesAdapter(Context context, List<Object> itemList) {
        this.context = context;
        setItems(itemList);
    }

    public void setItems(List<Object> newItems) {
        originalList.clear();
        originalList.addAll(newItems);
        filteredList.clear();
        filteredList.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean enable) {
        selectionMode = enable;
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    public List<Object> getSelectedItems() {
        List<Object> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < filteredList.size()) {
                selected.add(filteredList.get(pos));
            }
        }

        return selected;
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lowerQuery = query.toLowerCase(Locale.getDefault());
            for (Object item : originalList) {
                if (item instanceof Note) {
                    Note note = (Note) item;
                    if ((note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                            (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery)) ||
                            (note.getMood() != null && note.getMood().toLowerCase().contains(lowerQuery)) ||
                            (note.getTags() != null && note.getTags().toString().toLowerCase().contains(lowerQuery))) {
                        filteredList.add(note);
                    }
                }
            }
        }
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
            ((NoteViewHolder) holder).bind((Note) item, position);
        } else if (holder instanceof PlanViewHolder) {
            ((PlanViewHolder) holder).bind((PlanWrapper) item, position);
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
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

        void bind(Note note, int position) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            dateTime.setText(note.getDateTime());
            mood.setText("Настроение: " + (note.getMood() != null ? note.getMood() : "—"));
            tags.setText(note.getTags() != null && !note.getTags().isEmpty()
                    ? "Теги: " + String.join(", ", note.getTags())
                    : "Теги: —");

            itemView.setBackgroundColor(selectedPositions.contains(position) ? Color.LTGRAY : Color.TRANSPARENT);

            itemView.setOnLongClickListener(v -> {
                selectionMode = true;
                toggleSelection(position);
                return true;
            });

            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(position);
                }
            });

            btnEdit.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
            btnDelete.setVisibility(selectionMode ? View.GONE : View.VISIBLE);

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
                            filter("");
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

        void bind(PlanWrapper plan, int position) {
            StringBuilder builder = new StringBuilder("План на день:\n");
            for (String task : plan.getTasks()) {
                builder.append("• ").append(task).append("\n");
            }
            textPlan.setText(builder.toString().trim());

            itemView.setBackgroundColor(selectedPositions.contains(position) ? Color.LTGRAY : Color.TRANSPARENT);

            itemView.setOnLongClickListener(v -> {
                selectionMode = true;
                toggleSelection(position);
                return true;
            });

            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(position);
                }
            });
        }
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    public void updateData(List<Object> newData) {
        originalList.clear();
        originalList.addAll(newData);
        filter(""); // сбрасывает фильтр и обновляет
    }


}
