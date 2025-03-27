package com.example.kurso;

import java.util.List;

public class Note {
    private String title;
    private String content;
    private String dateTime;
    private List<String> tags;
    private String mood;

    public Note() {} // Обязательный пустой конструктор для Firestore

    public Note(String title, String content, String dateTime, List<String> tags, String mood) {
        this.title = title;
        this.content = content;
        this.dateTime = dateTime;
        this.tags = tags;
        this.mood = mood;
    }

    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDateTime() { return dateTime; }
    public List<String> getTags() { return tags; }
    public String getMood() { return mood; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setMood(String mood) { this.mood = mood; }
}
