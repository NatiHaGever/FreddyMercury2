package com.example.freddymercury;

import java.util.ArrayList;
import java.util.List;

public class TaskFile {
    public String fileName;
    public String icon;
    public List<Task> tasks;

    public TaskFile(String fileName) {
        this.fileName = fileName;
        this.tasks = new ArrayList<>();
    }
}