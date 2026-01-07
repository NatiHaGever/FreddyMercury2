package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUp extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button signupBtn;
    Button goToLogin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signupBtn = findViewById(R.id.signupBtn);
        goToLogin = findViewById(R.id.LogIn);

        auth = FirebaseAuth.getInstance();




        signupBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }


            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUp.this, LogIn.class));
                            finish();
                        } else {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        goToLogin.setOnClickListener(v ->
                startActivity(new Intent(SignUp.this, LogIn.class))
        );
    }
}
