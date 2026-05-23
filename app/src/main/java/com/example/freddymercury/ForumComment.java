package com.example.freddymercury;

import androidx.annotation.Keep;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

@Keep
public class ForumComment implements Serializable {
    public String commentId;
    public String authorId;
    public String authorName;
    public String text;
    
    @ServerTimestamp
    public Date timestamp; // Use java.util.Date for Serialization compatibility

    public ForumComment() {}

    public ForumComment(String authorId, String authorName, String text) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = new Date();
    }
}
