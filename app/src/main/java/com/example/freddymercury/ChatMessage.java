package com.example.freddymercury;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;

@Keep
public class ChatMessage {
    public String messageId;
    public String groupId;
    public String senderId;
    public String senderName;
    public String messageText;
    public String audioUrl;    // URL for voice message in Firebase Storage
    public String messageType; // "text" or "voice"
    public Timestamp timestamp;

    public ChatMessage() {
        // Required for Firebase
    }

    // Constructor for text messages (4 args)
    public ChatMessage(String groupId, String senderId, String senderName, String messageText) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageText = messageText;
        this.messageType = "text";
        this.timestamp = Timestamp.now();
    }

    // Constructor for voice messages (5 args)
    public ChatMessage(String groupId, String senderId, String senderName, String audioUrl, boolean isVoice) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.audioUrl = audioUrl;
        this.messageType = "voice";
        this.timestamp = Timestamp.now();
    }
}
