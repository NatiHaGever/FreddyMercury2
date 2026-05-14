package com.example.freddymercury;

import java.util.ArrayList;
import java.util.List;

public class TaskFile {
    public String fileName;
    public String icon;
    public List<Task> tasks;
    public String userId;
    public String docId;

    public TaskFile() {
        // Required for Firebase
    }

    public TaskFile(String fileName, String userId) {
        this.fileName = fileName;
        this.userId = userId;
        this.tasks = new ArrayList<>();
        this.icon = "📁"; // Default icon
    }
}
