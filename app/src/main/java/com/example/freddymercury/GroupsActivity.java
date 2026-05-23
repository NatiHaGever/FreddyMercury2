package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Random;

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

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LogIn.class));
            finish();
            return;
        }

        // Toolbar Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Group Tasks");
        }

        // Side Menu Setup
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update real user info in the menu header
        updateMenuHeader(navigationView, user);

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

        groupsRecycler = findViewById(R.id.groupsRecycler);
        createGroupBtn = findViewById(R.id.createGroupBtn);
        joinGroupBtn = findViewById(R.id.joinGroupBtn);

        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            showGroupOptionsDialog(group);
        });

        groupsRecycler.setLayoutManager(new LinearLayoutManager(this));
        groupsRecycler.setAdapter(adapter);

        createGroupBtn.setOnClickListener(v -> showCreateGroupDialog());
        joinGroupBtn.setOnClickListener(v -> showJoinGroupDialog());

        loadUserGroups();
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

    private void showGroupOptionsDialog(Group group) {
        String[] options = {"📋 Tasks & Files", "💬 Group Chat"};
        new AlertDialog.Builder(this)
                .setTitle(group.groupName)
                .setItems(options, (dialog, which) -> {
                    Intent intent = new Intent(GroupsActivity.this, GroupDetailsActivity.class);
                    intent.putExtra("group", group);
                    if (which == 1) intent.putExtra("start_with_chat", true);
                    startActivity(intent);
                })
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_my_tasks) {
            startActivity(new Intent(this, Home.class));
            finish();
        } else if (id == R.id.menu_forum) {
            startActivity(new Intent(this, ForumActivity.class));
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
        db.collection("groups").whereArrayContains("members", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        groupList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Group g = doc.toObject(Group.class);
                            g.groupId = doc.getId();
                            groupList.add(g);
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
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) createGroup(name);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createGroup(String name) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        String customGroupId = db.collection("groups").document().getId();
        String groupCode = generateRandomCode(6);
        Group newGroup = new Group(customGroupId, name, groupCode, user.getUid());
        db.collection("groups").document(customGroupId).set(newGroup)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Group Created! Code: " + groupCode, Toast.LENGTH_LONG).show());
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join Group");
        final EditText input = new EditText(this);
        input.setHint("Enter 6-digit Code");
        input.setPadding(40, 40, 40, 40);
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
        if (user == null) return;
        db.collection("groups").whereEqualTo("groupCode", code).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Group group = doc.toObject(Group.class);
                            if (group.members != null && group.members.contains(user.getUid())) {
                                Toast.makeText(this, "Already a member", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("groups").document(doc.getId())
                                        .update("members", FieldValue.arrayUnion(user.getUid()))
                                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Joined Group!", Toast.LENGTH_SHORT).show());
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid group code!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
