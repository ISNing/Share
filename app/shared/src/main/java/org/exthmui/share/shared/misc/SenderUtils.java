package org.exthmui.share.shared.misc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.WorkManager;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.send.ReceiverInfo;
import org.exthmui.share.shared.base.send.Sender;

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

    public static Notification buildSendingNotification(Context context,
                                                        IConnectionType connectionType,
                                                        int statusCode, UUID workerId,
                                                        long totalBytesToSend, long bytesSent,
                                                        @NonNull FileInfo[] fileInfos,
                                                        @Nullable ReceiverInfo receiverInfo,
                                                        boolean indeterminate) {
        createProgressNotificationChannel(context);

        String fileNameStr = Utils.genFileInfosStr(context, fileInfos);

        String receiverName = receiverInfo == null ? null : receiverInfo.getDisplayName();
        if (receiverName == null)
            receiverName = context.getString(R.string.notification_placeholder_unknown);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending, fileInfos.length, receiverName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending, fileInfos.length, Formatter.formatFileSize(context, bytesSent), Formatter.formatFileSize(context, totalBytesToSend));
        String subText = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_sending, fileInfos.length, receiverName, fileNameStr);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        return new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSubText(subText)
                .setStyle(new NotificationCompat.BigTextStyle().setSummaryText(text).bigText(bigText))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setProgress((int) totalBytesToSend, (int) bytesSent, indeterminate)
                .setSmallIcon(R.drawable.ic_notification_send)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent)
                .build();
    }

    public static Notification buildSendingSucceededNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        String receiverName = output.getString(Sender.TARGET_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = Utils.genFileInfosStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_succeeded, fileNames == null ? 1 : fileNames.length, receiverName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_succeeded, fileNames == null ? 1 : fileNames.length, fileNameStr, fileNames == null ? 1 : fileNames.length);
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_sending_succeeded, fileNames == null ? 1 : fileNames.length, fileNameStr);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_success);

        if (bigText != null)
            builder.setStyle(new NotificationCompat.BigTextStyle().setSummaryText(text).bigText(bigText));
        return builder.build();
    }

    public static Notification buildSendingFailedNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        int statusCode = output.getInt(BaseWorker.STATUS_CODE, Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal());
        String message = output.getString(BaseWorker.F_MESSAGE);
        String localizedMessage = output.getString(BaseWorker.F_LOCALIZED_MESSAGE);

        String receiverName = output.getString(Sender.TARGET_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = Utils.genFileInfosStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_failed, fileNames == null ? 1 : fileNames.length, receiverName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_failed, fileNames == null ? 1 : fileNames.length, localizedMessage);
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_sending_failed, fileNames == null ? 1 : fileNames.length, localizedMessage, fileNameStr);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SEND_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_failed);

        if (bigText != null)
            builder.setStyle(new NotificationCompat.BigTextStyle().setSummaryText(text).bigText(bigText));
        return builder.build();
    }
}
