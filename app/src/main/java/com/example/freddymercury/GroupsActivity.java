package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    RecyclerView groupsRecycler;
    Button createGroupBtn, joinGroupBtn;
    GroupAdapter adapter;
    List<Group> groupList;

    FirebaseFirestore db;
    FirebaseAuth auth;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Group Tasks");
        }

        // --- Side Menu (Navigation Drawer) Setup ---
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update header info
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            TextView emailText = navigationView.getHeaderView(0).findViewById(R.id.userEmailText);
            if (emailText != null) emailText.setText(email);
        }

        // Modern way to handle back press
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

        groupsRecycler = findViewById(R.id.groupsRecycler);
        createGroupBtn = findViewById(R.id.createGroupBtn);
        joinGroupBtn = findViewById(R.id.joinGroupBtn);

        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(GroupsActivity.this, GroupDetailsActivity.class);
            intent.putExtra("group", group);
            startActivity(intent);
        });

        groupsRecycler.setLayoutManager(new LinearLayoutManager(this));
        groupsRecycler.setAdapter(adapter);

        createGroupBtn.setOnClickListener(v -> showCreateGroupDialog());
        joinGroupBtn.setOnClickListener(v -> showJoinGroupDialog());

        loadUserGroups();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_my_tasks) {
            startActivity(new Intent(this, Home.class));
            finish();
        } else if (id == R.id.menu_group_tasks) {
            // Already here
        } else if (id == R.id.menu_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        auth.signOut();
        startActivity(new Intent(GroupsActivity.this, LogIn.class));
        finish();
    }

    private void loadUserGroups() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        db.collection("groups").whereArrayContains("members", userId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        groupList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            groupList.add(doc.toObject(Group.class));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Group");
        final EditText input = new EditText(this);
        input.setHint("Group Name");
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) createGroup(name);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createGroup(String name) {
        String groupId = UUID.randomUUID().toString();
        String groupCode = groupId.replace("-", "").substring(0, 6).toUpperCase();
        String adminId = auth.getCurrentUser().getUid();
        Group newGroup = new Group(groupId, name, groupCode, adminId);
        db.collection("groups").document(groupId).set(newGroup)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group Created! Code: " + groupCode, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join Group");
        final EditText input = new EditText(this);
        input.setHint("Enter Group Code");
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.setView(input);
        builder.setPositiveButton("Join", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!code.isEmpty()) joinGroup(code);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void joinGroup(String code) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e("GroupDebug", "No authenticated user session found!");
            return;
        }
        String userId = user.getUid();

        // 1. Process code via a local temporary variable
        String tempCode = code.toUpperCase().trim();
        if (tempCode.startsWith("CODE:")) {
            tempCode = tempCode.replace("CODE:", "").trim();
        }

        // 2. Lock it into a final variable so lambdas can safely compile
        final String cleanCode = tempCode;
        Log.d("GroupDebug", "Querying Firestore for code: '" + cleanCode + "'");

        db.collection("groups").whereEqualTo("groupCode", cleanCode).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("GroupDebug", "Response received. Document match count: " + queryDocumentSnapshots.size());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Group group = doc.toObject(Group.class);

                            if (group.members != null && group.members.contains(userId)) {
                                Toast.makeText(this, "Already a member", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("groups").document(doc.getId())
                                        .update("members", FieldValue.arrayUnion(userId))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("GroupDebug", "Successfully appended User UID to array.");
                                            Toast.makeText(this, "Joined Group!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("GroupDebug", "Failed to update array: ", e);
                                            Toast.makeText(this, "Join failed", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    } else {
                        Log.w("GroupDebug", "Firestore returned empty records for code: " + cleanCode);
                        Toast.makeText(this, "Invalid Code: " + cleanCode, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GroupDebug", "Query failed completely: ", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}