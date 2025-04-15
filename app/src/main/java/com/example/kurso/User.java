package com.example.kurso;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class User {
    private String userId;
    private String displayName;
    private String email;
    private String bio;
    private String profileImagePath;
    private List<String> friendIds;
    private Timestamp createdAt;
    private Timestamp lastUpdated;
    private String mood;
    private List<Map<String, Object>> moodHistory;

    public User() {
        // Required empty constructor for Firestore
        friendIds = new ArrayList<>();
    }

    public User(String userId, String displayName, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.friendIds = new ArrayList<>();
        this.createdAt = Timestamp.now();
        this.lastUpdated = Timestamp.now();
    }

    // Геттеры и сеттеры
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    public List<String> getFriendIds() { return friendIds; }
    public void setFriendIds(List<String> friendIds) { this.friendIds = friendIds; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public List<Map<String, Object>> getMoodHistory() {
        return moodHistory;
    }

    public void setMoodHistory(List<Map<String, Object>> moodHistory) {
        this.moodHistory = moodHistory;
    }

    // Вспомогательные методы
    public void addFriend(String friendId) {
        if (!friendIds.contains(friendId)) {
            friendIds.add(friendId);
        }
    }

    public void removeFriend(String friendId) {
        friendIds.remove(friendId);
    }

    public boolean isFriend(String userId) {
        return friendIds.contains(userId);
    }
} 