package com.example.novatrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Alarm {

    public static void setAlarm(Context context, long triggerTime,
                                String title, String message, int requestCode) {

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_text", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );
    }
}