package com.example.freddymercury;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onTaskCompletedToggle(Task task);
        void onTaskDelete(Task task);
        void onTaskClick(Task task);
        void onViewImage(Task task); // Added for the preview button
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
        Context context = holder.itemView.getContext();

        holder.title.setText(t.title);
        holder.description.setText(t.description);
        holder.date.setText("Due: " + t.dueDate);

        // Load image with Glide if it exists
        if (t.imageUrl != null && !t.imageUrl.isEmpty()) {
            holder.taskUploadedImage.setVisibility(View.VISIBLE);
            holder.viewImageBtn.setVisibility(View.VISIBLE); // Show button if image exists
            Glide.with(context)
                    .load(t.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.taskUploadedImage);
        } else {
            holder.taskUploadedImage.setVisibility(View.GONE);
            holder.viewImageBtn.setVisibility(View.GONE); // Hide button if no image
        }

        if (t.completed) {
            holder.itemContainer.setBackgroundColor(context.getResources().getColor(R.color.task_done_bg));
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.title.setTextColor(context.getResources().getColor(R.color.text_muted));
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.completedBtn.setText("Undo");
        } else {
            holder.itemContainer.setBackgroundColor(context.getResources().getColor(android.R.color.white));
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.title.setTextColor(context.getResources().getColor(R.color.text_primary));
            holder.statusBadge.setVisibility(View.GONE);
            holder.completedBtn.setText("Complete");
        }

        holder.itemContainer.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });

        holder.completedBtn.setOnClickListener(v -> {
            if (listener != null) listener.onTaskCompletedToggle(t);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onTaskDelete(t);
        });

        holder.viewImageBtn.setOnClickListener(v -> {
            if (listener != null) listener.onViewImage(t);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View itemContainer;
        TextView title, date, description, statusBadge;
        ImageView taskUploadedImage;
        Button completedBtn, deleteBtn, viewImageBtn;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            title = itemView.findViewById(R.id.taskTitle);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            date = itemView.findViewById(R.id.taskDate);
            completedBtn = itemView.findViewById(R.id.completedBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            viewImageBtn = itemView.findViewById(R.id.viewImageBtn);
            description = itemView.findViewById(R.id.Description);
            taskUploadedImage = itemView.findViewById(R.id.taskUploadedImage);
        }
    }
}
