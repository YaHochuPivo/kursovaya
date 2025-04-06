package com.example.kurso;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NOTE = 0;
    private static final int TYPE_PLAN = 1;

    private Context context;
    private List<Object> itemList;
    private FirebaseFirestore db;

    public NotesAdapter(Context context, List<Object> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = itemList.get(position);
        if (item instanceof Note) return TYPE_NOTE;
        else return TYPE_PLAN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        if (viewType == TYPE_NOTE) return new NoteViewHolder(view);
        else return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = itemList.get(position);

        if (holder instanceof NoteViewHolder && item instanceof Note) {
            Note note = (Note) item;
            NoteViewHolder h = (NoteViewHolder) holder;

            h.title.setText(note.getTitle());
            h.content.setText(note.getContent());
            h.dateTime.setText("Дата: " + note.getDateTime());

            // Настроение
            String mood = note.getMood();
            String emoji = "😶";
            if (mood != null) {
                switch (mood.toLowerCase()) {
                    case "грустный": emoji = "😢"; break;
                    case "злой": emoji = "😠"; break;
                    case "нейтральный": emoji = "😐"; break;
                    case "счастливый": emoji = "🙂"; break;
                    case "возбужденный": emoji = "😄"; break;
                }
            }
            h.mood.setText(emoji);

            // Теги
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                h.tags.setText("Теги: " + android.text.TextUtils.join(", ", note.getTags()));
            } else {
                h.tags.setText("Теги: —");
            }

            // Кнопка редактирования
            h.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, CreateNoteActivity.class);
                intent.putExtra("noteId", note.getId());
                intent.putExtra("noteTitle", note.getTitle());
                intent.putExtra("noteContent", note.getContent());
                intent.putExtra("noteDateTime", note.getDateTime());
                intent.putExtra("noteMood", note.getMood());
                intent.putStringArrayListExtra("noteTags", new ArrayList<>(note.getTags() != null ? note.getTags() : new ArrayList<>()));
                context.startActivity(intent);
            });

            // Кнопка удаления
            h.btnDelete.setOnClickListener(v -> {
                String id = note.getId();
                if (id != null && !id.isEmpty()) {
                    db.collection("notes").document(id).delete()
                            .addOnSuccessListener(aVoid -> {
                                itemList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, itemList.size());
                            });
                }
            });

        } else if (holder instanceof PlanViewHolder && item instanceof PlanWrapper) {
            PlanViewHolder h = (PlanViewHolder) holder;
            PlanWrapper plan = (PlanWrapper) item;

            h.title.setText("📅 План на день");
            h.content.setText(joinTasks(plan.getTasks()));
            h.dateTime.setText("");
            h.mood.setText("🗂");
            h.tags.setText("Задач: " + (plan.getTasks() != null ? plan.getTasks().size() : 0));

            // Редактирование плана
            h.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, DailyPlanActivity.class);
                intent.putExtra("planId", plan.getId());
                intent.putStringArrayListExtra("tasks", new ArrayList<>(plan.getTasks()));
                context.startActivity(intent);
            });

            // Удаление плана
            h.btnDelete.setOnClickListener(v -> {
                if (plan.getId() != null && !plan.getId().isEmpty()) {
                    db.collection("daily_plans").document(plan.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                itemList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, itemList.size());
                            });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    private String joinTasks(List<String> tasks) {
        StringBuilder builder = new StringBuilder();
        for (String task : tasks) {
            builder.append("• ").append(task).append("\n");
        }
        return builder.toString().trim();
    }

    // 🔹 ViewHolder для заметки
    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, dateTime, mood, tags;
        Button btnEdit, btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            content = itemView.findViewById(R.id.noteContent);
            dateTime = itemView.findViewById(R.id.noteDateTime);
            mood = itemView.findViewById(R.id.noteMood);
            tags = itemView.findViewById(R.id.noteTags);
            btnEdit = itemView.findViewById(R.id.btnEditNote);
            btnDelete = itemView.findViewById(R.id.btnDeleteNote);
        }
    }

    // 🔹 ViewHolder для плана (наследуем поля NoteViewHolder)
    public static class PlanViewHolder extends NoteViewHolder {
        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // 🔹 Обёртка для плана
    public static class PlanWrapper {
        private String id;
        private List<String> tasks;

        public PlanWrapper(String id, List<String> tasks) {
            this.id = id;
            this.tasks = tasks;
        }

        public String getId() {
            return id;
        }

        public List<String> getTasks() {
            return tasks;
        }
    }
}
