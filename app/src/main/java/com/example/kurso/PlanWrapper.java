package com.example.kurso;

import java.util.List;
import java.util.ArrayList;

public class PlanWrapper {
    private String id;
    private List<TaskItem> tasks;
    private long timestamp;
    private String userId;

    public PlanWrapper() {
        this.tasks = new ArrayList<>();
    }

    public PlanWrapper(Plan plan) {
        this.id = plan.getId();
        this.tasks = plan.getTasks();
        this.timestamp = plan.getTimestamp();
        this.userId = plan.getUserId();
    }

    public PlanWrapper(String id, List<TaskItem> tasks) {
        this.id = id;
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
