package com.example.freddymercury;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AddTask extends AppCompatActivity {

    EditText taskTitleInput;
    TextView dueDateText;
    Button saveTaskBtn;
    EditText taskDescription;

    String selectedDate = "";
    String groupId = null;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Extract the group identifier passed from GroupDetailsActivity
        groupId = getIntent().getStringExtra("groupId");
        Log.d("TaskDebug", "AddTask opened with Group ID: " + groupId);

        taskDescription = findViewById(R.id.editDescription);
        taskTitleInput = findViewById(R.id.taskTitleInput);
        dueDateText = findViewById(R.id.dueDateText);
        saveTaskBtn = findViewById(R.id.saveTaskBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        dueDateText.setOnClickListener(v -> showDatePicker());
        saveTaskBtn.setOnClickListener(v -> saveTask());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    selectedDate = day + "/" + (month + 1) + "/" + year;
                    dueDateText.setText(selectedDate);
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveTask() {
        String title = taskTitleInput.getText().toString().trim();
        String desc = taskDescription.getText().toString().trim();

        if (title.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User session not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // 1. Construct the basic task instance data
        Task task = new Task(title, selectedDate, userId, desc);

        // 2. Explicitly stamp the task with the target Group ID if available
        if (groupId != null && !groupId.isEmpty()) {
            task.groupId = groupId;
            Log.d("TaskDebug", "Stamping task with Group ID: " + groupId);
        } else {
            task.groupId = "personal";
            Log.w("TaskDebug", "No Group ID found. Task marked as personal.");
        }

        // 3. Write data to the global "tasks" collection tree
        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(doc -> {
                    Log.d("TaskDebug", "Task document saved successfully with ID: " + doc.getId());
                    Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
                    finish(); // Exits activity and returns to GroupDetailsActivity
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskDebug", "Error writing task to Firestore: ", e);
                    Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show();
                });
    }
}