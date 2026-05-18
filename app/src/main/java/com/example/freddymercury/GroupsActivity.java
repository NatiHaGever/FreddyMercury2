package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Group Tasks");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            TextView emailText = navigationView.getHeaderView(0).findViewById(R.id.userEmailText);
            if (emailText != null) emailText.setText(email);
        }

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
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        // Generate document ID first to explicitly match internal property
        String customGroupId = db.collection("groups").document().getId();
        String groupCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Group newGroup = new Group(customGroupId, name, groupCode, userId);

        db.collection("groups").document(customGroupId).set(newGroup)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group Created!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show());
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join Group");
        final EditText input = new EditText(this);
        input.setHint("Enter 6-digit Group Code");
        builder.setView(input);
        builder.setPositiveButton("Join", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!code.isEmpty()) joinGroup(code);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void joinGroup(String code) {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("groups").whereEqualTo("groupCode", code).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        String targetGroupId = doc.getId();

                        // Use arrayUnion to push member without overwriting existing data
                        db.collection("groups").document(targetGroupId)
                                .update("members", FieldValue.arrayUnion(userId))
                                .addOnSuccessListener(aVoid -> Toast.makeText(GroupsActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(GroupsActivity.this, "Error joining group", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Invalid group code!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show());
    }
}