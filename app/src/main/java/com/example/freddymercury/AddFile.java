package com.example.freddymercury;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddFile extends AppCompatActivity {

    EditText fileNameInput;
    Button saveFileBtn;

    String groupId = null;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        groupId = getIntent().getStringExtra("groupId");

        fileNameInput = findViewById(R.id.fileNameInput);
        saveFileBtn = findViewById(R.id.saveFileBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        saveFileBtn.setOnClickListener(v -> saveFile());
    }

    private void saveFile() {
        String fileName = fileNameInput.getText().toString().trim();

        if (fileName.isEmpty()) {
            Toast.makeText(this, "Please enter a file name", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        TaskFile taskFile = new TaskFile(fileName, userId);
        if (groupId != null) {
            taskFile.groupId = groupId;
        }

        db.collection("files")
                .add(taskFile)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "File created successfully", Toast.LENGTH_SHORT).show();
                    finish(); // This correctly returns you to the Group or Home screen
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show());
    }
}
