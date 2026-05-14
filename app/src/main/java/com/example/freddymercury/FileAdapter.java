package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<TaskFile> fileList;
    private final OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(TaskFile taskFile);
        void onAddTaskToFileClick(TaskFile taskFile);
    }

    public FileAdapter(List<TaskFile> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        TaskFile currentFile = fileList.get(position);
        holder.fileName.setText(currentFile.fileName);

        int taskCount = currentFile.tasks != null ? currentFile.tasks.size() : 0;
        holder.fileCount.setText(taskCount + " Tasks");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(currentFile);
            }
        });

        holder.AddTaskBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddTaskToFileClick(currentFile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileCount;
        Button AddTaskBtn;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            AddTaskBtn = itemView.findViewById(R.id.AddTaskBtn);
            fileName = itemView.findViewById(R.id.fileName);
            fileCount = itemView.findViewById(R.id.fileCount);
        }
    }
}
