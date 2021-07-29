package org.exthmui.share.shared;

import android.app.Notification;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    public static void createProgressNotificationChannelGroup(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_group_progress_name);
            String description = context.getString(R.string.notification_channel_group_progress_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }

    public static void createServiceNotificationChannelGroup(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_group_service_name);
            String description = context.getString(R.string.notification_channel_group_service_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_SERVICE_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }

    public static void createRequestNotificationChannelGroup(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notification_channel_group_request_name);
            String description = context.getString(R.string.notification_channel_group_request_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }
}
