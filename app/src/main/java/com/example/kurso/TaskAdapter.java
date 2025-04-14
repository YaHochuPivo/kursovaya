package com.example.kurso;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<TaskItem> tasks;
    private final Context context;
    private OnTaskStatusChangeListener statusChangeListener;

    public interface OnTaskStatusChangeListener {
        void onTaskStatusChanged(int position, boolean isChecked);
    }

    public void setOnTaskStatusChangeListener(OnTaskStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public TaskAdapter(Context context, List<TaskItem> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = tasks.get(position);

        holder.editTask.setText(task.getText());
        holder.textTime.setText(task.getTime());
        holder.checkCompleted.setChecked(task.isDone());

        holder.editTask.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.setText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        holder.checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setDone(isChecked);
            if (statusChangeListener != null) {
                statusChangeListener.onTaskStatusChanged(position, isChecked);
            }
        });

        // ✅ Выбор времени
        holder.timeContainer.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute1) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                        task.setTime(selectedTime);
                        holder.textTime.setText(selectedTime);
                    },
                    hour, minute, true
            );
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public List<TaskItem> getTasks() {
        return tasks;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        EditText editTask;
        TextView textTime;
        CheckBox checkCompleted;
        View timeContainer;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            editTask = itemView.findViewById(R.id.editTaskText);
            textTime = itemView.findViewById(R.id.textTime);
            checkCompleted = itemView.findViewById(R.id.checkCompleted);
            timeContainer = itemView.findViewById(R.id.timeContainer); // 🔹 должен существовать в item_task.xml
        }
    }
}
