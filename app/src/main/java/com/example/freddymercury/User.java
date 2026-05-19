package com.example.freddymercury;

import androidx.annotation.Keep;

@Keep
public class User {
    public String userId;
    public String username;
    public String email;

    // Required empty constructor for Firestore
    public User() {
    }

    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}