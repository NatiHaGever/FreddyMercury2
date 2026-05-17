package com.example.freddymercury;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TaskFile implements Serializable {
    public String fileName;
    public String icon;
    public List<Task> tasks;
    public String userId;
    public String docId;
    public String groupId; // New field for shared files

    public TaskFile() {
        // Required for Firebase
    }

    public TaskFile(String fileName, String userId) {
        this.fileName = fileName;
        this.userId = userId;
        this.tasks = new ArrayList<>();
        this.icon = "📁"; // Default icon
        this.groupId = null; // Default to personal
    }
}
