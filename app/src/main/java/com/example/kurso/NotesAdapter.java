package com.example.kurso;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private Context context;
    private List<Note> noteList;

    public NotesAdapter(Context context, List<Note> noteList) {
        this.context = context;
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // Устанавливаем заголовок и содержимое
        holder.title.setText(note.getTitle() != null ? note.getTitle() : "Без заголовка");
        holder.content.setText(note.getContent() != null ? note.getContent() : "Без содержимого");

        // Отображение даты
        holder.dateTime.setText(note.getDateTime() != null ? "Дата: " + note.getDateTime() : "Дата не указана");

        // Настроение (иконки)
        String mood = note.getMood();
        if (mood != null) {
            switch (mood) {
                case "sad": holder.mood.setText("😢"); break;
                case "angry": holder.mood.setText("😠"); break;
                case "neutral": holder.mood.setText("😐"); break;
                case "happy": holder.mood.setText("🙂"); break;
                case "excited": holder.mood.setText("😄"); break;
                default: holder.mood.setText("");
            }
        } else {
            holder.mood.setText(""); // Настроение не выбрано
        }

        // Теги
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            holder.tags.setText("Теги: " + android.text.TextUtils.join(", ", note.getTags()));
        } else {
            holder.tags.setText("Теги: нет");
        }
    }

    @Override
    public int getItemCount() {
        return noteList != null ? noteList.size() : 0;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, dateTime, mood, tags;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            content = itemView.findViewById(R.id.noteContent);
            dateTime = itemView.findViewById(R.id.noteDateTime);
            mood = itemView.findViewById(R.id.noteMood);
            tags = itemView.findViewById(R.id.noteTags);
        }
    }
}
