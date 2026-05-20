package com.example.freddymercury;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupChatFragment extends Fragment {

    private String groupId;
    private String currentUserId;
    private String currentUsername = "User";

    private RecyclerView recyclerView;
    private ChatAdapter adapter; // Global variable is correctly declared here
    private List<ChatMessage> messageList;
    private EditText editMessage;
    private View btnSend;
    private ImageButton btnRecord;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private MediaRecorder recorder;
    private String audioFileName;
    private boolean isRecording = false;
    private long startTime;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    public static GroupChatFragment newInstance(String groupId) {
        GroupChatFragment fragment = new GroupChatFragment();
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_chat, container, false);

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUsername = doc.getString("username");
            }
        });

        recyclerView = view.findViewById(R.id.chatRecyclerView);
        editMessage = view.findViewById(R.id.editChatMessage);
        btnSend = view.findViewById(R.id.btnSendMessage);
        btnRecord = view.findViewById(R.id.btnRecordVoice);

        view.findViewById(R.id.btnBackToTasks).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        messageList = new ArrayList<>();

        // Initializing the global adapter variable without repeating the type name
        adapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        setupVoiceRecorder();
        listenForMessages();

        return view;
    }

    private void setupVoiceRecorder() {
        btnRecord.setOnClickListener(v -> {
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
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void startRecording() {
        // FIX: Replaced "temp_audio.m4a" with a UUID string.
        // If a user sent two messages rapidly, the second recording would overwrite the first locally before the cloud task finished.
        audioFileName = requireContext().getCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString() + ".m4a";

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
            btnRecord.setColorFilter(Color.RED);
            Toast.makeText(getContext(), "Recording... Tap again to stop", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("VoiceRecord", "prepare() failed", e);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            boolean wasShort = (System.currentTimeMillis() - startTime) < 1000;
            try {
                recorder.stop();
            } catch (RuntimeException stopException) {
                wasShort = true;
            }
            recorder.release();
            recorder = null;
            isRecording = false;
            btnRecord.clearColorFilter();

            if (wasShort) {
                Toast.makeText(getContext(), "Recording too short", Toast.LENGTH_SHORT).show();
                new File(audioFileName).delete();
            } else {
                uploadVoiceMessage();
            }
        }
    }

    private void uploadVoiceMessage() {
        final File audioFile = new File(audioFileName);
        if (!audioFile.exists()) return;

        Uri fileUri = Uri.fromFile(audioFile);
        String remoteFileName = UUID.randomUUID().toString() + ".m4a";
        final StorageReference storageRef = storage.getReference().child("voice_messages/" + groupId + "/" + remoteFileName);

        storageRef.putFile(fileUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            sendVoiceMessage(uri.toString());
            if (audioFile.exists()) audioFile.delete();
        }).addOnFailureListener(e -> {
            Log.e("VoiceRecord", "Upload failed: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendVoiceMessage(String audioUrl) {
        ChatMessage voiceMsg = new ChatMessage(groupId, currentUserId, currentUsername, audioUrl, true);
        db.collection("groups").document(groupId).collection("messages").add(voiceMsg);
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage message = new ChatMessage(groupId, currentUserId, currentUsername, text);
        db.collection("groups").document(groupId).collection("messages").add(message)
                .addOnSuccessListener(documentReference -> editMessage.setText(""))
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to send", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void listenForMessages() {
        db.collection("groups").document(groupId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            messageList.add(doc.toObject(ChatMessage.class));
                        }
                        adapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            recyclerView.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    // ADDED: Lifecycle hook connection to clear background audio memory leaks
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.stopAudio();
        }
    }
}