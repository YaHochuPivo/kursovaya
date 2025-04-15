package com.example.kurso;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;

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
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnRestoreClickListener onRestoreClickListener;
    private OnDeleteForeverClickListener onDeleteForeverClickListener;
    private boolean isTrashMode = false;

    public interface OnSelectionChangeListener {
        void onSelectionChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Object item);
    }

    public interface OnRestoreClickListener {
        void onRestoreClick(Note note);
    }

    public interface OnDeleteForeverClickListener {
        void onDeleteForeverClick(Note note);
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setOnRestoreClickListener(OnRestoreClickListener listener) {
        this.onRestoreClickListener = listener;
        this.isTrashMode = true;
    }

    public void setOnDeleteForeverClickListener(OnDeleteForeverClickListener listener) {
        this.onDeleteForeverClickListener = listener;
        this.isTrashMode = true;
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
        TextView title, description, dateTime, mood, tags;
        ImageButton btnEdit, btnDelete, btnRestore, btnDeleteForever;
        CheckBox checkBox;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
            dateTime = itemView.findViewById(R.id.textDateTime);
            mood = itemView.findViewById(R.id.textMood);
            tags = itemView.findViewById(R.id.textTags);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDeleteForever = itemView.findViewById(R.id.btnDeleteForever);
            checkBox = itemView.findViewById(R.id.checkBox);
        }

        void bind(Note note, int position) {
            title.setText(note.getTitle());
            description.setText(note.getContent());
            
            // Форматируем и отображаем дату
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(note.getCreatedAt()));
            dateTime.setText(dateStr);
            
            // Отображаем настроение
            if (note.getMood() != null && !note.getMood().isEmpty()) {
                mood.setVisibility(View.VISIBLE);
                mood.setText("Настроение: " + note.getMood());
            } else {
                mood.setVisibility(View.GONE);
            }
            
            // Отображаем теги
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                tags.setVisibility(View.VISIBLE);
                tags.setText("Теги: " + String.join(", ", note.getTags()));
            } else {
                tags.setVisibility(View.GONE);
            }

            // Настройка видимости кнопок в зависимости от режима
            if (isTrashMode) {
                btnRestore.setVisibility(View.VISIBLE);
                btnDeleteForever.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);

                btnRestore.setOnClickListener(v -> {
                    if (onRestoreClickListener != null) {
                        onRestoreClickListener.onRestoreClick(note);
                    }
                });

                btnDeleteForever.setOnClickListener(v -> {
                    if (onDeleteForeverClickListener != null) {
                        onDeleteForeverClickListener.onDeleteForeverClick(note);
                    }
                });
            } else {
                btnRestore.setVisibility(View.GONE);
                btnDeleteForever.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(context, CreateNoteActivity.class);
                    intent.putExtra("noteId", note.getId());
                    context.startActivity(intent);
                });

                btnDelete.setOnClickListener(v -> {
                    // Сначала получаем данные заметки
                    FirebaseFirestore.getInstance()
                        .collection("notes")
                        .document(note.getId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Сохраняем в корзину
                                FirebaseFirestore.getInstance()
                                    .collection("deleted_notes")
                                    .add(documentSnapshot.getData())
                                    .addOnSuccessListener(reference -> {
                                        // Удаляем из основной коллекции
                                        FirebaseFirestore.getInstance()
                                            .collection("notes")
                                            .document(note.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(context, "Заметка перемещена в корзину", Toast.LENGTH_SHORT).show();
                                                // Удаляем из списка
                                                int pos = originalList.indexOf(note);
                                                if (pos != -1) {
                                                    originalList.remove(pos);
                                                    filter(currentQuery);
                                                }
                                            })
                                            .addOnFailureListener(e -> 
                                                Toast.makeText(context, "Ошибка удаления заметки", Toast.LENGTH_SHORT).show()
                                            );
                                    })
                                    .addOnFailureListener(e -> 
                                        Toast.makeText(context, "Ошибка перемещения в корзину", Toast.LENGTH_SHORT).show()
                                    );
                            }
                        });
                });
            }

            // Обработка выделения
            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(selectedPositions.contains(position));
            
            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(position);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (!selectionMode) {
                    setSelectionMode(true);
                    toggleSelection(position);
                    return true;
                }
                return false;
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
                intent.putExtra("planId", plan.getId());
                intent.putParcelableArrayListExtra("tasks", new ArrayList<>(plan.getTasks()));
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
