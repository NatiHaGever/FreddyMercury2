package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity implements FileAdapter.OnFileClickListener, TaskAdapter.OnTaskActionListener {

    TextView todayDateText;
    RecyclerView tasksRecycler, filesRecycler;
    FloatingActionButton addTask;
    Button logoutBtn, deleteAllBtn, FileBtn;

    FirebaseFirestore db;
    FirebaseAuth auth;

    TaskAdapter taskAdapter;
    List<Task> taskList;

    FileAdapter fileAdapter;
    List<TaskFile> fileList;

    ListenerRegistration taskListener, fileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        todayDateText = findViewById(R.id.todayDateText);
        tasksRecycler = findViewById(R.id.tasksRecycler);
        filesRecycler = findViewById(R.id.filesRecycler);
        addTask = findViewById(R.id.addTaskFab);
        logoutBtn = findViewById(R.id.logoutBtn);
        deleteAllBtn = findViewById(R.id.deleteAllBtn);
        FileBtn = findViewById(R.id.FileBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        todayDateText.setText("Today: " + today);

        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(Home.this, LogIn.class));
            finish();
        });

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(taskAdapter);

        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this);
        // Changed to vertical scrolling
        filesRecycler.setLayoutManager(new LinearLayoutManager(this));
        filesRecycler.setAdapter(fileAdapter);

        addTask.setOnClickListener(v -> startActivity(new Intent(Home.this, AddTask.class)));
        FileBtn.setOnClickListener(v -> startActivity(new Intent(Home.this, AddFile.class)));

        deleteAllBtn.setOnClickListener(v -> showDeleteAllConfirmation());

        setupRealtimeListeners();
    }

    private void setupRealtimeListeners() {
        String userId = auth.getCurrentUser().getUid();

        taskListener = db.collection("tasks").whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        taskList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Task t = doc.toObject(Task.class);
                            t.docId = doc.getId();
                            taskList.add(t);
                        }
                        taskAdapter.notifyDataSetChanged();
                    }
                });

        fileListener = db.collection("files").whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        fileList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            TaskFile f = doc.toObject(TaskFile.class);
                            f.docId = doc.getId();
                            fileList.add(f);
                        }
                        fileAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All")
                .setMessage("Are you sure you want to delete everything?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String userId = auth.getCurrentUser().getUid();
                    WriteBatch batch = db.batch();

                    db.collection("tasks").whereEqualTo("userId", userId).get().addOnSuccessListener(query -> {
                        for (QueryDocumentSnapshot doc : query) batch.delete(doc.getReference());
                        db.collection("files").whereEqualTo("userId", userId).get().addOnSuccessListener(queryFiles -> {
                            for (QueryDocumentSnapshot doc : queryFiles) batch.delete(doc.getReference());
                            batch.commit().addOnSuccessListener(aVoid -> 
                                Toast.makeText(Home.this, "Everything deleted", Toast.LENGTH_SHORT).show());
                        });
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onFileClick(TaskFile taskFile) {
        FileTasksFragment fragment = FileTasksFragment.newInstance(taskFile.fileName, taskFile.tasks, taskFile.docId);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAddTaskToFileClick(TaskFile taskFile) {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks available to add", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] taskTitles = new String[taskList.size()];
        for (int i = 0; i < taskList.size(); i++) {
            taskTitles[i] = taskList.get(i).title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Add to " + taskFile.fileName)
                .setItems(taskTitles, (dialog, which) -> {
                    addTaskToTaskFile(taskFile, taskList.get(which));
                })
                .show();
    }

    @Override
    public void onDeleteFileClick(TaskFile taskFile) {
        db.collection("files").document(taskFile.docId).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show());
    }

    private void addTaskToTaskFile(TaskFile file, Task task) {
        if (file.tasks == null) file.tasks = new ArrayList<>();
        
        for (Task t : file.tasks) {
            if (t.docId.equals(task.docId)) {
                Toast.makeText(this, "Task already in file", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        file.tasks.add(task);
        db.collection("files").document(file.docId).set(file)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onTaskCompletedToggle(Task task) {
        task.completed = !task.completed;
        db.collection("tasks").document(task.docId).update("completed", task.completed);
        
        for (TaskFile file : fileList) {
            if (file.tasks != null) {
                boolean updated = false;
                for (Task t : file.tasks) {
                    if (t.docId.equals(task.docId)) {
                        t.completed = task.completed;
                        updated = true;
                    }
                }
                if (updated) {
                    db.collection("files").document(file.docId).set(file);
                }
            }
        }
    }

    @Override
    public void onTaskDelete(Task task) {
        db.collection("tasks").document(task.docId).delete();

        for (TaskFile file : fileList) {
            if (file.tasks != null) {
                boolean removed = false;
                for (int i = 0; i < file.tasks.size(); i++) {
                    if (file.tasks.get(i).docId.equals(task.docId)) {
                        file.tasks.remove(i);
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    db.collection("files").document(file.docId).set(file);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
        if (fileListener != null) fileListener.remove();
    }
}
