package com.example.freddymercury;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileTasksFragment extends Fragment implements TaskAdapter.OnTaskActionListener {

    private String fileName;
    private String fileDocId;
    private final List<Task> taskList = new ArrayList<>();
    private TaskAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration fileListener;

    public FileTasksFragment() {
        // Required empty public constructor
    }

    public static FileTasksFragment newInstance(String fileName, List<Task> tasks, String fileDocId) {
        FileTasksFragment fragment = new FileTasksFragment();
        Bundle args = new Bundle();
        args.putString("fileName", fileName);
        args.putString("fileDocId", fileDocId);
        args.putSerializable("tasks", (Serializable) tasks);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_tasks, container, false);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            fileName = getArguments().getString("fileName");
            fileDocId = getArguments().getString("fileDocId");
            List<Task> initialTasks = (List<Task>) getArguments().getSerializable("tasks");
            if (initialTasks != null) {
                taskList.clear();
                taskList.addAll(initialTasks);
            }
        }

        TextView title = view.findViewById(R.id.fragmentFileTitle);
        RecyclerView recyclerView = view.findViewById(R.id.fragmentTasksRecycler);
        Button closeBtn = view.findViewById(R.id.closeFragmentBtn);

        title.setText("Tasks in: " + fileName);

        adapter = new TaskAdapter(taskList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        closeBtn.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        startListening();

        return view;
    }

    private void startListening() {
        if (fileDocId == null || fileDocId.isEmpty()) return;

        fileListener = db.collection("files").document(fileDocId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) return; //
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        TaskFile file = documentSnapshot.toObject(TaskFile.class);
                        if (file != null && file.tasks != null) { // Ensure tasks is not null and if not, updates the list
                            taskList.clear();
                            taskList.addAll(file.tasks);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public void onTaskCompletedToggle(Task task) {
        task.completed = !task.completed;

        db.collection("files").document(fileDocId).update("tasks", taskList)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Sync failed", Toast.LENGTH_SHORT).show());

        if (task.docId != null && !task.docId.isEmpty()) {
            db.collection("tasks").document(task.docId).update("completed", task.completed); //updates outside the file
        }
    }

    @Override
    public void onTaskDelete(Task task) {
        Task toRemove = null;
        for (Task t : taskList) {
            if ((t.docId != null && t.docId.equals(task.docId)) || t.title.equals(task.title)) {
                toRemove = t;
                break;
            }
        }

        if (toRemove != null) {
            taskList.remove(toRemove);
            db.collection("files").document(fileDocId).update("tasks", taskList)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Removed from file", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onTaskClick(Task task) {
    }

    @Override
    public void onViewImage(Task task) {
        if (task.imageUrl != null && !task.imageUrl.isEmpty()) {
            TaskImagePreviewFragment fragment = TaskImagePreviewFragment.newInstance(task.title, task.imageUrl);
            fragment.show(getParentFragmentManager(), "image_preview");
        } else {
            Toast.makeText(getContext(), "No image for this task", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fileListener != null) {
            fileListener.remove();
        }
    }
}