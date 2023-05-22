package com.example.reminderhms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcast extends BroadcastReceiver {
    public static final String CHANNEL_ID = "notifyReminderHMS";
    public static final String EXTRA_REMINDER = "com.example.android.reminderhms.REMINDER";
    public static final String EXTRA_ID = "com.example.android.reminderhms.ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
                .setContentTitle(intent.getStringExtra(ReminderBroadcast.EXTRA_REMINDER))//should take from Intent's extra
                .setContentText("Test")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(intent.getIntExtra(ReminderBroadcast.EXTRA_ID, -1), builder.build());

        Thread thread = new Thread() {
            public void run(){
                ReminderRoomDB.getDatabase(context).reminderDao().deleteById(intent.getIntExtra(ReminderBroadcast.EXTRA_ID, -1));
            }
        };

        thread.start();

    }
}
