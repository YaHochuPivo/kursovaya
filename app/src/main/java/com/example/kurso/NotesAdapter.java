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
import java.text.SimpleDateFormat;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_NOTE = 0;
    private final int TYPE_PLAN = 1;

    private final Context context;
    private final List<Object> originalList = new ArrayList<>();
    private final List<Object> filteredList = new ArrayList<>();
    private String currentQuery = "";

    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();

    private OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged();
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged();
        }
    }

    public NotesAdapter(Context context, List<Object> itemList) {
        this.context = context;
        setItems(itemList);
    }

    public void setItems(List<Object> newItems) {
        originalList.clear();
        originalList.addAll(newItems);
        filter(currentQuery); // Применяем текущий фильтр к новым данным
    }

    public void setSelectionMode(boolean enable) {
        selectionMode = enable;
        selectedPositions.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
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
        currentQuery = query.toLowerCase().trim();
        filteredList.clear();

        if (currentQuery.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            for (Object item : originalList) {
                if (item instanceof Note) {
                    Note note = (Note) item;
                    if (matchesQuery(note)) {
                        filteredList.add(note);
                    }
                } else if (item instanceof PlanWrapper) {
                    PlanWrapper plan = (PlanWrapper) item;
                    if (matchesQuery(plan)) {
                        filteredList.add(plan);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesQuery(Note note) {
        return note.getTitle().toLowerCase().contains(currentQuery) ||
               note.getContent().toLowerCase().contains(currentQuery) ||
               (note.getMood() != null && note.getMood().toLowerCase().contains(currentQuery)) ||
               (note.getTags() != null && String.join(" ", note.getTags()).toLowerCase().contains(currentQuery));
    }

    private boolean matchesQuery(PlanWrapper plan) {
        for (TaskItem task : plan.getTasks()) {
            if (task.getText().toLowerCase().contains(currentQuery)) {
                return true;
            }
        }
        return false;
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
            
            // Форматируем дату из Timestamp или createdAt
            String dateStr = "Дата: —";
            if (note.getTimestamp() != null) {
                dateStr = "Дата: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(note.getTimestamp().toDate());
            } else if (note.getCreatedAt() > 0) {
                dateStr = "Дата: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(new Date(note.getCreatedAt()));
            }
            dateTime.setText(dateStr);
            
            mood.setText("Настроение: " + (note.getMood() != null ? note.getMood() : "—"));
            tags.setText(note.getTags() != null && !note.getTags().isEmpty()
                    ? "Теги: " + String.join(", ", note.getTags())
                    : "Теги: —");

            itemView.setBackgroundColor(selectedPositions.contains(position) ? Color.LTGRAY : Color.TRANSPARENT);

            itemView.setOnLongClickListener(v -> {
                if (!selectionMode) {
                    selectionMode = true;
                    toggleSelection(position);
                    notifyDataSetChanged(); // Обновляем все элементы для показа/скрытия кнопок
                    return true;
                }
                return false;
            });

            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(position);
                } else {
                    Intent intent = new Intent(context, CreateNoteActivity.class);
                    intent.putExtra("noteId", note.getId());
                    context.startActivity(intent);
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
                            filter(currentQuery);
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            });
        }
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView textPlan;
        ImageButton btnEditPlan, btnDeletePlan;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlan = itemView.findViewById(R.id.textPlan);
            btnEditPlan = itemView.findViewById(R.id.btnEditPlan);
            btnDeletePlan = itemView.findViewById(R.id.btnDeletePlan);
        }

        void bind(PlanWrapper plan, int position) {
            StringBuilder builder = new StringBuilder("План на день:\n");
            for (TaskItem task : plan.getTasks()) {
                builder.append("• ").append(task.getText());
                if (Boolean.TRUE.equals(task.isDone())) builder.append(" ✓");
                if (task.getTime() != null) builder.append(" (").append(task.getTime()).append(")");
                builder.append("\n");
            }
            textPlan.setText(builder.toString().trim());

            itemView.setBackgroundColor(selectedPositions.contains(position) ? Color.LTGRAY : Color.TRANSPARENT);

            itemView.setOnLongClickListener(v -> {
                if (!selectionMode) {
                    selectionMode = true;
                    toggleSelection(position);
                    notifyDataSetChanged(); // Обновляем все элементы для показа/скрытия кнопок
                    return true;
                }
                return false;
            });

            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(position);
                } else {
                    Intent intent = new Intent(context, DailyPlanActivity.class);
                    context.startActivity(intent);
                }
            });

            btnEditPlan.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
            btnDeletePlan.setVisibility(selectionMode ? View.GONE : View.VISIBLE);

            btnEditPlan.setOnClickListener(v -> {
                Intent intent = new Intent(context, DailyPlanActivity.class);
                context.startActivity(intent);
            });

            btnDeletePlan.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("daily_plans").document(plan.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "План удален", Toast.LENGTH_SHORT).show();
                            originalList.remove(plan);
                            filter("");
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            });
        }
    }

    private void toggleSelection(int position) {
        if (selectionMode) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
                if (selectedPositions.isEmpty()) {
                    selectionMode = false;
                }
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
            notifySelectionChanged();
        }
    }

    public void updateData(List<Object> newData) {
        originalList.clear();
        originalList.addAll(newData);
        filter(currentQuery);
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    public void clearSelection() {
        selectionMode = false;
        selectedPositions.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

}
