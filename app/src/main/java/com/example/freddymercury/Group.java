package com.example.freddymercury;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Keep
public class Group implements Serializable {
    public String groupId;
    public String groupName;
    public String groupCode;
    public String adminId;
    public List<Task> taskList = new ArrayList<>();     // Initialized to prevent NullPointerException
    public List<TaskFile> fileList = new ArrayList<>(); // FIXED: Changed from File to TaskFile
    public List<String> members = new ArrayList<>();

    public Group() {
        // Required for Firebase
    }

    public Group(String groupId, String groupName, String groupCode, String adminId) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupCode = groupCode;
        this.adminId = adminId;
        this.members = new ArrayList<>();
        this.members.add(adminId);
        this.taskList = new ArrayList<>();
        this.fileList = new ArrayList<>();
    }
}