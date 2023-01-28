package org.exthmui.share.shared.misc;

import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

public abstract class NotificationUtils {
    public static void createProgressNotificationChannelGroup(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_progress_name);
            String description = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_progress_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }

    public static void createServiceNotificationChannelGroup(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_service_name);
            String description = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_service_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_SERVICE_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }

    public static void createRequestNotificationChannelGroup(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_request_name);
            String description = context.getString(org.exthmui.share.shared.R.string.notification_channel_group_request_description);
            NotificationChannelGroup channel = new NotificationChannelGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID, name);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setDescription(description);
            }
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannelGroup(channel);
        }
    }
}
