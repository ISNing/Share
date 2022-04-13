package org.exthmui.share.shared.misc;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_ACCEPT;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_ACCEPTATION_DIALOG;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_REJECT;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_FILE_NAME;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_FILE_SIZE;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_PEER_INFO_TRANSFER;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_PLUGIN_CODE;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_REQUEST_ID;
import static org.exthmui.share.shared.IShareBroadcastReceiver.ACTION_STOP_RECEIVER;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.WorkManager;

import org.exthmui.share.shared.IShareBroadcastReceiver;
import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.ui.AcceptationRequestActivity;

import java.util.UUID;

public abstract class ReceiverUtils {
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

    public static PendingIntent buildStopReceiverPendingIntent(Context context, String pluginCode) {
        Intent dialogIntent = new Intent()
                .setAction(ACTION_STOP_RECEIVER)
                .setPackage(context.getApplicationContext().getPackageName())
                .putExtra(IShareBroadcastReceiver.EXTRA_PLUGIN_CODE, pluginCode);
        return PendingIntent.getBroadcast(context, 0, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildOpenFilePendingIntent(Context context, Uri uri) {
        Intent dialogIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(uri);
        return PendingIntent.getBroadcast(context, 0, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildDialogPendingIntent(Context context, String pluginCode, String requestId, SenderInfo senderInfo, String[] fileNames, long[] fileSizes, int notificationId) {
        Intent dialogIntent = new Intent()
                .setAction(ACTION_ACCEPTATION_DIALOG)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PEER_INFO_TRANSFER, senderInfo)
                .putExtra(EXTRA_FILE_NAME, fileNames)
                .putExtra(EXTRA_FILE_SIZE, fileSizes)
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

    public static void requestAcceptation(Context context, String pluginCode, String requestId, SenderInfo senderInfo, String[] fileNames, long[] fileSizes, int notificationId) {
        createRequestNotificationChannel(context);

        String fileNameStr = genFileNameAndSizeStr(context, fileNames, fileSizes);

        @SuppressLint("LaunchActivityFromNotification") NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REQUEST_CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSmallIcon(R.drawable.ic_notification_request)
                .setContentTitle(context.getString(R.string.notification_title_receive_request))
                .setContentText(context.getResources().getQuantityString(R.plurals.notification_text_receive_request, fileNames.length, senderInfo.getDisplayName(), fileNameStr))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(buildDialogPendingIntent(context, pluginCode, requestId, senderInfo, fileNames, fileSizes, notificationId))
                .addAction(R.drawable.ic_action_accept, context.getString(R.string.notification_action_accept),
                        buildAcceptPendingIntent(context, pluginCode, requestId, notificationId))
                .addAction(R.drawable.ic_action_reject, context.getString(R.string.notification_action_reject),
                        buildRejectPendingIntent(context, pluginCode, requestId, notificationId));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }

    public static void startRequestActivity(Context context, String pluginCode, String requestId, SenderInfo senderInfo, String fileName, long fileSize, int notificationId) {
        Intent intent = new Intent(context, AcceptationRequestActivity.class)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PEER_INFO_TRANSFER, senderInfo)
                .putExtra(EXTRA_FILE_NAME, fileName)
                .putExtra(EXTRA_FILE_SIZE, fileSize)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
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
    public static Notification buildReceivingNotification(Context context, IConnectionType connectionType, int statusCode, UUID workerId, long totalBytesToSend, long bytesReceived, @NonNull String[] fileNames, @Nullable SenderInfo senderInfo, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title;
        String text;
        String senderName = senderInfo == null ? null : senderInfo.getDisplayName();
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);

        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        if (statusCode == Constants.TransmissionStatus.INITIALIZING.getNumVal()) {
            title = context.getString(R.string.notification_title_initializing);
            text = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
            cancelPendingIntent = buildStopReceiverPendingIntent(context, connectionType.getCode());
        } else if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) {
            title = context.getString(R.string.notification_title_receive_waiting);
            text = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
            cancelPendingIntent = buildStopReceiverPendingIntent(context, connectionType.getCode());
        } else {
            String fileNameStr = genFileNameAndSizeStr(context, fileNames, null);

            title = context.getResources().getQuantityString(R.plurals.notification_title_receiving, fileNames.length, senderName);
            text = context.getResources().getQuantityString(R.plurals.notification_text_receiving, fileNames.length, senderName, fileNameStr);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID).setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (statusCode != Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal())
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }

    public static Notification buildReceivingSucceededNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        String senderName = output.getString(Receiver.FROM_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        String[] fileUris = output.getStringArray(Entity.FILE_URIS);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = genFileNameAndSizeStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_succeeded, fileNames == null ? 1 : fileNames.length, senderName, fileNameStr);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_succeeded, fileNames == null ? 1 : fileNames.length, senderName, fileNameStr);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send);
        if (fileUris != null && fileUris.length == 1) {
            String openFile = context.getString(R.string.notification_action_open_file);
            PendingIntent openFilePendingIntent = buildOpenFilePendingIntent(context, Uri.parse(fileUris[0]));
            builder.addAction(R.drawable.ic_action_cancel, openFile, openFilePendingIntent);
            builder.setContentIntent(openFilePendingIntent);
        }
        return builder.build();
    }

    public static Notification buildReceivingFailedNotification(Context context, Data output) {
        createProgressNotificationChannel(context);

        int statusCode = output.getInt(BaseWorker.STATUS_CODE, Constants.TransmissionStatus.UNKNOWN_ERROR.getNumVal());
        String message = output.getString(BaseWorker.F_MESSAGE);
        String localizedMessage = output.getString(BaseWorker.F_LOCALIZED_MESSAGE);

        String senderName = output.getString(Receiver.FROM_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = genFileNameAndSizeStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_sending_failed, fileNames == null ? 1 : fileNames.length, senderName, fileNameStr, localizedMessage);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_sending_failed, fileNames == null ? 1 : fileNames.length, senderName, fileNameStr, localizedMessage);

        return new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_send)
                .build();
    }

    private static String genFileNameAndSizeStr(Context context, @Nullable String[] fileNames, @Nullable long[] fileSizes) {
        StringBuilder fileNameStr;
        if (fileNames != null) {
            fileNameStr = new StringBuilder(fileNames[0]);
            for (int i=0; i < fileNames.length; i++) {
                String fileSizeStr;
                if (fileSizes != null)
                    fileSizeStr = Formatter.formatFileSize(context, fileSizes[i]);
                else fileSizeStr = context.getString(R.string.notification_placeholder_unknown);
                if (fileNames[i] == null) fileNames[i] = context.getString(R.string.notification_placeholder_unknown);
                fileNameStr.append("\n").append(String.format("%s(%s)", fileNames[i], fileSizeStr));
            }
        } else
            fileNameStr = new StringBuilder(context.getString(R.string.notification_placeholder_unknown));
        return fileNameStr.toString();
    }
}
