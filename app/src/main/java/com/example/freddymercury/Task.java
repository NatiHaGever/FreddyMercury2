package com.example.freddymercury;

import java.io.Serializable;

public class Task implements Serializable {

    public String title;
    public String dueDate;
    public String userId;
    public String description;
    public boolean completed;
    public String docId;
    public String groupId; // New field for shared tasks

    public Task() {}

    public Task(String title, String dueDate, String userId, String description) {
        this.title = title;
        this.dueDate = dueDate;
        this.userId = userId;
        this.completed = false;
        this.description = description;
        this.docId = "";
        this.groupId = null; // Default to personal
    }
}
