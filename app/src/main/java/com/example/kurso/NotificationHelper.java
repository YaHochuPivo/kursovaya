package com.example.kurso;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.util.Log;
import java.util.Calendar;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Напоминания";
    private static final String CHANNEL_DESCRIPTION = "Напоминания о создании заметок";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String REMINDER_ENABLED_KEY = "reminder_enabled";
    private static final String REMINDER_HOUR_KEY = "reminder_hour";
    private static final String REMINDER_MINUTE_KEY = "reminder_minute";

    private final Context context;
    private final SharedPreferences prefs;

    public NotificationHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        createNotificationChannel(context);
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void scheduleReminder(int hour, int minute) {
        // Сохраняем настройки
        prefs.edit()
            .putBoolean(REMINDER_ENABLED_KEY, true)
            .putInt(REMINDER_HOUR_KEY, hour)
            .putInt(REMINDER_MINUTE_KEY, minute)
            .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Устанавливаем время для уведомления
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Если время уже прошло, добавляем день
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Устанавливаем ежедневное повторение
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
            Log.d(TAG, "Reminder scheduled for " + hour + ":" + minute);
        }
    }

    public void cancelReminder() {
        // Отключаем напоминания в настройках
        prefs.edit()
            .putBoolean(REMINDER_ENABLED_KEY, false)
            .apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Reminder cancelled");
        }
    }

    public static void showReminderNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Время для новой заметки!")
            .setContentText("Как прошел ваш день? Запишите свои мысли и настроение")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public boolean isReminderEnabled() {
        return prefs.getBoolean(REMINDER_ENABLED_KEY, false);
    }

    public int getReminderHour() {
        return prefs.getInt(REMINDER_HOUR_KEY, 20); // По умолчанию 20:00
    }

    public int getReminderMinute() {
        return prefs.getInt(REMINDER_MINUTE_KEY, 0);
    }
} 