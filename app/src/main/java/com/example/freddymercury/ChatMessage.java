package com.example.freddymercury;

import androidx.annotation.Keep;
import java.io.Serializable;
import java.util.Date;

@Keep
public class ChatMessage implements Serializable {
    public String messageId;
    public String groupId;
    public String senderId;
    public String senderName;
    public String messageText;
    public String audioUrl;
    public String messageType; // "text" or "voice"
    public Date timestamp; // Use Date without ServerTimestamp for instant real-time sorting

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
        this.timestamp = new Date(); // Set locally for immediate UI update
    }

    // Constructor for voice messages (5 args)
    public ChatMessage(String groupId, String senderId, String senderName, String audioUrl, boolean isVoice) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.audioUrl = audioUrl;
        this.messageType = "voice";
        this.timestamp = new Date(); // Set locally for immediate UI update
    }
}
