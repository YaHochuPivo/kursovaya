package com.example.kurso;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.annotation.NonNull;

public class NotificationSettingsDialog extends Dialog {
    private final NotificationHelper notificationHelper;
    private TextView timePickerLabel;
    private SwitchMaterial enableNotificationsSwitch;
    private TimePicker timePicker;
    private Button saveButton;
    private Button cancelButton;

    public NotificationSettingsDialog(@NonNull Context context) {
        super(context);
        notificationHelper = new NotificationHelper(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notification_settings);

        timePickerLabel = findViewById(R.id.timePickerLabel);
        enableNotificationsSwitch = findViewById(R.id.enableNotificationsSwitch);
        timePicker = findViewById(R.id.timePicker);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        updateUI();

        enableNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timePicker.setEnabled(isChecked);
        });

        saveButton.setOnClickListener(v -> {
            if (enableNotificationsSwitch.isChecked()) {
                notificationHelper.scheduleReminder(
                    timePicker.getHour(),
                    timePicker.getMinute()
                );
            } else {
                notificationHelper.cancelReminder();
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void updateUI() {
        boolean isEnabled = notificationHelper.isReminderEnabled();
        int hour = notificationHelper.getReminderHour();
        int minute = notificationHelper.getReminderMinute();

        enableNotificationsSwitch.setChecked(isEnabled);
        timePicker.setHour(hour);
        timePicker.setMinute(minute);
        timePicker.setEnabled(isEnabled);
    }
} 