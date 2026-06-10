package com.example.freddymercury;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationScheduler {

    private static final String TAG = "NotificationDebug";

    /**
     * Schedules a notification reminder 1 day before the task's due date.
     * Uses setAndAllowWhileIdle for reliability across all Android versions.
     */
    public static void scheduleTaskAlert(Context context, Task task) {
        if (task == null || task.dueDate == null || task.dueDate.isEmpty() || task.docId == null) return;

        try {
            // MATCHING: Use "d/M/yyyy" to match AddTask.java's date picker output
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Date date = sdf.parse(task.dueDate);
            if (date == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Set alert for 1 day before the due date
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            
            // Set notification time to 9:00 AM on that day
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long triggerTime = calendar.getTimeInMillis();

            // If the alert time is already in the past, don't schedule it
            if (triggerTime <= System.currentTimeMillis()) {
                Log.d(TAG, "Skipping alert for '" + task.title + "' - time already passed.");
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, AlertReceiver.class);
            intent.putExtra("type", "task");
            intent.putExtra("task_title", task.title);
            intent.putExtra("task_id", task.docId);

            // Unique request code based on task document ID hash
            int requestCode = task.docId.hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast( // asks to save it and send it when it should
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE for Android 12+ and security
            );

            // On Android 12+, inexact alarms with setAndAllowWhileIdle do NOT require 
            // the SCHEDULE_EXACT_ALARM permission, avoiding SecurityExceptions.
            alarmManager.setAndAllowWhileIdle( // sends the notification
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            Log.d(TAG, "Alert scheduled for: " + calendar.getTime().toString() + " (Task: " + task.title + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alert: " + e.getMessage());
        }
    }

    public static void cancelAlert(Context context, String docId) {
        if (docId == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                docId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Notification alert cancelled for task ID: " + docId);
        }
    }
}
