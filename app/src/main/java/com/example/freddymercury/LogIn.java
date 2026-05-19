package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogIn extends AppCompatActivity {

    EditText usernameInput, passwordInput; // Changed emailInput to usernameInput
    Button loginBtn;
    Button SignBtn;

    FirebaseAuth auth;
    FirebaseFirestore db; // Added Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        // Bind fields (Make sure R.id.usernameInput matches your XML modification)
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        SignBtn = findViewById(R.id.SignUp);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SignBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LogIn.this, SignUp.class);
            startActivity(intent);
        });

        loginBtn.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // STEP 1: Query Firestore to find the email tied to this username
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // STEP 2: Extract the mapped email address from the found document
                            String actualEmail = querySnapshot.getDocuments().get(0).getString("email");

                            if (actualEmail != null) {
                                // STEP 3: Pass the resolved email and password to Firebase Auth
                                performFirebaseAuthLogin(actualEmail, password);
                            }
                        } else {
                            Toast.makeText(LogIn.this, "Username not found!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LoginError", "Firestore resolution query failed", e);
                        Toast.makeText(LogIn.this, "Database connection error.", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void performFirebaseAuthLogin(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LogIn.this, Home.class));
                        finish();
                    } else {
                        Toast.makeText(LogIn.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}