package org.exthmui.share.shared;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_ACCEPT;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_ACCEPTATION_DIALOG;
import static org.exthmui.share.shared.ShareBroadcastReceiver.ACTION_REJECT;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_FILE_SIZE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PEER_NAME;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_PLUGIN_CODE;
import static org.exthmui.share.shared.ShareBroadcastReceiver.EXTRA_REQUEST_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkManager;

import org.exthmui.share.shared.ui.AcceptationRequestActivity;

import java.util.UUID;

public class ReceiverUtils {
    private static final String REQUEST_CHANNEL_ID = "org.exthmui.share.notification.channel.REQUEST";
    private static final String RECEIVE_PROGRESS_CHANNEL_ID = "org.exthmui.share.notification.channel.RECEIVE";
    private static final String RECEIVE_SERVICE_CHANNEL_ID = "org.exthmui.share.notification.channel.RECEIVE_SERVICE";

    public static void createProgressNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createProgressNotificationChannelGroup(context);
            String name = context.getString(R.string.notification_channel_receive_name);
            String description = context.getString(R.string.notification_channel_receive_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(RECEIVE_PROGRESS_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_PROGRESS_CHANNEL_GROUP_ID);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void createServiceNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createServiceNotificationChannelGroup(context);
            String name = context.getString(R.string.notification_channel_receive_service_name);
            String description = context.getString(R.string.notification_channel_receive_service_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(RECEIVE_SERVICE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_SERVICE_CHANNEL_GROUP_ID);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void createRequestNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createRequestNotificationChannelGroup(context);
            String name = context.getString(R.string.notification_channel_request_name);
            String description = context.getString(R.string.notification_channel_request_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(REQUEST_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setGroup(Constants.NOTIFICATION_REQUEST_CHANNEL_GROUP_ID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setAllowBubbles(true);
            }
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification buildServiceNotification(Context context) {
        createServiceNotificationChannel(context);

        return new NotificationCompat.Builder(context, RECEIVE_SERVICE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title_receive_service))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .build();
    }

    public static PendingIntent buildDialogPendingIntent(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSize, int notificationId) {
        Intent dialogIntent = new Intent()
                .setAction(ACTION_ACCEPTATION_DIALOG)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PEER_NAME, peerName)
                .putExtra(EXTRA_FILE_NAME, fileName)
                .putExtra(EXTRA_FILE_SIZE, fileSize)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, 0, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildAcceptPendingIntent(Context context, String pluginCode, String requestId, int notificationId) {
        Intent acceptIntent = new Intent()
                .setAction(ACTION_ACCEPT)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildRejectPendingIntent(Context context, String pluginCode, String requestId, int notificationId) {
        Intent rejectIntent = new Intent()
                .setAction(ACTION_REJECT)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, 0, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void requestAcceptation(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSizeBytes, int notificationId) {
        createRequestNotificationChannel(context);

        @SuppressLint("LaunchActivityFromNotification") NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REQUEST_CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSmallIcon(R.drawable.ic_notification_request)
                .setContentTitle(context.getString(R.string.notification_title_receive_request))
                .setContentText(context.getString(R.string.notification_content_receive_request, peerName, fileName, Formatter.formatFileSize(context, fileSizeBytes)))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(buildDialogPendingIntent(context, pluginCode, requestId, peerName, fileName, fileSizeBytes, notificationId))
                .addAction(R.drawable.ic_action_accept, context.getString(R.string.notification_action_accept),
                        buildAcceptPendingIntent(context, pluginCode, requestId, notificationId))
                .addAction(R.drawable.ic_action_reject, context.getString(R.string.notification_action_reject),
                        buildRejectPendingIntent(context, pluginCode, requestId, notificationId));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }

    public static void startRequestActivity(Context context, String pluginCode, String requestId, String peerName, String fileName, long fileSize, int notificationId) {
        Intent intent = new Intent(context, AcceptationRequestActivity.class)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PEER_NAME, peerName)
                .putExtra(EXTRA_FILE_NAME, fileName)
                .putExtra(EXTRA_FILE_SIZE, fileSize)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static Notification buildReceivingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesReceived, @Nullable String fileName, @Nullable String senderName, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title;
        if (fileName == null)
            fileName = context.getString(R.string.notification_placeholder_unknown);
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            title = context.getString(R.string.notification_title_receive_waiting);
        else
            title = context.getString(R.string.notification_title_receive_receiving, fileName, senderName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID).setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (statusCode != Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }

    public static Notification buildReceivingNotification(Context context, int statusCode, UUID workerId, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable String senderName, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title;
        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i] == null)
                fileNames[i] = context.getString(R.string.notification_placeholder_unknown);
        }
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            title = context.getString(R.string.notification_title_receive_waiting);
        else if (fileNames.length == 1)
            title = context.getString(R.string.notification_title_receive_receiving, fileNames[0], senderName);
        else if (fileNames.length == 2)
            title = context.getString(R.string.notification_title_receive_receiving_two, fileNames[0], fileNames[1], senderName);
        else
            title = context.getString(R.string.notification_title_receive_receiving_multi, fileNames[0], fileNames[1], fileNames.length - 2, senderName);
        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID).setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(context.getString(Constants.TransmissionStatus.parse(statusCode).getFriendlyStringRes()))
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true);
        if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (statusCode != Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }
}
