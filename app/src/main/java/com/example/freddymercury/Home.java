package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity {

    TextView todayDateText;
    RecyclerView tasksRecycler;
    FloatingActionButton addTask;
    Button logoutBtn, deleteAllBtn;

    FirebaseFirestore db;
    FirebaseAuth auth;
    TaskAdapter adapter;
    List<Task> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        todayDateText = findViewById(R.id.todayDateText);
        tasksRecycler = findViewById(R.id.tasksRecycler);
        addTask = findViewById(R.id.addTaskFab);
        logoutBtn = findViewById(R.id.logoutBtn);
        deleteAllBtn = findViewById(R.id.deleteAllBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // תאריך היום
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        todayDateText.setText("Today: " + today);

        // Logout
        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(Home.this, LogIn.class));
            finish();
        });

        // RecyclerView
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(adapter);

        // Add Task
        addTask.setOnClickListener(v ->
                startActivity(new Intent(Home.this, AddTask.class))
        );

        // Delete All
        deleteAllBtn.setOnClickListener(v -> {
            String userId = auth.getCurrentUser().getUid();
            db.collection("tasks").whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (QueryDocumentSnapshot doc : query) {
                            db.collection("tasks").document(doc.getId()).delete();
                        }
                        taskList.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(Home.this, "All tasks deleted", Toast.LENGTH_SHORT).show();
                    });
        });

        loadTasksFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromFirestore();
    }

    private void loadTasksFromFirestore() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("tasks").whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        taskList.clear();
                        for(QueryDocumentSnapshot doc : task.getResult()){
                            Task t = doc.toObject(Task.class);
                            t.docId = doc.getId();
                            taskList.add(t);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(Home.this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
