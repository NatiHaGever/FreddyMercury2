package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<TaskFile> fileList;
    private final OnFileClickListener listener;

    // Interface לטיפול בלחיצות על קובץ
    public interface OnFileClickListener {
        void onFileClick(TaskFile taskFile);
    }

    public FileAdapter(List<TaskFile> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // שים לב: אתה צריך ליצור קובץ XML בשם file_item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        TaskFile currentFile = fileList.get(position);
        holder.fileName.setText(currentFile.fileName);

        // מציג כמה משימות יש בתוך הקובץ
        int taskCount = currentFile.tasks != null ? currentFile.tasks.size() : 0;
        holder.fileCount.setText(taskCount + " Tasks");

        // הגדרת הלחיצה על כל התיקייה
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(currentFile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileCount;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileCount = itemView.findViewById(R.id.fileCount);
        }
    }
}