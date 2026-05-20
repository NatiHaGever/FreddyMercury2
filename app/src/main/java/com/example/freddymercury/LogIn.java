package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogIn extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginBtn, SignBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

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

            // Look up username document to resolve the mapped authentication email string
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            String actualEmail = querySnapshot.getDocuments().get(0).getString("email");
                            if (actualEmail != null) {
                                performFirebaseAuthLogin(actualEmail, password);
                            } else {
                                Toast.makeText(LogIn.this, "Error resolving account data.", Toast.LENGTH_SHORT).show();
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