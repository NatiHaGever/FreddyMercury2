package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final FirebaseFirestore db;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
        this.db = FirebaseFirestore.getInstance();
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
        holder.date.setText("Due: " + t.dueDate);
        holder.completedBtn.setText(t.completed ? "Undo" : "Done");

        // סמן / בטל
        holder.completedBtn.setOnClickListener(v -> {
            t.completed = !t.completed;
            db.collection("tasks").document(t.docId).update("completed", t.completed);
            notifyItemChanged(position);
        });

        // מחק משימה
        holder.deleteBtn.setOnClickListener(v -> {
            db.collection("tasks").document(t.docId).delete();
            tasks.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        Button completedBtn, deleteBtn;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.taskTitle);
            date = itemView.findViewById(R.id.taskDate);
            completedBtn = itemView.findViewById(R.id.completedBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}
