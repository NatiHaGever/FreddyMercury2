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
        } //which xml to use here
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position); //sends data for the xml to use

        if (holder instanceof TextViewHolder) {
            TextViewHolder textHolder = (TextViewHolder) holder;
            textHolder.messageText.setText(message.messageText);
            textHolder.senderName.setText(message.senderName);
            setTime(textHolder.timeText, message);
        } else if (holder instanceof VoiceViewHolder) {
            VoiceViewHolder voiceHolder = (VoiceViewHolder) holder;
            voiceHolder.senderName.setText(message.senderName);
            setTime(voiceHolder.timeText, message);

            // Update UI if this message is currently playing
            if (position == currentlyPlayingPosition) {
                voiceHolder.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                voiceHolder.btnPlay.setImageResource(android.R.drawable.ic_media_play);
            }

            voiceHolder.btnPlay.setOnClickListener(v -> {
                // Get the ABSOLUTE LATEST version of the message from the list
                int currentPos = holder.getBindingAdapterPosition(); // finds exact position
                if (currentPos != RecyclerView.NO_POSITION) { //security check
                    ChatMessage latestMsg = messageList.get(currentPos);
                    
                    if (latestMsg.audioUrl == null || latestMsg.audioUrl.isEmpty()) {
                        Toast.makeText(v.getContext(), "Voice note is still uploading...", Toast.LENGTH_SHORT).show();
                        return;
                    } // if doesnt exists currently
                    
                    playAudio(latestMsg.audioUrl, currentPos, v.getContext()); //plays
                }
            });
        }
    }

    private void setTime(TextView timeView, ChatMessage message) {
        if (message.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeView.setText(sdf.format(message.timestamp));
        } else {
            // Show real-time indicator while syncing
            timeView.setText("..."); 
        }
    }

    private void playAudio(String url, int position, android.content.Context context) {
        if (currentlyPlayingPosition == position) {
            stopAudio();
            return;
        }

        stopAudio();

        mediaPlayer = new MediaPlayer();
        currentlyPlayingPosition = position;

        try { //prepares the app to play the audio with avoiding any errors
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync(); // prepares the player in the background
            mediaPlayer.setOnPreparedListener(MediaPlayer::start); // ready? play it
            mediaPlayer.setOnCompletionListener(mp -> stopAudio()); // finished? stop
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(context, "Cannot play audio", Toast.LENGTH_SHORT).show();
                stopAudio();
                return true;
            });
            notifyItemChanged(position);

        } catch (IOException e) {
            e.printStackTrace();
            stopAudio();
        }
    }

    public void stopAudio() { // checks if it is playing and stops
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.release(); // deletes it from memory
            mediaPlayer = null;
        }

        int oldPlayingPosition = currentlyPlayingPosition;
        currentlyPlayingPosition = -1; // back to default

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
