package com.example.freddymercury;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationScheduler {

    private static final String TAG = "NotificationDebug";

    public static void scheduleTwoDayAlert(Context context, Task task) {
        Log.d(TAG, "--- scheduleTwoDayAlert TEST RIG called ---");

        if (task == null) return;

        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, AlertReceiver.class);
            String safeTitle = (task.title != null) ? task.title : "Untitled Task";
            intent.putExtra("task_title", safeTitle);

            int requestCode = (task.docId != null) ? task.docId.hashCode() : (int) System.currentTimeMillis();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // --- TEST RIG: OVERRIDE CALENDAR ---
            // Force the alarm to trigger exactly 10 seconds from right now
            long triggerInTenSeconds = System.currentTimeMillis() + 10000;

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerInTenSeconds,
                    pendingIntent
            );

            Log.d(TAG, "TEST SUCCESS: Alarm will fire in exactly 10 seconds for: " + safeTitle);

        } catch (Exception e) {
            Log.e(TAG, "Test Rig Crash", e);
        }
    }
}