package com.example.freddymercury;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messageList;
    private final String currentUserId;

    private static final int VIEW_TYPE_TEXT_SENT = 1;
    private static final int VIEW_TYPE_TEXT_RECEIVED = 2;
    private static final int VIEW_TYPE_VOICE_SENT = 3;
    private static final int VIEW_TYPE_VOICE_RECEIVED = 4;

    private MediaPlayer mediaPlayer;
    // CRUCIAL: Track which item is playing right now
    private int currentlyPlayingPosition = -1;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        boolean isMe = message.senderId.equals(currentUserId);
        boolean isVoice = "voice".equals(message.messageType);

        if (isMe) {
            return isVoice ? VIEW_TYPE_VOICE_SENT : VIEW_TYPE_TEXT_SENT;
        } else {
            return isVoice ? VIEW_TYPE_VOICE_RECEIVED : VIEW_TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_TEXT_SENT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
                return new TextViewHolder(view);
            case VIEW_TYPE_TEXT_RECEIVED:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
                return new TextViewHolder(view);
            case VIEW_TYPE_VOICE_SENT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_voice_sent, parent, false);
                return new VoiceViewHolder(view);
            case VIEW_TYPE_VOICE_RECEIVED:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_voice_received, parent, false);
                return new VoiceViewHolder(view);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof TextViewHolder) {
            TextViewHolder textHolder = (TextViewHolder) holder;
            textHolder.messageText.setText(message.messageText);
            textHolder.senderName.setText(message.senderName);
            setTime(textHolder.timeText, message);
        } else if (holder instanceof VoiceViewHolder) {
            VoiceViewHolder voiceHolder = (VoiceViewHolder) holder;
            voiceHolder.senderName.setText(message.senderName);
            setTime(voiceHolder.timeText, message);

            // UI UPDATE: Check if this specific row is the one playing right now
            if (position == currentlyPlayingPosition) {
                voiceHolder.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                voiceHolder.btnPlay.setImageResource(android.R.drawable.ic_media_play);
            }

            voiceHolder.btnPlay.setOnClickListener(v -> {
                // Pass position so we can selectively reload this row's UI layout
                playAudio(message.audioUrl, position, v.getContext());
            });
        }
    }

    private void setTime(TextView timeView, ChatMessage message) {
        if (message.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeView.setText(sdf.format(message.timestamp.toDate()));
        }
    }

    private void playAudio(String url, int position, android.content.Context context) {
        if (url == null || url.isEmpty()) return;

        // TACTIC: If the user clicks the pause button on the memo that is ALREADY playing, stop it.
        if (currentlyPlayingPosition == position) {
            stopAudio();
            return;
        }

        // Clean slate: Stop any other running audio track
        stopAudio();

        mediaPlayer = new MediaPlayer();
        currentlyPlayingPosition = position;

        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);

            // Clean up back to play icons when track runs out completely
            mediaPlayer.setOnCompletionListener(mp -> stopAudio());

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(context, "Cannot play audio", Toast.LENGTH_SHORT).show();
                stopAudio();
                return true;
            });

            // Update this item's look right away to show it's loading/playing
            notifyItemChanged(position);

        } catch (IOException e) {
            e.printStackTrace();
            stopAudio();
        }
    }

    // Public method to shut everything down cleanly
    public void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        int oldPlayingPosition = currentlyPlayingPosition;
        currentlyPlayingPosition = -1;

        // Refresh the old running item so its pause icon swaps back to play
        if (oldPlayingPosition != -1) {
            notifyItemChanged(oldPlayingPosition);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, timeText;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            senderName = itemView.findViewById(R.id.text_message_name);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
    }

    static class VoiceViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, timeText;
        ImageButton btnPlay;

        public VoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.text_message_name);
            timeText = itemView.findViewById(R.id.text_message_time);
            btnPlay = itemView.findViewById(R.id.btnPlayVoice);
        }
    }
}