package com.example.kurso;

import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content;
    private Timestamp timestamp;
    private String mood;
    private List<String> tags;
    private String userId; // 🔹 Добавлен userId для импорта и синхронизации
    private long createdAt; // Добавляем поле для времени создания

    public Note() {
        // Пустой конструктор для Firebase
        this.createdAt = System.currentTimeMillis();
    }

    public Note(String id, String title, String content, String mood, List<String> tags, String userId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.tags = tags;
        this.userId = userId;
        this.timestamp = Timestamp.now();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getText() { return content; } // Алиас для совместимости
    public Timestamp getTimestamp() { return timestamp; }
    public String getMood() { return mood; }
    public List<String> getTags() { return tags; }
    public String getUserId() { return userId; }
    public long getCreatedAt() { return createdAt; } // Геттер для времени создания

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setText(String text) { this.content = text; } // Алиас для совместимости
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setMood(String mood) { this.mood = mood; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; } // Сеттер для времени создания

    // Вспомогательные методы для работы с датами
    public Date getDate() {
        return timestamp != null ? timestamp.toDate() : new Date();
    }

    public void setDate(Date date) {
        this.timestamp = date != null ? new Timestamp(date) : Timestamp.now();
        this.createdAt = date != null ? date.getTime() : System.currentTimeMillis();
    }
}
