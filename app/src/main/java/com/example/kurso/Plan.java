package com.example.kurso;

import java.util.List;
import java.util.ArrayList;

public class Plan {
    private String id;
    private List<TaskItem> tasks;
    private long timestamp;
    private String userId;

    public Plan() {
        // Пустой конструктор для Firebase
    }

    public Plan(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TaskItem> getTasks() {
        return tasks != null ? tasks : new ArrayList<>();
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
