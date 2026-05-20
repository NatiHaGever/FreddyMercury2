package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ForumActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView forumRecycler;
    private FloatingActionButton addPostFab;
    private ForumAdapter adapter;
    private List<ForumPost> postList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration forumListener;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("How to Do-it?");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update header info with real user data
        updateMenuHeader(navigationView, user);

        forumRecycler = findViewById(R.id.forumRecycler);
        addPostFab = findViewById(R.id.addPostFab);

        postList = new ArrayList<>();
        adapter = new ForumAdapter(postList, post -> {
            Intent intent = new Intent(ForumActivity.this, ForumPostDetailActivity.class);
            intent.putExtra("post", post);
            startActivity(intent);
        });

        forumRecycler.setLayoutManager(new LinearLayoutManager(this));
        forumRecycler.setAdapter(adapter);

        addPostFab.setOnClickListener(v -> startActivity(new Intent(ForumActivity.this, AddForumPostActivity.class)));

        setupRealtimeListener();

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

    private void setupRealtimeListener() {
        forumListener = db.collection("forum")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ForumPost post = doc.toObject(ForumPost.class);
                            post.postId = doc.getId();
                            postList.add(post);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
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
    protected void onDestroy() {
        super.onDestroy();
        if (forumListener != null) forumListener.remove();
    }
}
