package org.exthmui.share.shared;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;

import java.util.UUID;

public class SenderUtils {
    private static final String SEND_CHANNEL_ID = "org.exthmui.share.notification.channel.SEND";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_receive_name);
            String description = context.getString(R.string.notification_channel_receive_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(SEND_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification buildSendingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesSent, String fileName, String targetName, boolean indeterminate) {
        createNotificationChannel(context);//TODO: make use of statuscode

        String title = String.format(context.getString(R.string.notification_title_sending), fileName, targetName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        return new NotificationCompat.Builder(context, SEND_CHANNEL_ID)
                .setContentTitle(title)
                .setProgress((int)totalBytesToSend, (int)bytesSent, indeterminate)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_notification_send)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent)
                .build();
    }
}
