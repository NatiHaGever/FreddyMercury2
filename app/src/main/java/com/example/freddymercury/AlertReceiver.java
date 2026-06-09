package com.example.freddymercury;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlertReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "task_alerts_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        String title = intent.getStringExtra("task_title");
        if (title == null) title = "Reminder";

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Do-it! Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        } //checking version of android

        Intent activityIntent = new Intent(context, Home.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE); //opening home when pressed on notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if ("chat".equals(type)) {
            String groupName = intent.getStringExtra("group_name");
            String senderName = intent.getStringExtra("sender_name");
            String message = intent.getStringExtra("message");
            builder.setContentTitle("New message in " + groupName)
                   .setContentText(senderName + ": " + message);
        } else {
            builder.setContentTitle("Task Deadline Approaching!")
                   .setContentText("\"" + title + "\" is due in 1 day.");
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
