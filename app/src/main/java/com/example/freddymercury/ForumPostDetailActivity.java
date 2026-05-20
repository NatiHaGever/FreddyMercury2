package com.example.freddymercury;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ForumPostDetailActivity extends AppCompatActivity {

    private TextView titleText, authorText, descriptionText;
    private RecyclerView commentsRecycler;
    private EditText commentInput;
    private ImageButton btnSendComment;

    private ForumPost post;
    private CommentAdapter adapter;
    private List<ForumComment> commentList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_post_detail);

        post = (ForumPost) getIntent().getSerializableExtra("post");
        if (post == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Post Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        titleText = findViewById(R.id.detailPostTitle);
        authorText = findViewById(R.id.detailPostAuthor);
        descriptionText = findViewById(R.id.detailPostDescription);
        commentsRecycler = findViewById(R.id.commentsRecycler);
        commentInput = findViewById(R.id.commentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        titleText.setText(post.title);
        authorText.setText("Asked by: " + post.authorName);
        descriptionText.setText(post.description);

        commentList = new ArrayList<>();
        adapter = new CommentAdapter(commentList);
        commentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        commentsRecycler.setAdapter(adapter);

        btnSendComment.setOnClickListener(v -> sendComment());

        loadComments();
    }

    private void sendComment() {
        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Clear input early for better UX
        commentInput.setText("");

        db.collection("users").document(user.getUid()).get().addOnSuccessListener(doc -> {
            String authorName = "Anonymous";
            if (doc.exists()) {
                String name = doc.getString("username");
                if (name != null) authorName = name;
            }

            ForumComment comment = new ForumComment(user.getUid(), authorName, text);
            db.collection("forum").document(post.postId).collection("comments").add(comment)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Comment added!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error adding comment", Toast.LENGTH_SHORT).show();
                        commentInput.setText(text);
                    });
        });
    }

    private void loadComments() {
        db.collection("forum").document(post.postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        commentList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ForumComment comment = doc.toObject(ForumComment.class);
                            comment.commentId = doc.getId();
                            commentList.add(comment);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
