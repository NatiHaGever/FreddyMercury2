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
        if (task == null || task.dueDate == null || task.dueDate.isEmpty()) return;

        try {
            // Task dueDate format: "d/M/yyyy"
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

            long triggerTime = calendar.getTimeInMillis();

            // If the alert time is in the past, don't schedule it
            if (triggerTime <= System.currentTimeMillis()) {
                Log.d(TAG, "Skipping alert for " + task.title + " - time already passed.");
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, AlertReceiver.class);
            intent.putExtra("task_title", task.title);

            // Use docId hash as unique request code for this task
            int requestCode = (task.docId != null) ? task.docId.hashCode() : (int) System.currentTimeMillis();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );

            Log.d(TAG, "Alert scheduled for: " + calendar.getTime().toString() + " for task: " + task.title);

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling alert", e);
        }
    }
}
