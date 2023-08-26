package org.exthmui.share.shared.misc;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_ACCEPT;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_ACCEPTATION_DIALOG;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.ACTION_REJECT;
import static org.exthmui.share.shared.AcceptationBroadcastReceiver.EXTRA_FILE_INFOS;
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
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkManager;

import org.exthmui.share.shared.IShareBroadcastReceiver;
import org.exthmui.share.shared.R;
import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.FileInfo;
import org.exthmui.share.shared.base.IConnectionType;
import org.exthmui.share.shared.base.receive.Receiver;
import org.exthmui.share.shared.base.receive.SenderInfo;
import org.exthmui.share.shared.ui.AcceptationRequestActivity;
import org.exthmui.utils.StackTraceUtils;
import org.exthmui.utils.UriPathUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

public abstract class ReceiverUtils {
    public static final String TAG = "ReceiverUtils";

    private static final String REQUEST_CHANNEL_ID = "org.exthmui.share.notification.channel.REQUEST";
    private static final String RECEIVE_PROGRESS_CHANNEL_ID = "org.exthmui.share.notification.channel.RECEIVE";
    private static final String RECEIVE_SERVICE_CHANNEL_ID = "org.exthmui.share.notification.channel.RECEIVE_SERVICE";

    public static void createProgressNotificationChannel(@NonNull Context context) {
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

    public static void createServiceNotificationChannel(@NonNull Context context) {
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

    public static void createRequestNotificationChannel(@NonNull Context context) {
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

    public static PendingIntent buildStopReceiverPendingIntent(@NonNull Context context, String pluginCode) {
        Intent stopReceiverIntent = new Intent()
                .setAction(ACTION_STOP_RECEIVER)
                .setPackage(context.getApplicationContext().getPackageName())
                .putExtra(IShareBroadcastReceiver.EXTRA_PLUGIN_CODE, pluginCode);
        return PendingIntent.getBroadcast(context, 0, stopReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildOpenFilePendingIntent(Context context, Uri uri) {
        Intent openFileIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(uri);
        return PendingIntent.getBroadcast(context, 0, openFileIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent buildDialogPendingIntent(Context context, String pluginCode, String requestId, SenderInfo senderInfo, FileInfo[] fileInfos, int notificationId) {
        Intent dialogIntent = buildRequestActivityIntent(context, pluginCode, requestId, senderInfo, fileInfos, notificationId);
        dialogIntent.setAction(ACTION_ACCEPTATION_DIALOG);
        return PendingIntent.getActivity(context, 0, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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

    public static void requestAcceptation(@NonNull Context context, String pluginCode, String requestId, @NonNull SenderInfo senderInfo, @NonNull FileInfo[] fileInfos, int notificationId) {
        createRequestNotificationChannel(context);

        String fileNameStr = Utils.genFileInfosStr(context, fileInfos);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_receive_request, fileInfos.length, senderInfo.getDisplayName(), fileInfos.length);
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_receive_request, fileInfos.length, senderInfo.getDisplayName(), fileInfos.length);

        @SuppressLint("LaunchActivityFromNotification") NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REQUEST_CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSmallIcon(R.drawable.ic_notification_request)
                .setContentTitle(context.getString(R.string.notification_title_receive_request))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(buildDialogPendingIntent(context, pluginCode, requestId, senderInfo, fileInfos, notificationId))
                .addAction(R.drawable.ic_action_accept, context.getString(R.string.notification_action_accept),
                        buildAcceptPendingIntent(context, pluginCode, requestId, notificationId))
                .addAction(R.drawable.ic_action_reject, context.getString(R.string.notification_action_reject),
                        buildRejectPendingIntent(context, pluginCode, requestId, notificationId));

        NotificationUtils.postNotification(context, notificationId, builder.build());
    }

    public static Intent buildRequestActivityIntent(@NonNull Context context, String pluginCode, String requestId, SenderInfo senderInfo, FileInfo[] fileInfos, int notificationId) {
        Intent intent = new Intent(context, AcceptationRequestActivity.class)
                .putExtra(EXTRA_PLUGIN_CODE, pluginCode)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_PEER_INFO_TRANSFER, senderInfo)
                .putExtra(EXTRA_FILE_INFOS, fileInfos)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static void startRequestActivity(@NonNull Context context, String pluginCode, String requestId, SenderInfo senderInfo, FileInfo[] fileInfos, int notificationId) {
        context.startActivity(buildRequestActivityIntent(context, pluginCode, requestId, senderInfo, fileInfos, notificationId));
    }

    @NonNull
    public static Notification buildServiceNotification(@NonNull Context context) {
        createServiceNotificationChannel(context);

        return new NotificationCompat.Builder(context, RECEIVE_SERVICE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title_receive_service))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .build();
    }

    @NonNull
    public static Notification buildReceivingNotification(@NonNull Context context, @NonNull IConnectionType connectionType, int statusCode, @NonNull UUID workerId, long totalBytesToSend, long bytesReceived, @NonNull FileInfo[] fileInfos, @Nullable SenderInfo senderInfo, boolean indeterminate) {
        createProgressNotificationChannel(context);

        String title;
        String text;
        String subText = null;
        String bigText = null;
        String senderName = senderInfo == null ? null : senderInfo.getDisplayName();
        if (senderName == null)
            senderName = context.getString(R.string.notification_placeholder_unknown);

        boolean setProgress = false;

        String cancel = context.getString(R.string.notification_action_cancel);
        PendingIntent cancelPendingIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId);

        if (statusCode == Constants.TransmissionStatus.INITIALIZING.getNumVal()) {
            title = context.getString(R.string.notification_title_receive_initializing);
            text = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
            cancelPendingIntent = buildStopReceiverPendingIntent(context, connectionType.getCode());
        } else if (statusCode == Constants.TransmissionStatus.WAITING_FOR_REQUEST.getNumVal()) {
            title = context.getString(R.string.notification_title_receive_waiting);
            text = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
            cancelPendingIntent = buildStopReceiverPendingIntent(context, connectionType.getCode());
        } else {
            setProgress = true;

            title = context.getResources().getQuantityString(R.plurals.notification_title_receiving, fileInfos.length, senderName);
            text = context.getResources().getQuantityString(R.plurals.notification_text_receiving, fileInfos.length, Formatter.formatFileSize(context, bytesReceived), Formatter.formatFileSize(context, totalBytesToSend));
            subText = context.getString(Constants.TransmissionStatus.parse(statusCode).getStrRes());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID).setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_receive)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel, cancel, cancelPendingIntent);
        if (subText != null)
            builder.setSubText(subText);
        if (setProgress)
            builder.setProgress((int) totalBytesToSend, (int) bytesReceived, indeterminate);
        return builder.build();
    }

    @NonNull
    public static Notification buildReceivingSucceededNotification(@NonNull Context context, @NonNull Bundle output) {
        createProgressNotificationChannel(context);

        String senderName = output.getString(Receiver.FROM_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = Utils.genFileInfosStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_receiving_succeeded, fileNames == null ? 1 : fileNames.length, senderName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_receiving_succeeded, fileNames == null ? 1 : fileNames.length, fileNameStr, fileNames == null ? 1 : fileNames.length);
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_receiving_succeeded, fileNames == null ? 1 : fileNames.length, fileNameStr);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_success);

        if (bigText != null)
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return builder.build();
    }

    @NonNull
    public static Notification buildReceivingFailedNotification(@NonNull Context context, @NonNull Bundle output) {
        createProgressNotificationChannel(context);

        int statusCode = output.getInt(BaseWorker.STATUS_CODE, Constants.TransmissionStatus.ERROR.getNumVal());
        String message = output.getString(BaseWorker.F_MESSAGE);
        String localizedMessage = output.getString(BaseWorker.F_LOCALIZED_MESSAGE);

        String senderName = output.getString(Receiver.FROM_PEER_NAME);
        String[] fileNames = output.getStringArray(Entity.FILE_NAMES);
        long[] fileSizes = output.getLongArray(Entity.FILE_SIZES);

        String fileNameStr = Utils.genFileInfosStr(context, fileNames, fileSizes);

        String title = context.getResources().getQuantityString(R.plurals.notification_title_receiving_failed, fileNames == null ? 1 : fileNames.length, senderName);
        String text = context.getResources().getQuantityString(R.plurals.notification_text_receiving_failed, fileNames == null ? 1 : fileNames.length, localizedMessage);
        String bigText = context.getResources().getQuantityString(R.plurals.notification_text_expanded_receiving_failed, fileNames == null ? 1 : fileNames.length, localizedMessage, fileNameStr);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, RECEIVE_PROGRESS_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notification_failed);

        if (bigText != null)
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return builder.build();
    }

    public static Pair<Uri, BufferedOutputStream> openFileOutputStream(@NonNull Context context, @Nullable String fileName) {
        DocumentFile destinationDirectory = Utils.getDestinationDirectory(context);
        if (!destinationDirectory.canWrite()) {
            Log.e(TAG, "Error occurred while opening FileOutputStream: No permission writing into the destination folder");
            return null;
        }
        if (fileName == null)
            fileName = Utils.getDefaultFileName(context);
        if (Utils.useSAF(context)) {
            DocumentFile file = destinationDirectory.createFile("", fileName);
            if (file == null) return null;
            OutputStream os;
            if (!file.canWrite()) {
                Log.e(TAG, "Error occurred while opening FileOutputStream: No permission writing into the file");
                return null;
            }
            try {
                os = context.getContentResolver().openOutputStream(file.getUri());
            } catch (FileNotFoundException e) {
                Log.e(TAG, String.format("Error occurred while opening FileOutputStream: %s(message: %s)", e, e.getMessage())
                        + "\n" + StackTraceUtils.getStackTraceString(e.getStackTrace()));
                return null;
            }
            if (os == null) return null;
            return new Pair<>(file.getUri(), new BufferedOutputStream(os));
        } else {
            String destinationDirectoryPath = new UriPathUtils(context).getPath(destinationDirectory.getUri());
            if (destinationDirectoryPath == null) return null;
            File file = new File(destinationDirectoryPath + "/" + fileName);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.e(TAG, String.format("Error occurred while FileOutputStream: %s(message: %s)", e, e.getMessage())
                        + "\n" + StackTraceUtils.getStackTraceString(e.getStackTrace()));
                return null;
            }
            return new Pair<>(Uri.fromFile(file), new BufferedOutputStream(fos));
        }
    }
}
