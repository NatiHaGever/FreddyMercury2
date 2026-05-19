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

public class SignUp extends AppCompatActivity {

    EditText usernameInput, emailInput, passwordInput; // Added usernameInput
    Button signupBtn;
    Button goToLogin;

    FirebaseAuth auth;
    FirebaseFirestore db; // Added Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Bind views (Make sure R.id.usernameInput exists in your XML layout)
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signupBtn = findViewById(R.id.signupBtn);
        goToLogin = findViewById(R.id.LogIn);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        signupBtn.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // STEP 1: Check if the username is already taken by another user
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnCompleteListener(usernameTask -> {
                        if (usernameTask.isSuccessful() && usernameTask.getResult() != null) {
                            if (!usernameTask.getResult().isEmpty()) {
                                // Username already exists in database! Stop execution.
                                Toast.makeText(SignUp.this, "This username is already taken!", Toast.LENGTH_SHORT).show();
                            } else {
                                // STEP 2: Username is unique, proceed with creating Firebase Auth account
                                createNewUserAccount(username, email, password);
                            }
                        } else {
                            Toast.makeText(SignUp.this, "Database check failed. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        goToLogin.setOnClickListener(v ->
                startActivity(new Intent(SignUp.this, LogIn.class))
        );
    }

    private void createNewUserAccount(String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();

                        // STEP 3: Create profile model object to map inside Firestore
                        User newUser = new User(uid, username, email);

                        db.collection("users")
                                .document(uid)
                                .set(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignUp.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUp.this, LogIn.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SignUpError", "Failed saving profile mapping node", e);
                                    Toast.makeText(SignUp.this, "Auth created, but profile save failed.", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(SignUp.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}