package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity implements FileAdapter.OnFileClickListener, TaskAdapter.OnTaskActionListener, NavigationView.OnNavigationItemSelectedListener {

    TextView todayDateText;
    RecyclerView tasksRecycler, filesRecycler;
    FloatingActionButton addTask;
    Button deleteAllBtn, FileBtn;

    FirebaseFirestore db;
    FirebaseAuth auth;

    TaskAdapter taskAdapter;
    List<Task> taskList;

    FileAdapter fileAdapter;
    List<TaskFile> fileList;

    ListenerRegistration taskListener, fileListener;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LogIn.class));
            finish();
            return;
        }

        // --- Toolbar Setup (Moved down to avoid camera) ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // FIX: Display real email and username in the side menu header
        updateMenuHeader(navigationView, user);

        todayDateText = findViewById(R.id.todayDateText);
        tasksRecycler = findViewById(R.id.tasksRecycler);
        filesRecycler = findViewById(R.id.filesRecycler);
        addTask = findViewById(R.id.addTaskFab);
        deleteAllBtn = findViewById(R.id.deleteAllBtn);
        FileBtn = findViewById(R.id.FileBtn);

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        todayDateText.setText("Today: " + today);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(taskAdapter);

        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this);
        filesRecycler.setLayoutManager(new LinearLayoutManager(this));
        filesRecycler.setAdapter(fileAdapter);

        addTask.setOnClickListener(v -> startActivity(new Intent(Home.this, AddTask.class)));
        FileBtn.setOnClickListener(v -> startActivity(new Intent(Home.this, AddFile.class)));
        deleteAllBtn.setOnClickListener(v -> showDeleteAllConfirmation());

        setupRealtimeListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    private void updateMenuHeader(NavigationView navigationView, FirebaseUser user) {
        View headerView = navigationView.getHeaderView(0);
        TextView emailText = headerView.findViewById(R.id.userEmailText);
        TextView usernameText = headerView.findViewById(R.id.usernameText);
        
        if (emailText != null) emailText.setText(user.getEmail());
        
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && usernameText != null) {
                String name = doc.getString("username");
                if (name != null) usernameText.setText(name);
            }
        });
    }

    private void setupRealtimeListeners() {
        String userId = auth.getCurrentUser().getUid();

        taskListener = db.collection("tasks").whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        taskList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Task t = doc.toObject(Task.class);
                            t.docId = doc.getId();
                            if (t.groupId == null || t.groupId.isEmpty() || t.groupId.equals("personal")) {
                                taskList.add(t);
                            }
                        }
                        taskAdapter.notifyDataSetChanged();
                    }
                });

        fileListener = db.collection("files").whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        fileList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            TaskFile f = doc.toObject(TaskFile.class);
                            f.docId = doc.getId();
                            if (f.groupId == null || f.groupId.isEmpty()) {
                                fileList.add(f);
                            }
                        }
                        fileAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_group_tasks) {
            startActivity(new Intent(this, GroupsActivity.class));
        } else if (id == R.id.menu_forum) {
            startActivity(new Intent(this, ForumActivity.class));
        } else if (id == R.id.menu_logout) {
            logout();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        auth.signOut();
        startActivity(new Intent(Home.this, LogIn.class));
        finish();
    }

    @Override
    public void onTaskClick(Task task) {}

    @Override
    public void onViewImage(Task task) {
        if (task.imageUrl != null && !task.imageUrl.isEmpty()) {
            TaskImagePreviewFragment fragment = TaskImagePreviewFragment.newInstance(task.title, task.imageUrl);
            fragment.show(getSupportFragmentManager(), "image_preview");
        }
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
                .setTitle("Insert task into " + taskFile.fileName)
                .setItems(taskTitles, (dialog, which) -> {
                    Task selectedTask = taskList.get(which);
                    // Use FieldValue.arrayUnion to safely add the task object to the array in Firestore
                    db.collection("files").document(taskFile.docId)
                            .update("tasks", FieldValue.arrayUnion(selectedTask))
                            .addOnSuccessListener(aVoid -> Toast.makeText(Home.this, "Inserted!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(Home.this, "Error inserting task", Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    @Override
    public void onDeleteFileClick(TaskFile taskFile) {
        db.collection("files").document(taskFile.docId).delete();
    }

    @Override
    public void onTaskCompletedToggle(Task task) {
        db.collection("tasks").document(task.docId).update("completed", !task.completed);
    }

    @Override
    public void onTaskDelete(Task task) {
        db.collection("tasks").document(task.docId).delete();
        NotificationScheduler.cancelAlert(this, task.docId);
    }

    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All")
                .setMessage("Are you sure you want to delete everything? This action is permanent.")
                .setPositiveButton("Yes, Delete All", (dialog, which) -> {
                    String userId = auth.getCurrentUser().getUid();
                    WriteBatch batch = db.batch();

                    db.collection("tasks").whereEqualTo("userId", userId).get().addOnSuccessListener(queryTasks -> {
                        for (QueryDocumentSnapshot doc : queryTasks) {
                            if (doc.getString("groupId") == null || doc.getString("groupId").equals("personal")) {
                                batch.delete(doc.getReference());
                                NotificationScheduler.cancelAlert(Home.this, doc.getId());
                            }
                        }
                        
                        db.collection("files").whereEqualTo("userId", userId).get().addOnSuccessListener(queryFiles -> {
                            for (QueryDocumentSnapshot doc : queryFiles) {
                                if (doc.getString("groupId") == null) {
                                    batch.delete(doc.getReference());
                                }
                            }
                            
                            batch.commit().addOnSuccessListener(aVoid -> 
                                Toast.makeText(Home.this, "All personal data cleared", Toast.LENGTH_SHORT).show());
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
        if (fileListener != null) fileListener.remove();
    }
}
