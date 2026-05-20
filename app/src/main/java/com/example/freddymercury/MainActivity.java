package com.example.freddymercury;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This links to the XML file we just fixed
        setContentView(R.layout.activity_main);

        // PASTE THE CODE HERE:
        Button btnSignIn = findViewById(R.id.btnSignIn);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LogIn.class));
        });

        btnCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignUp.class));
        });
    }
}