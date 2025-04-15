package com.example.kurso;

public class MoodEntry {
    private long timestamp;
    private float moodValue;
    private String userId;

    // Пустой конструктор для Firebase
    public MoodEntry() {}

    public MoodEntry(long timestamp, float moodValue, String userId) {
        this.timestamp = timestamp;
        this.moodValue = moodValue;
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getMoodValue() {
        return moodValue;
    }

    public void setMoodValue(float moodValue) {
        this.moodValue = moodValue;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
} 