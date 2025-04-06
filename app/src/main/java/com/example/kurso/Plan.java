package com.example.kurso;

import java.util.List;

public class Plan {
    private List<String> tasks;

    public Plan() {}

    public Plan(List<String> tasks) {
        this.tasks = tasks;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public void setTasks(List<String> tasks) {
        this.tasks = tasks;
    }
}
