package com.example.freddymercury;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {

    private String groupId;
    private String currentUsername = "Anonymous";

    private EditText editChatMessage;
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private ImageButton btnRecordVoice;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private ListenerRegistration chatListener;

    private MediaRecorder recorder;
    private String audioFileName;
    private boolean isRecording = false;
    private long startTime;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");

        if (groupId == null || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Invalid Chat Context", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(groupName != null ? groupName : "Group Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        editChatMessage = findViewById(R.id.editChatMessage);
        Button btnSendChat = findViewById(R.id.btnSendChat);
        btnRecordVoice = findViewById(R.id.btnRecordVoice);
        chatRecycler = findViewById(R.id.chatRecycler);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecycler.setLayoutManager(layoutManager);
        chatRecycler.setAdapter(chatAdapter);

        fetchCurrentUsername();
        btnSendChat.setOnClickListener(v -> sendMessage());

        setupVoiceRecorder();
        listenForMessages();
    }

    private void fetchCurrentUsername() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("username");
                        if (name != null) currentUsername = name;
                    }
                });
    }

    private void setupVoiceRecorder() {
        btnRecordVoice.setOnClickListener(v -> {
            if (!isRecording) {
                if (checkPermissions()) {
                    startRecording();
                } else {
                    requestPermissions();
                }
            } else {
                stopRecording();
            }
        });
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void startRecording() {
        audioFileName = getCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString() + ".m4a";
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(audioFileName);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            startTime = System.currentTimeMillis();
            btnRecordVoice.setColorFilter(Color.RED);
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("VoiceRecord", "prepare() failed", e);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            long duration = System.currentTimeMillis() - startTime;
            boolean wasShort = duration < 1000;
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                wasShort = true;
            }
            recorder.release();
            recorder = null;
            isRecording = false;
            btnRecordVoice.clearColorFilter();

            if (wasShort) {
                Toast.makeText(this, "Too short", Toast.LENGTH_SHORT).show();
                new File(audioFileName).delete();
            } else {
                uploadVoiceMessage();
            }
        }
    }

    private void uploadVoiceMessage() {
        final File audioFile = new File(audioFileName);
        if (!audioFile.exists() || audioFile.length() == 0) return;

        Uri fileUri = Uri.fromFile(audioFile); //converting to an url
        String remoteFileName = UUID.randomUUID().toString() + ".m4a"; // so 2 people wont ruin each other msg at the same time
        final StorageReference storageRef = storage.getReference().child("voice_messages/" + groupId + "/" + remoteFileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> { // requests to upload
                        sendVoiceMessage(uri.toString());  //uploads the file to db
                        audioFile.delete(); //deletes from memory
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void sendVoiceMessage(String audioUrl) {
        String uid = auth.getCurrentUser().getUid();
        ChatMessage voiceMsg = new ChatMessage(groupId, uid, currentUsername, audioUrl, true);
        db.collection("groups").document(groupId).collection("messages").add(voiceMsg);
    }

    private void listenForMessages() {
        Query chatQuery = db.collection("groups").document(groupId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        // FIX: Include Metadata Changes to see "local" messages instantly
        chatListener = chatQuery.addSnapshotListener(MetadataChanges.INCLUDE, (value, error) -> {
            if (error != null) return;
            if (value != null) {
                messageList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    ChatMessage message = doc.toObject(ChatMessage.class);
                    message.messageId = doc.getId();
                    messageList.add(message);
                }
                chatAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecycler.scrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    private void sendMessage() {
        String text = editChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        String uid = auth.getCurrentUser().getUid();
        ChatMessage newMessage = new ChatMessage(groupId, uid, currentUsername, text);
        editChatMessage.setText("");
        db.collection("groups").document(groupId).collection("messages").add(newMessage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatAdapter != null) chatAdapter.stopAudio();
        if (chatListener != null) chatListener.remove();
    }
}
