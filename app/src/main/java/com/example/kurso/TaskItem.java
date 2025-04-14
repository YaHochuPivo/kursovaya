package com.example.kurso;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskItem implements Parcelable {
    private String text;
    private String time;
    private boolean done;

    public TaskItem() {
        this.text = "";
        this.time = "";
        this.done = false;
    }

    public TaskItem(String text, String time, boolean done) {
        this.text = text;
        this.time = time;
        this.done = done;
    }

    protected TaskItem(Parcel in) {
        text = in.readString();
        time = in.readString();
        done = in.readByte() != 0;
    }

    public static final Creator<TaskItem> CREATOR = new Creator<TaskItem>() {
        @Override
        public TaskItem createFromParcel(Parcel in) {
            return new TaskItem(in);
        }

        @Override
        public TaskItem[] newArray(int size) {
            return new TaskItem[size];
        }
    };

    public String getText() { return text; }
    public String getTime() { return time; }
    public boolean isDone() { return done; }

    public void setText(String text) { this.text = text; }
    public void setTime(String time) { this.time = time; }
    public void setDone(boolean done) { this.done = done; }

    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(time);
        dest.writeByte((byte) (done ? 1 : 0));
    }

    public static List<TaskItem> fromFirestoreList(List<Map<String, Object>> rawTasks) {
        List<TaskItem> tasks = new ArrayList<>();
        if (rawTasks == null) return tasks;

        for (Map<String, Object> taskMap : rawTasks) {
            String text = (String) taskMap.get("text");
            String time = (String) taskMap.get("time");
            Boolean done = (Boolean) taskMap.get("done");
            if (text != null && time != null) {
                tasks.add(new TaskItem(text, time, done != null && done));
            }
        }
        return tasks;
    }
}
