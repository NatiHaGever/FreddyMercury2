package com.example.freddymercury;

public class Task {

    public String title;
    public String dueDate;
    public String userId;
    public boolean completed;
    public Task() {}
    public String docId;

    public Task(String title, String dueDate, String userId) {
        this.title = title;
        this.dueDate = dueDate;
        this.userId = userId;
        this.completed = false;
        docId="";
    }
}
