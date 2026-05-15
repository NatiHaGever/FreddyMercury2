package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskCompletedToggle(Task task);
        void onTaskDelete(Task task);
    }

    public TaskAdapter(List<Task> tasks, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task t = tasks.get(position);
        holder.title.setText(t.title);
        holder.description.setText(t.description);
        holder.date.setText("Due: " + t.dueDate);
        holder.completedBtn.setText(t.completed ? "Undo" : "Done");

        holder.completedBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskCompletedToggle(t);
            }
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, description;
        Button completedBtn, deleteBtn;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.taskTitle);
            date = itemView.findViewById(R.id.taskDate);
            completedBtn = itemView.findViewById(R.id.completedBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            description = itemView.findViewById(R.id.Description);
        }
    }
}
