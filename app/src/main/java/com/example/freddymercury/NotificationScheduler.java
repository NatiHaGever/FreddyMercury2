package com.example.freddymercury;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationScheduler {

    private static final String TAG = "NotificationDebug";

    public static void scheduleTwoDayAlert(Context context, Task task) {
        if (task == null || task.dueDate == null || task.dueDate.isEmpty() || task.docId == null) return;

        try {
            // Task dueDate format: "d/M/yyyy" (e.g. 25/12/2023)
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Date date = sdf.parse(task.dueDate);
            if (date == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // Set alert for 2 days before the due date
            calendar.add(Calendar.DAY_OF_YEAR, -2);
            
            // Set notification time to 9:00 AM on that day
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long triggerTime = calendar.getTimeInMillis();

            // If the alert time is in the past, don't schedule it
            if (triggerTime <= System.currentTimeMillis()) {
                Log.d(TAG, "Skipping alert - time already passed for: " + task.title);
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, AlertReceiver.class);
            intent.putExtra("task_title", task.title);
            intent.putExtra("task_id", task.docId);

            // Use docId hash as unique request code
            int requestCode = task.docId.hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // FIX: Using setAndAllowWhileIdle instead of setExactAndAllowWhileIdle
            // This avoids the strict SecurityException and permission requirement on Android 12+
            // while still allowing the alarm to fire during battery-saving (Doze) modes.
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            Log.d(TAG, "Alert scheduled for: " + calendar.getTime().toString() + " for task: " + task.title);

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alert", e);
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
            Log.d(TAG, "Alert cancelled for: " + docId);
        }
    }
}
