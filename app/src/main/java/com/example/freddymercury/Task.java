package com.example.freddymercury;

import androidx.annotation.Keep;

@Keep
public class Task {
    public String docId;       // Used for Firestore's Document ID
    public String title;
    public String dueDate;
    public String userId;
    public String description;
    public boolean completed;
    public String groupId;     // <-- MUST BE PUBLIC FOR FIRESTORE MAPS
    public String imageUrl;    // <-- Added for ImgBB storage URLs

    // 1. Required empty constructor for Firestore
    public Task() {
    }

    // 2. The constructor your AddTask activity uses
    public Task(String title, String dueDate, String userId, String description) {
        this.title = title;
        this.dueDate = dueDate;
        this.userId = userId;
        this.description = description;
        this.completed = false;
        this.groupId = "";
        this.imageUrl = ""; // Default to empty string
    }
}