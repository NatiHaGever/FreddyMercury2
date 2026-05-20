package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
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
    private String currentUserId;
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

        // FIX: Display real email and username in the side menu
        View headerView = navigationView.getHeaderView(0);
        TextView userEmailText = headerView.findViewById(R.id.userEmailText);
        TextView usernameText = headerView.findViewById(R.id.usernameText);
        if (userEmailText != null) userEmailText.setText(user.getEmail());
        
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists() && usernameText != null) {
                String name = doc.getString("username");
                if (name != null) usernameText.setText(name);
            }
        });

        // Handle back press to close chat or drawer before leaving
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    onBackPressed();
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

        Button btnOpenChat = findViewById(R.id.btnOpenChat);
        if (btnOpenChat != null) {
            btnOpenChat.setOnClickListener(v -> openGroupChat());
        }

        Button leaveGroupBtn = findViewById(R.id.leaveGroupBtn);
        if (leaveGroupBtn != null) {
            leaveGroupBtn.setOnClickListener(v -> showLeaveGroupConfirmationDialog());
        }

        setupRealtimeListeners();

        // Check if we should start with the chat fragment open
        if (getIntent().getBooleanExtra("start_with_chat", false)) {
            openGroupChat();
        }
    }

    private void openGroupChat() {
        GroupChatFragment fragment = GroupChatFragment.newInstance(group.groupId);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.groupFragmentContainer, fragment)
                .addToBackStack("chat")
                .commit();
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

    @Override
    public void onTaskCompletedToggle(Task task) {
        db.collection("tasks").document(task.docId).update("completed", !task.completed);
    }

    @Override
    public void onTaskDelete(Task task) {
        db.collection("tasks").document(task.docId).delete();
    }

    @Override
    public void onTaskClick(Task task) {
        // detail view
    }

    @Override
    public void onViewImage(Task task) {
        if (task.imageUrl != null && !task.imageUrl.isEmpty()) {
            TaskImagePreviewFragment fragment = TaskImagePreviewFragment.newInstance(task.title, task.imageUrl);
            fragment.show(getSupportFragmentManager(), "image_preview");
        } else {
            Toast.makeText(this, "No image for this task", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLeaveGroupConfirmationDialog() {
        boolean isAdmin = currentUserId.equals(group.adminId);
        String title = isAdmin ? "Delete Group Permanent?" : "Leave Group";
        String message = isAdmin
                ? "As the creator, leaving will delete this group permanently for everyone. Proceed?"
                : "Are you sure you want to leave this group?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm", (dialog, which) -> handleLeaveGroupLogic(isAdmin))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLeaveGroupLogic(boolean isAdmin) {
        if (isAdmin) {
            db.collection("groups").document(group.groupId).delete()
                    .addOnSuccessListener(aVoid -> finish());
        } else {
            db.collection("groups").document(group.groupId)
                    .update("members", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> finish());
        }
    }

    private void setupRealtimeListeners() {
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
                .addToBackStack("file_tasks")
                .commit();
    }

    @Override
    public void onAddTaskToFileClick(TaskFile taskFile) {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "No group tasks available to add", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] taskTitles = new String[taskList.size()];
        for (int i = 0; i < taskList.size(); i++) {
            taskTitles[i] = taskList.get(i).title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Add task to " + taskFile.fileName)
                .setItems(taskTitles, (dialog, which) -> {
                    Task selectedTask = taskList.get(which);
                    if (taskFile.tasks == null) taskFile.tasks = new ArrayList<>();
                    
                    // Check for duplicates
                    for (Task t : taskFile.tasks) {
                        if (t.docId != null && t.docId.equals(selectedTask.docId)) {
                            Toast.makeText(this, "Already in file", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    taskFile.tasks.add(selectedTask);
                    db.collection("files").document(taskFile.docId).update("tasks", taskFile.tasks)
                            .addOnSuccessListener(aVoid -> Toast.makeText(GroupDetailsActivity.this, "Added to file!", Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    @Override
    public void onDeleteFileClick(TaskFile taskFile) {
        db.collection("files").document(taskFile.docId).delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskListener != null) taskListener.remove();
        if (fileListener != null) fileListener.remove();
    }
}
