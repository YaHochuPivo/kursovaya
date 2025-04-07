package com.example.kurso;

import java.util.List;

public class PlanWrapper {
    private String id;
    private List<String> tasks;

    public PlanWrapper() {}

    public PlanWrapper(String id, List<String> tasks) {
        this.id = id;
        this.tasks = tasks;
    }

    public String getId() { return id; }
    public List<String> getTasks() { return tasks; }

    public void setId(String id) { this.id = id; }
    public void setTasks(List<String> tasks) { this.tasks = tasks; }
}

