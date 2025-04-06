package com.example.kurso;

import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content;
    private String dateTime;
    private String mood;
    private List<String> tags;
    private String userId; // 🔹 Добавлен userId для импорта и синхронизации

    public Note() {
        // Обязательный пустой конструктор для Firestore
    }

    public Note(String id, String title, String content, String dateTime, String mood, List<String> tags, String userId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.dateTime = dateTime;
        this.mood = mood;
        this.tags = tags;
        this.userId = userId;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDateTime() { return dateTime; }
    public String getMood() { return mood; }
    public List<String> getTags() { return tags; }
    public String getUserId() { return userId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setMood(String mood) { this.mood = mood; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUserId(String userId) { this.userId = userId; }
}
