package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailsActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener, TaskAdapter.OnTaskActionListener, NavigationView.OnNavigationItemSelectedListener {

    private Group group;
    private TextView groupTitle;
    private RecyclerView tasksRecycler, filesRecycler;
    private TaskAdapter taskAdapter;
    private FileAdapter fileAdapter;
    private final List<Task> taskList = new ArrayList<>();
    private final List<TaskFile> fileList = new ArrayList<>();

    private FirebaseFirestore db;
    private java.lang.String currentUserId;
    private FirebaseAuth auth;
    private ListenerRegistration groupMetaListener, taskListener, fileListener;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        group = (Group) getIntent().getSerializableExtra("group");
        if (group == null || group.groupId == null) {
            Toast.makeText(this, "Error: Invalid Group Metadata", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        String email = user.getEmail();
        TextView emailText = navigationView.getHeaderView(0).findViewById(R.id.userEmailText);
        if (emailText != null) emailText.setText(email);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    finish();
                }
            }
        });

        groupTitle = findViewById(R.id.groupTitleText);
        groupTitle.setText(group.groupName);

        tasksRecycler = findViewById(R.id.groupTasksRecycler);
        filesRecycler = findViewById(R.id.groupFilesRecycler);

        taskAdapter = new TaskAdapter(taskList, this);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(taskAdapter);

        fileAdapter = new FileAdapter(fileList, this);
        filesRecycler.setLayoutManager(new LinearLayoutManager(this));
        filesRecycler.setAdapter(fileAdapter);

        findViewById(R.id.addGroupTaskFab).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTask.class);
            intent.putExtra("groupId", group.groupId);
            startActivity(intent);
        });

        findViewById(R.id.addGroupFileBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddFile.class);
            intent.putExtra("groupId", group.groupId);
            startActivity(intent);
        });

        // NEW: Leave Group Button Hook (Make sure to add this ID to your layout XML file!)
        Button leaveGroupBtn = findViewById(R.id.leaveGroupBtn);
        if (leaveGroupBtn != null) {
            leaveGroupBtn.setOnClickListener(v -> showLeaveGroupConfirmationDialog());
        }

        setupRealtimeListeners();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_my_tasks) {
            startActivity(new Intent(this, Home.class));
            finish();
        } else if (id == R.id.menu_group_tasks) {
            startActivity(new Intent(this, GroupsActivity.class));
            finish();
        } else if (id == R.id.menu_logout) {
            auth.signOut();
            startActivity(new Intent(this, LogIn.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // NEW: Handles the confirmation dialog and checks admin privilege rules
    private void showLeaveGroupConfirmationDialog() {
        boolean isAdmin = currentUserId.equals(group.adminId);
        String title = isAdmin ? "Delete Group Permanent?" : "Leave Group";
        String message = isAdmin
                ? "As the creator/admin, leaving will delete this group and all its tasks permanently for everyone. Proceed?"
                : "Are you sure you want to leave this group? You won't see these tasks anymore.";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(isAdmin ? "Delete Permanently" : "Leave Group", (dialog, which) -> handleLeaveGroupLogic(isAdmin))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // NEW: Executes backend database tasks safely depending on user privileges
    private void handleLeaveGroupLogic(boolean isAdmin) {
        if (isAdmin) {
            // Delete the entire document if admin drops out
            db.collection("groups").document(group.groupId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show());
        } else {
            // Atomically rip the UID cleanly from array pool
            db.collection("groups").document(group.groupId)
                    .update("members", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "You left the group", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupRealtimeListeners() {
        groupMetaListener = db.collection("groups").document(group.groupId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Group updatedGroup = snapshot.toObject(Group.class);
                        if (updatedGroup != null) {
                            this.group = updatedGroup;
                            this.group.groupId = snapshot.getId();
                            groupTitle.setText(group.groupName + " (" + group.members.size() + " Members)");
                        }
                    }
                });

        taskListener = db.collection("tasks").whereEqualTo("groupId", group.groupId)
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

        fileListener = db.collection("files").whereEqualTo("groupId", group.groupId)
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

    @Override
    public void onFileClick(TaskFile taskFile) {
        FileTasksFragment fragment = FileTasksFragment.newInstance(taskFile.fileName, taskFile.tasks, taskFile.docId);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.groupFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAddTaskToFileClick(TaskFile taskFile) {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks available in group", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] taskTitles = new String[taskList.size()];
        for (int i = 0; i < taskList.size(); i++) {
            taskTitles[i] = taskList.get(i).title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Add to " + taskFile.fileName)
                .setItems(taskTitles, (dialog, which) -> addTaskToTaskFile(taskFile, taskList.get(which)))
                .show();
    }

    private void addTaskToTaskFile(TaskFile file, Task task) {
        if (file.tasks == null) file.tasks = new ArrayList<>();
        for (Task t : file.tasks) {
            if (t.docId != null && t.docId.equals(task.docId)) {
                Toast.makeText(this, "Already in file", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        file.tasks.add(task);
        db.collection("files").document(file.docId).set(file);
    }

    @Override
    public void onDeleteFileClick(TaskFile taskFile) {
        db.collection("files").document(taskFile.docId).delete();
    }

    @Override
    public void onTaskCompletedToggle(Task task) {
        task.completed = !task.completed;
        db.collection("tasks").document(task.docId).update("completed", task.completed);
        for (TaskFile file : fileList) {
            if (file.tasks != null) {
                boolean updated = false;
                for (Task t : file.tasks) {
                    if (t.docId != null && t.docId.equals(task.docId)) {
                        t.completed = task.completed;
                        updated = true;
                    }
                }
                if (updated) db.collection("files").document(file.docId).set(file);
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
                    if (file.tasks.get(i).docId != null && file.tasks.get(i).docId.equals(task.docId)) {
                        file.tasks.remove(i);
                        removed = true;
                        break;
                    }
                }
                if (removed) db.collection("files").document(file.docId).set(file);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupMetaListener != null) groupMetaListener.remove();
        if (taskListener != null) taskListener.remove();
        if (fileListener != null) fileListener.remove();
    }
}