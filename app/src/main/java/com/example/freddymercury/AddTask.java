package com.example.freddymercury;



import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    Button Back;

    String selectedDate = "";

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        taskTitleInput = findViewById(R.id.taskTitleInput);
        dueDateText = findViewById(R.id.dueDateText);
        saveTaskBtn = findViewById(R.id.saveTaskBtn);
        Back=findViewById(R.id.backBtn);
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

        if (title.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Task task = new Task(title, selectedDate, userId);

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
                    finish(); // חזרה למסך הבית
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show());
    }



}
