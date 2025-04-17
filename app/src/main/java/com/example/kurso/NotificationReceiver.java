package com.example.kurso;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received notification broadcast");
        NotificationHelper notificationHelper = new NotificationHelper(context);
        NotificationHelper.showReminderNotification(context);
        
        // Планируем следующее уведомление
        int hour = notificationHelper.getReminderHour();
        int minute = notificationHelper.getReminderMinute();
        if (notificationHelper.isReminderEnabled()) {
            notificationHelper.scheduleReminder(hour, minute);
        }
    }
} 