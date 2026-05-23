package com.example.freddymercury;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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

public class GroupChatFragment extends Fragment {

    private String groupId;
    private String currentUserId;
    private String currentUsername = "User";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
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

    private ListenerRegistration chatListener;

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
                if (checkPermissions()) startRecording();
                else requestPermissions();
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
            Toast.makeText(getContext(), "Recording...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("VoiceRecord", "prepare() failed", e);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            boolean wasShort = (System.currentTimeMillis() - startTime) < 1000;
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                wasShort = true;
            }
            recorder.release();
            recorder = null;
            isRecording = false;
            btnRecord.clearColorFilter();

            if (wasShort) {
                Toast.makeText(getContext(), "Too short", Toast.LENGTH_SHORT).show();
                new File(audioFileName).delete();
            } else {
                prepareRealtimeVoiceMessage();
            }
        }
    }

    private void prepareRealtimeVoiceMessage() {
        // 1. Create the database entry IMMEDIATELY so it shows in the list right away
        final String messageId = db.collection("groups").document(groupId).collection("messages").document().getId();
        final ChatMessage placeholder = new ChatMessage(groupId, currentUserId, currentUsername, "Sending voice...", true);
        placeholder.messageId = messageId;
        placeholder.audioUrl = ""; // Special value to show "loading" state in UI

        db.collection("groups").document(groupId).collection("messages").document(messageId).set(placeholder)
                .addOnSuccessListener(aVoid -> uploadAudioAndFinalize(messageId));
    }

    private void uploadAudioAndFinalize(String messageId) {
        final File audioFile = new File(audioFileName);
        if (!audioFile.exists()) return;

        final StorageReference storageRef = storage.getReference().child("voice_messages/" + groupId + "/" + audioFile.getName());

        storageRef.putFile(Uri.fromFile(audioFile)).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            // 2. Update the existing message with the real URL
            db.collection("groups").document(groupId).collection("messages").document(messageId)
                    .update("audioUrl", uri.toString(), "messageText", "Voice Message");
            audioFile.delete();
        }).addOnFailureListener(e -> {
            // 3. Cleanup placeholder if upload failed
            db.collection("groups").document(groupId).collection("messages").document(messageId).delete();
            Toast.makeText(getContext(), "Voice send failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage message = new ChatMessage(groupId, currentUserId, currentUsername, text);
        db.collection("groups").document(groupId).collection("messages").add(message)
                .addOnSuccessListener(documentReference -> editMessage.setText(""));
    }

    private void listenForMessages() {
        Query query = db.collection("groups").document(groupId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        // MetadataChanges.INCLUDE ensures we see local writes (our own messages) instantly
        chatListener = query.addSnapshotListener(MetadataChanges.INCLUDE, (value, error) -> {
            if (error != null) return;
            if (value != null) {
                messageList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    messageList.add(doc.toObject(ChatMessage.class));
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListener != null) chatListener.remove();
        if (adapter != null) adapter.stopAudio();
    }
}
