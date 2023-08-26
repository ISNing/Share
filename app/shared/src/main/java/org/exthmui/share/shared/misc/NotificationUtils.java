package org.exthmui.share.shared.misc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import java.util.LinkedList;
import java.util.List;

public abstract class NotificationUtils {
    final static private List<Pair<Integer, Notification>> sNotificationList = new LinkedList<>();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static boolean isPermissionGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public static void postNotificationDirect(@NonNull Context context, int id, @NonNull Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(id, notification);
    }

    public static void postNotification(@NonNull Context context, int id, @NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isPermissionGranted(context)) {
                sNotificationList.add(new Pair<>(id, notification));
                return;
            }
        }
        postNotificationDirect(context, id, notification);
    }

    private static void postAllNotificationsBlocked(@NonNull Context context) {
        for (Pair<Integer, Notification> p : sNotificationList) {
            postNotificationDirect(context, p.first, p.second);
        }
    }

    public static void notifyPermissionGranted(@NonNull Context context) {
        postAllNotificationsBlocked(context);
    }

    public static void cancelNotification(@NonNull Context context, int id) {
        sNotificationList.removeIf(p -> p.first == id);
        cancelNotificationDirect(context, id);
    }

    public static void cancelNotificationDirect(@NonNull Context context, int id) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(id);
    }

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
