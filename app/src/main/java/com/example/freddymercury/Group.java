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
    public List<String> members = new ArrayList<>(); // Fixed: Always initialize to prevent NPE

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
    }
}
