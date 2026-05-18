package com.example.freddymercury;

import android.content.res.ColorStateList;
import android.graphics.Paint;
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
        // Updated to use "item_task" to match your customized item layout filename
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

        // Context instance for fetching colors dynamically
        var context = holder.itemView.getContext();

        // High-Visibility "Done" State Logic
        if (t.completed) {
            // 1. Change background container to highly visible soft green accent
            holder.itemContainer.setBackgroundColor(context.getResources().getColor(R.color.task_done_bg));

            // 2. Strike through the title and gray it out
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.title.setTextColor(context.getResources().getColor(R.color.text_muted));

            // 3. Reveal the green status badge
            holder.statusBadge.setVisibility(View.VISIBLE);

            // 4. Update actionable button UI state
            holder.completedBtn.setText("Undo");
            holder.completedBtn.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.text_muted)));
        } else {
            // Reset to default active task state
            holder.itemContainer.setBackgroundColor(context.getResources().getColor(android.R.color.white));

            // Remove strike-through styling
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.title.setTextColor(context.getResources().getColor(R.color.text_primary));

            // Hide the status badge
            holder.statusBadge.setVisibility(View.GONE);

            // Reset button to standard Emerald Green accent accent profile
            holder.completedBtn.setText("Complete");
            holder.completedBtn.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.colorAccent)));
        }

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
        View itemContainer;
        TextView title, date, description, statusBadge;
        Button completedBtn, deleteBtn;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            title = itemView.findViewById(R.id.taskTitle);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            date = itemView.findViewById(R.id.taskDate);
            completedBtn = itemView.findViewById(R.id.completedBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            description = itemView.findViewById(R.id.Description);
        }
    }
}