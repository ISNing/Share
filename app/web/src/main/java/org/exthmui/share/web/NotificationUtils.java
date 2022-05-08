package org.exthmui.share.web;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.misc.Constants;

public abstract class NotificationUtils {
    private static final String WEB_SERVICE_CHANNEL_ID = "org.exthmui.share.notification.channel.WEB_SERVICE";

    public static void createServiceNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            org.exthmui.share.shared.misc.NotificationUtils.createProgressNotificationChannelGroup(context);
            CharSequence name = context.getString(R.string.notification_channel_web_service_name);
            String description = context.getString(R.string.notification_channel_web_service_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(WEB_SERVICE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_SERVICE_CHANNEL_GROUP_ID);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    public static Notification buildServiceNotification(@NonNull Context context) {
        createServiceNotificationChannel(context);

        return new NotificationCompat.Builder(context, WEB_SERVICE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title_send_service))
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send)
                .build();
    }
}
