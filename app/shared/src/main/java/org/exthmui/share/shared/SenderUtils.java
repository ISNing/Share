package org.exthmui.share.shared;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;

import java.util.UUID;

public abstract class SenderUtils {
    private static final String SEND_PROGRESS_CHANNEL_ID = "org.exthmui.share.notification.channel.SEND";
    private static final String SEND_SERVICE_CHANNEL_ID = "org.exthmui.share.notification.channel.SEND_SERVICE";

    public static void createProgressNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createProgressNotificationChannelGroup(context);
            CharSequence name = context.getString(R.string.notification_channel_send_name);
            String description = context.getString(R.string.notification_channel_send_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(SEND_PROGRESS_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void createServiceNotificationChannel(Context context) {
        NotificationUtils.createServiceNotificationChannelGroup(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_send_service_name);
            String description = context.getString(R.string.notification_channel_send_service_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(SEND_SERVICE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_SERVICE_CHANNEL_GROUP_ID);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification buildServiceNotification(Context context) {
        createServiceNotificationChannel(context);

        return new NotificationCompat.Builder(context, SEND_SERVICE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title_send_service))
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send)
                .build();
    }

    public static Notification buildSendingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesSent, @Nullable String fileName, @Nullable String targetName, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title = context.getString(R.string.notification_title_sending, fileName, targetName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setProgress((int) totalBytesToSend, (int) bytesSent, indeterminate)
                .setSmallIcon(R.drawable.ic_notification_send)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent)
                .build();
    }

    public static Notification buildSendingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable String targetName, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title;
        if (fileNames.length == 2)
            title = context.getString(R.string.notification_title_sending_two, fileNames[0], fileNames[1], targetName);
        else
            title = context.getString(R.string.notification_title_sending_multi, fileNames[0], fileNames[1], fileNames.length - 2, targetName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setProgress((int) totalBytesToSend, (int) bytesSent, indeterminate)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_notification_send)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent)
                .build();
    }
}
