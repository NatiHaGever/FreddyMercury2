package com.example.freddymercury;

import androidx.annotation.Keep;
import java.io.Serializable;

@Keep
public class Task implements Serializable {
    public String docId;
    public String title;
    public String dueDate;
    public String userId;
    public String description;
    public boolean completed;
    public String groupId;
    public String imageUrl;

    // Fixed: Corrected constructor name from User() to Task()
    public Task() {
    }

    public Task(String title, String dueDate, String userId, String description) {
        this.title = title;
        this.dueDate = dueDate;
        this.userId = userId;
        this.description = description;
        this.completed = false;
        this.groupId = "personal";
        this.imageUrl = "";
    }
}
