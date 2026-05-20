package com.example.freddymercury;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddForumPostActivity extends AppCompatActivity {

    private EditText postTitleInput, postDescriptionInput;
    private Button savePostBtn;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_forum_post);

        postTitleInput = findViewById(R.id.postTitleInput);
        postDescriptionInput = findViewById(R.id.postDescriptionInput);
        savePostBtn = findViewById(R.id.savePostBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        savePostBtn.setOnClickListener(v -> savePost());
    }

    private void savePost() {
        String title = postTitleInput.getText().toString().trim();
        String description = postDescriptionInput.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Fetch username first
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            String authorName = "Anonymous";
            if (doc.exists()) {
                String name = doc.getString("username");
                if (name != null) authorName = name;
            }

            String postId = db.collection("forum").document().getId();
            ForumPost post = new ForumPost(user.getUid(), authorName, title, description);
            post.postId = postId;

            db.collection("forum").document(postId).set(post)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddForumPostActivity.this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddForumPostActivity.this, "Error posting", Toast.LENGTH_SHORT).show());
        });
    }
}
