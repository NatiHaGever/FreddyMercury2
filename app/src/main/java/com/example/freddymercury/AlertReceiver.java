package com.example.freddymercury;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class AlertReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "task_alerts_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. THIS PROVES THE ALARM TRIGGERED!
        Log.d("AlertDebug", "🔔 ALARM WOKE UP THE APP! Executing receiver...");
        Toast.makeText(context, "ALARM TRIGGERED IN BACKGROUND!", Toast.LENGTH_LONG).show();

        String taskTitle = intent.getStringExtra("task_title");
        if (taskTitle == null) taskTitle = "A task";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for tasks due in 2 days");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Task Deadline Approaching!")
                .setContentText("\"" + taskTitle + "\" is due in 2 days.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        Log.d("AlertDebug", "🔔 NOTIFICATION SENT TO ANDROID OS!");
    }
}