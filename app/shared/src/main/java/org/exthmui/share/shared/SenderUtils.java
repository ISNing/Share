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
import androidx.work.Data;
import androidx.work.WorkManager;

import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.misc.IConnectionType;

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

    public static Notification buildSendingNotification(Context context, IConnectionType connectionType, int statusCode, UUID workerId, long totalBytesToSend, long bytesSent, @Nullable String fileName, @Nullable String targetName, boolean indeterminate) {
        return buildSendingNotification(context, connectionType, statusCode, workerId, totalBytesToSend, bytesSent, new String[]{fileName,}, targetName, indeterminate);
    }

    public static Notification buildSendingNotification(Context context, IConnectionType connectionType, int statusCode, UUID workerId, long totalBytesToSend, long bytesSent, @NonNull String[] fileNames, @Nullable String targetName, boolean indeterminate) {
        createProgressNotificationChannel(context);

        if (fileNames[0] == null)
            fileNames[0] = context.getString(R.string.notification_placeholder_unknown);
        StringBuilder fileNameStr = new StringBuilder(fileNames[0]);
        for (String s : fileNames) {
            if (s == null) s = context.getString(R.string.notification_placeholder_unknown);
            fileNameStr.append("\n").append(s);
        }

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending, fileNames.length, targetName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending, fileNames.length, targetName, fileNameStr.toString());
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setProgress((int) totalBytesToSend, (int) bytesSent, indeterminate)
                .setSmallIcon(R.drawable.ic_notification_send)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent)
                .build();
    }

    public static Notification buildSendingFailedNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        int statusCode = output.getInt(BaseWorker.STATUS_CODE, Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal());
        String message = output.getString(BaseWorker.F_MESSAGE);
        String localizedMessage = output.getString(BaseWorker.F_LOCALIZED_MESSAGE);

        String targetName = output.getString(Sender.TARGET_PEER_NAME);
        String fileName = output.getString(Entity.FILE_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);

        StringBuilder fileNameStr;
        if (fileName != null) fileNameStr = new StringBuilder(fileName);
        else if (fileNames != null) {
            fileNameStr = new StringBuilder(fileNames[0]);
            for (String s : fileNames) {
                if (s == null) s = context.getString(R.string.notification_placeholder_unknown);
                fileNameStr.append("\n").append(s);
            }
        } else
            fileNameStr = new StringBuilder(context.getString(R.string.notification_placeholder_unknown));

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_failed, fileNames == null ? 1 : fileNames.length, targetName, fileNameStr.toString(), localizedMessage);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_failed, fileNames == null ? 1 : fileNames.length, targetName, fileNameStr.toString(), localizedMessage);

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send)
                .build();
    }

    public static Notification buildSendingSucceededNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        String targetName = output.getString(Sender.TARGET_PEER_NAME);
        String fileName = output.getString(Entity.FILE_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);

        StringBuilder fileNameStr;
        if (fileName != null) fileNameStr = new StringBuilder(fileName);
        else if (fileNames != null) {
            fileNameStr = new StringBuilder(fileNames[0]);
            for (String s : fileNames) fileNameStr.append("\n").append(s);
        } else
            fileNameStr = new StringBuilder(context.getString(R.string.notification_placeholder_unknown));

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_succeeded, fileNames == null ? 1 : fileNames.length, targetName, fileNameStr.toString());
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_succeeded, fileNames == null ? 1 : fileNames.length, targetName, fileNameStr.toString());

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send)
                .build();
    }
}
