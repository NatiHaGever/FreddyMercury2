package com.example.freddymercury;

import androidx.annotation.Keep;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

@Keep
public class ForumPost implements Serializable {
    public String postId;
    public String authorId;
    public String authorName;
    public String title;
    public String description;
    
    @ServerTimestamp
    public Date timestamp;

    public ForumPost() {}

    public ForumPost(String authorId, String authorName, String title, String description) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.description = description;
        this.timestamp = new Date();
    }
}
