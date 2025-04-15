package com.example.kurso;

public class Gift {
    private String giftId;
    private String fromUserId;
    private String fromUserDisplayName;
    private String toUserId;
    private String giftType;
    private long timestamp;

    public Gift() {
        // Required empty constructor for Firestore
    }

    public Gift(String fromUserId, String fromUserDisplayName, String toUserId, String giftType) {
        this.fromUserId = fromUserId;
        this.fromUserDisplayName = fromUserDisplayName;
        this.toUserId = toUserId;
        this.giftType = giftType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getGiftId() { return giftId; }
    public void setGiftId(String giftId) { this.giftId = giftId; }
    
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    
    public String getFromUserDisplayName() { return fromUserDisplayName; }
    public void setFromUserDisplayName(String fromUserDisplayName) { this.fromUserDisplayName = fromUserDisplayName; }
    
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    
    public String getGiftType() { return giftType; }
    public void setGiftType(String giftType) { this.giftType = giftType; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 